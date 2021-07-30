#include <ssd_chunk.h>

#define INT_ORDER 2
#define DOUBLE_ORDER 3

struct localbuf_updater {
    struct chunk    *chunk;
    uint32_t        pos;
};

static struct ssd_lbid
ssd_get_lbid(uint64_t pos, uint32_t order)
{
    struct ssd_lbid res;
    res.index = (pos >> order);
    res.offset = pos ^ (res.index << order);
    return res;
}

void
ssd_chunk_print(const struct ssd_chunk *chunk)
{
    printf("==========\n");
    printf("chunk namespace: %d\n", spdk_nvme_ns_get_id(chunk->ns->ns));
    printf("chunk capacity: %lu bytes\n", chunk->capacity);
    printf("logical block size: %u bytes\n", 1 << chunk->ns->lb_order);
    printf("offset in namespace: %u blocks\n", chunk->lb_offset);
    printf("cache:\n");
    printf("\t actual cache size: %u lines\n", chunk->local_cache.actual_size);
    printf("\t blocks in cache:\n");
    for (uint8_t i = 0; i < chunk->local_cache.actual_size; i++) {
        void* buf = ssd_cache_get_page(chunk->global_cache, chunk->local_cache.lbs[i]);
        printf("\t\t line %u block %u address %p ", i, chunk->local_cache.ids[i], buf);
        printf("pending ? %c\n", chunk->local_cache.pending[i] == 1 ? 'y' : 'n');
    }
}

void
ssd_chunk_free(struct ssd_chunk *chunk)
{
    int is_cache_pending;

    struct ssd_chunk_cache *cache = &chunk->local_cache;

    do {
        is_cache_pending = 0; 
        for (uint8_t k = 0; k < cache->actual_size; k++) {
            if (cache->pending[k]) {
                is_cache_pending = 1;
                spdk_nvme_qpair_process_completions(chunk->qpair, 0);
            }
        }
    } while (is_cache_pending);

    for (uint8_t k = 0; k < cache->actual_size; k++) {

        ssd_cache_handle_t handle = cache->lbs[k];

        if (ssd_cache_valid_handle(chunk->global_cache, handle)) {
            ssd_cache_push(chunk->global_cache, handle);
        }
    }
    
    free(chunk);
}

struct ssd_chunk*
ssd_chunk_init(struct ctrlr_entry       *ctrlr,
               struct spdk_nvme_qpair   *qpair,
               struct ssd_cache         *cache,
               uint64_t                 capacity)
{
    struct ssd_chunk    *chunk = NULL;
    struct ns_entry     *entry;
    uint32_t            lbs_needed;

    TAILQ_FOREACH(entry, &ctrlr->ns, link) {

        lbs_needed = (capacity >> entry->lb_order);
        if ((capacity ^ (lbs_needed << entry->lb_order)) != 0) {
            lbs_needed++;
        }

        printf("lbs needed: %u\n", lbs_needed);

        if (lbs_needed <= entry->lb_capacity - entry->lbs_occupied) {

            chunk = (struct ssd_chunk*)malloc(sizeof(struct ssd_chunk));
            chunk->capacity = capacity;
            chunk->ns = entry;
            chunk->qpair = qpair; 
            chunk->global_cache = cache;
            chunk->lb_offset = entry->lbs_occupied;

            chunk->local_cache.curr_cacheline = 0;

            uint8_t size = 0;

            for (uint8_t k = 0; k < MAX_CHUNK_CACHESIZE; k++) {

                ssd_cache_handle_t handle = ssd_cache_pop(chunk->global_cache);
                
                if (ssd_cache_valid_handle(chunk->global_cache, handle)) {
                    chunk->local_cache.lbs[size] = handle;
                    chunk->local_cache.ids[size] = size;
                    chunk->local_cache.pending[k] = 0;
                    size++;
                }
            }

            chunk->local_cache.actual_size = size;

            entry->lbs_occupied += lbs_needed;
            
            return chunk;
        }

    }

    return chunk;
}

static void
check_io_error(const struct spdk_nvme_cpl *completion)
{
    if (spdk_nvme_cpl_is_error(completion)) {
        fprintf(stderr, "I/O error status: %s\n", spdk_nvme_cpl_get_status_string(&completion->status));
        fprintf(stderr, "I/O failed, aborting run\n");
        exit(1);
    }
}

static void
complete(void *arg, const struct spdk_nvme_cpl *completion)
{
    check_io_error(completion);

    uint64_t upd = (uint64_t)arg;
    uint64_t cacheline = upd >> 48; 
    struct ssd_chunk_cache *cache = (struct ssd_chunk_cache*)(upd ^ (cacheline << 48));
    
    cache->pending[(uint8_t)cacheline] = 0;
}

static uint8_t
search_cacheline(struct ssd_chunk_cache *cache, uint32_t id) {

    uint8_t k;

    for (k = 0; k < cache->actual_size; k++) {
        if (cache->ids[k] == id) {
            break;
        }
    }

    return k;
}

