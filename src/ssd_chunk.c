#include <ssd_chunk.h>

#define INT_ORDER		2
#define DOUBLE_ORDER	3

#define ASYNC	0
#define SYNC	1

#define READY			0
#define	PENDING_STORE	1
#define PENDING_LOAD	2

static inline int min(int a, int b) { return a < b ? a : b ; }
static inline int max(int a, int b) { return a > b ? a : b ; }

static void
update_prediction_rate(struct ssd_chunk_cache *cache, const uint32_t old_id, const uint32_t new_id)
{
	int hit_prediction_rate = (int)cache->hit_prediction_rate;

	if (old_id + 1 == new_id) {
		hit_prediction_rate = min(255, hit_prediction_rate + 8);
	} else {
		hit_prediction_rate = max(0, hit_prediction_rate - 8);
	}

	cache->hit_prediction_rate = (uint8_t)hit_prediction_rate;
}

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
    printf("chunk capacity: %lu bytes\n", chunk->capacity << chunk->ns->lb_order);
    printf("logical block size: %u bytes\n", 1 << chunk->ns->lb_order);
    printf("offset in namespace: %u blocks\n", chunk->lb_offset);
    printf("cache:\n");
	printf("\t hit prediction rate: %.2f\n", chunk->local_cache.hit_prediction_rate / 2.55);
    printf("\t actual cache size: %u lines\n", chunk->local_cache.actual_size);
    printf("\t blocks in cache:\n");
    for (uint8_t i = 0; i < chunk->local_cache.actual_size; i++) {
        void* buf = ssd_cache_get_page(chunk->local_cache.lbs[i]);
        printf("\t\t line %u block %u address %p ", i, chunk->local_cache.ids[i], buf);
		uint8_t stat = chunk->local_cache.pending[i];
        printf("pending ? %c\n", stat == READY ? 'n' : stat == PENDING_STORE ? 'w' : 'r');
    }
}

void
ssd_chunk_sync(struct ssd_chunk *chunk)
{
	bool cache_pending;

    struct ssd_chunk_cache *cache = &chunk->local_cache;

    do {
        
		spdk_nvme_qpair_process_completions(chunk->qpair, 0);

		cache_pending = false;

        for (uint8_t k = 0; k < cache->actual_size; k++) {
            if (cache->pending[k]) {
                cache_pending = true;
            }
        }

    } while (cache_pending);

}

void
ssd_chunk_free(struct ssd_chunk *chunk)
{
    struct ssd_chunk_cache *cache = &chunk->local_cache;

	ssd_chunk_sync(chunk);

   	for (uint8_t k = 0; k < cache->actual_size; k++) {

        ssd_cache_handle_t handle = cache->lbs[k];

        if (ssd_cache_valid_handle(handle)) {
            ssd_cache_push(chunk->global_cache, handle);
        }
    }
    
    free(chunk);
}

struct ssd_chunk*
ssd_chunk_init(struct ctrlr_entry       *ctrlr,
               struct spdk_nvme_qpair   *qpair,
               struct ssd_cache         *cache,
               uint64_t                 capacity,
			   uint8_t					initial_hpr)
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
            chunk->capacity = lbs_needed;
            chunk->ns = entry;
            chunk->qpair = qpair; 
            chunk->global_cache = cache;
            chunk->lb_offset = entry->lbs_occupied;

            chunk->local_cache.curr_cacheline = 0;
			chunk->local_cache.hit_prediction_rate = initial_hpr;

            uint8_t size = 0;

            for (uint8_t k = 0; k < MAX_CHUNK_CACHESIZE; k++) {

				if (size >= lbs_needed)
					break;

                ssd_cache_handle_t handle = ssd_cache_pop(chunk->global_cache);
                
                if (ssd_cache_valid_handle(handle)) {
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
        exit(EXIT_FAILURE);
    }
}