static int
sync_load_lb(struct ssd_chunk *chunk, uint32_t id)
{
    struct ssd_chunk_cache *cache = &chunk->local_cache;

    uint64_t upd = cache->curr_cacheline;
    upd << 48;
    upd |= (uint64_t)cache;

    ssd_cache_handle_t handle = cache->lbs[cache->curr_cacheline];

    cache->pending[cache->curr_cacheline] = 1;
    int rc = spdk_nvme_ns_cmd_read(chunk->ns->ns,
                                chunk->qpair,
                                ssd_cache_get_page(chunk->global_cache, handle),
                                chunk->lb_offset + id * chunk->ns->lb_sectors,
                                chunk->ns->lb_sectors,
                                complete,
                                (void*)upd,
                                0);
    if (rc != 0) {
        fprintf(stderr, "async ssd read operation failed\n");
    } else {
        while (cache->pending[cache->curr_cacheline]) {
            spdk_nvme_qpair_process_completions(chunk->qpair, 0);
        }
    }
    
    return rc;
}

static int
async_flush_lb(struct ssd_chunk *chunk, uint8_t cacheline)
{
    struct ssd_chunk_cache *cache = &chunk->local_cache;

    uint64_t upd = cacheline;
    upd << 48;
    upd |= (uint64_t)cache;

    ssd_cache_handle_t handle = cache->lbs[cacheline];
    uint32_t id = cache->ids[cacheline];

    return spdk_nvme_ns_cmd_write(chunk->ns->ns,
                                chunk->qpair,
                                ssd_cache_get_page(chunk->global_cache, handle),
                                chunk->lb_offset + id * chunk->ns->lb_sectors,
                                chunk->ns->lb_sectors,
                                complete,
                                (void*)upd,
                                0);
}

static int
force_cache_update(struct ssd_chunk *chunk, uint32_t id) {

    struct ssd_chunk_cache *cache = &chunk->local_cache;

    if (cache->actual_size < MAX_CHUNK_CACHESIZE) {

        ssd_cache_handle_t handle = ssd_cache_pop(chunk->global_cache);

        if (ssd_cache_valid_handle(chunk->global_cache, handle)) {

            cache->lbs[cache->actual_size] = handle;
            cache->ids[cache->actual_size] = id;
            cache->curr_cacheline = cache->actual_size++;

            return sync_load_lb(chunk, id);
        }
    }

    while (1) {
        for (uint8_t k = 0; k < cache->actual_size; k++) {
            if (!cache->pending[k]) {
                cache->ids[k] = id;
                cache->curr_cacheline = k;
                return sync_load_lb(chunk, id);
            }
            spdk_nvme_qpair_process_completions(chunk->qpair, 0);
        }
    }
}

static void*
fetch_lb(struct ssd_chunk *chunk, uint32_t new_id)
{
    struct ssd_chunk_cache *cache = &chunk->local_cache;

    uint8_t  old_cacheline = cache->curr_cacheline;
    uint32_t old_id = cache->ids[old_cacheline];
    void*    old_lb = ssd_cache_get_page(chunk->global_cache, cache->lbs[old_cacheline]);
    
    if (old_id == new_id) {
        return old_lb;
    }

    chunk->local_cache.pending[old_cacheline] = 1;

    if (cache->actual_size == 1) {

        if (async_flush_lb(chunk, old_cacheline) != 0) {
            fprintf(stderr, "failed to update cache\n");
            return NULL;
        }

        if (force_cache_update(chunk, new_id) != 0) {
            fprintf(stderr, "failed to update cache\n");
            return NULL;
        }
        
        return ssd_cache_get_page(chunk->global_cache, cache->lbs[0]);
    }

    uint8_t new_cacheline = search_cacheline(cache, new_id);

    if (new_cacheline == cache->actual_size) {

        if (force_cache_update(chunk, new_id) != 0) {
            fprintf(stderr, "failed to update cache\n");
            return NULL;
        }

    } else {

        cache->pending[new_cacheline] = 0;
        cache->curr_cacheline = new_cacheline;
    }

    if (async_flush_lb(chunk, old_cacheline) != 0) {
        fprintf(stderr, "failed to update cache\n");
        return NULL;
    }

    return ssd_cache_get_page(chunk->global_cache, cache->lbs[cache->curr_cacheline]);
}

double
ssd_chunk_read_double(struct ssd_chunk* chunk, uint64_t pos)
{   
    struct ssd_lbid id = ssd_get_lbid(pos, chunk->ns->lb_order - DOUBLE_ORDER);
    double *lb = (double*)fetch_lb(chunk, id.index);
    if (lb) {
        return lb[id.offset];
    } else {
        fprintf(stderr, "cannot fetch cache block for read double at pos %lu\n", pos);
        return 0;
    }
}

void
ssd_chunk_write_double(struct ssd_chunk* chunk, uint64_t pos, double value)
{
    struct ssd_lbid id = ssd_get_lbid(pos, chunk->ns->lb_order - DOUBLE_ORDER);
    double *lb = (double*)fetch_lb(chunk, id.index);
    if (lb) {
        lb[id.offset] = value;
    } else {
        fprintf(stderr, "cannot fetch cache block for read double at pos %lu\n", pos);
    }
}