static void
complete(void *arg, const struct spdk_nvme_cpl *completion)
{
    check_io_error(completion);

    uint64_t upd = (uint64_t)arg;
    uint64_t cacheline = upd >> 48; 
    struct ssd_chunk_cache *cache = (struct ssd_chunk_cache*)(upd ^ (cacheline << 48));
    
    cache->pending[(uint8_t)cacheline] = READY;
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
load_lb(struct ssd_chunk *chunk, const uint32_t value, const uint8_t cacheline, const uint8_t sync_flag)
{
    struct ssd_chunk_cache *cache = &chunk->local_cache;

    uint64_t upd = cacheline;
    upd = upd << 48;
    upd |= (uint64_t)cache;

    ssd_cache_handle_t handle = cache->lbs[cacheline];

    int rc = spdk_nvme_ns_cmd_read(chunk->ns->ns,
                                chunk->qpair,
                                ssd_cache_get_page(handle),
                                chunk->lb_offset + value * chunk->ns->lb_sectors,
                                chunk->ns->lb_sectors,
                                complete,
                                (void*)upd,
                                0);
    if (rc != 0) {
        fprintf(stderr, "async ssd read operation failed with block # %u\n", value);
    } else {
        while ((sync_flag != ASYNC) && (cache->pending[cacheline])) {
            spdk_nvme_qpair_process_completions(chunk->qpair, 0);
        }
    }
    
    return rc;
}

static int
store_lb(struct ssd_chunk *chunk, const uint8_t cacheline, const uint8_t sync_flag)
{
    struct ssd_chunk_cache *cache = &chunk->local_cache;

	uint32_t value = cache->ids[cacheline];

    uint64_t upd = cacheline;
    upd = upd << 48;
    upd |= (uint64_t)cache;

    ssd_cache_handle_t handle = cache->lbs[cacheline]; 
	
    int rc = spdk_nvme_ns_cmd_write(chunk->ns->ns,
                                chunk->qpair,
                                ssd_cache_get_page(handle),
                                chunk->lb_offset + value * chunk->ns->lb_sectors,
                                chunk->ns->lb_sectors,
                                complete,
                                (void*)upd,
                                0);

	if (rc != 0) {
        fprintf(stderr, "async ssd write operation failed with block # %u\n", value);
    } else {
        while ((sync_flag != ASYNC) && (cache->pending[cacheline])) {
            spdk_nvme_qpair_process_completions(chunk->qpair, 0);
        }
    }
    
    return rc;
}

static int
cache_update(struct ssd_chunk *chunk, const uint32_t id, uint8_t *cacheline, const uint8_t sync_flag)
{
    struct ssd_chunk_cache *cache = &chunk->local_cache;

	// Nice moment to try augmenting the cache size
    if (cache->actual_size < MAX_CHUNK_CACHESIZE) {

        ssd_cache_handle_t handle = ssd_cache_pop(chunk->global_cache);

        if (ssd_cache_valid_handle(handle)) {

            cache->lbs[cache->actual_size] = handle;
            cache->ids[cache->actual_size] = id;
			cache->pending[cache->actual_size] = PENDING_LOAD;

			*cacheline = cache->actual_size++;
			return load_lb(chunk, id, *cacheline, sync_flag);
        }
    }

	// TODO: add mechanism for pushing back the unused cachelines
	
    do {

		spdk_nvme_qpair_process_completions(chunk->qpair, 0);

        for (uint8_t k = 0; k < cache->actual_size; k++) {

            if (cache->pending[k] == READY) {

                cache->ids[k] = id;
				cache->pending[k] = PENDING_LOAD;

				*cacheline = k;
				return load_lb(chunk, id, *cacheline, sync_flag);
            }
       	}

		// Only do that once for asynchronous task
    } while (sync_flag);

	return 0;
}

static void*
fetch_lb(struct ssd_chunk *chunk, uint32_t new_id)
{
    struct ssd_chunk_cache *cache = &chunk->local_cache;

    uint8_t  old_cacheline = cache->curr_cacheline;
    uint32_t old_id = cache->ids[old_cacheline];
    void*    old_lb = ssd_cache_get_page(cache->lbs[old_cacheline]);
    
    if (old_id == new_id) {
        return old_lb;
    }

	update_prediction_rate(cache, old_id, new_id);
	
    cache->pending[old_cacheline] = PENDING_STORE;

	// This case is special, because a singleline cache is obliged to
	// complete the store operation before the next load. In the case
	// of multiline cache it could be more convenient to try load first
	// and flush the old data next. It is done to prevent overflowing the
	// cache qpair by irrelevant completions
    if (cache->actual_size == 1) {

        if (store_lb(chunk, old_cacheline, ASYNC)) {
            fprintf(stderr, "failed to update cache: store error\n");
            return NULL;
        }

        if (cache_update(chunk, new_id, &old_cacheline, SYNC)) {
            fprintf(stderr, "failed to update cache: load error\n");
            return NULL;
        }
        
        return ssd_cache_get_page(cache->lbs[0]);
    }

    uint8_t new_cacheline = search_cacheline(cache, new_id);

    if (new_cacheline == cache->actual_size) { // Cache miss

		// Performing a forced cache load on ready cacheline
        if (cache_update(chunk, new_id, &new_cacheline, SYNC)) {
            fprintf(stderr, "failed to update cache: load error\n");
            return NULL;
        }

    } else { // Cache hit

		while (cache->pending[new_cacheline] != READY) {
			spdk_nvme_qpair_process_completions(chunk->qpair, 0);
		}

		// Cache waits for loads, but simply ignores pending writes
		// in case of cache hit. It should be safe to use the data
		// after erasing the pending flag, because all the modifications
		// of this data will be reflushed in the future anyway
        // cache->pending[new_cacheline] = READY;
    }

    if (store_lb(chunk, old_cacheline, ASYNC)) {
        fprintf(stderr, "failed to update cache: store error\n");
        return NULL;
    }

	cache->curr_cacheline = new_cacheline;

	// Prediction part
	new_id++;
	if 	(spdk_likely(new_id < chunk->capacity) &&
		(cache->hit_prediction_rate >= 200) &&
		(search_cacheline(cache, new_id) == cache->actual_size))
	{
		// Cache never blocks on prediction steps (ASYNC flag is used)
		cache->pending[cache->curr_cacheline] = PENDING_STORE;
		cache_update(chunk, new_id, &new_cacheline, ASYNC);
		cache->pending[cache->curr_cacheline] = READY;
	}

    return ssd_cache_get_page(cache->lbs[cache->curr_cacheline]);
}

double
ssd_chunk_read_double(struct ssd_chunk* chunk, uint64_t pos)
{   
    struct ssd_lbid id = ssd_get_lbid(pos, chunk->ns->lb_order - DOUBLE_ORDER);
    double *lb = (double*)fetch_lb(chunk, id.index);
    if (lb) {
        return lb[id.offset];
    } else {
        fprintf(stderr, "cannot fetch cache block for read_double() at pos %lu\n", pos);
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
        fprintf(stderr, "cannot fetch cache block for write_double() at pos %lu\n", pos);
    }
}
