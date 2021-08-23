#include <ssd_chunk.h>
#include <ssd_allocator.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

#define INT_ORDER		2
#define DOUBLE_ORDER	3

#define ASYNC			0
#define SYNC			1

#define READY			0
#define AWAITING_STORE	1
#define	PENDING_STORE	2
#define PENDING_LOAD	3

#define CLEAN			0
#define DIRTY			1

static inline int64_t min(int64_t a, int64_t b) { return a < b ? a : b ; }
static inline int64_t max(int64_t a, int64_t b) { return a > b ? a : b ; }

static void
update_prediction_rate(struct ssd_chunk *chunk, int adjust)
{
	int64_t hit_prediction_rate = (int64_t)chunk->cache_prediction_rate;
	hit_prediction_rate = max(0, min(255, hit_prediction_rate + adjust));
	chunk->cache_prediction_rate = (uint8_t)hit_prediction_rate;
}

struct io_id {
	uint32_t index;
	uint32_t offset;
};

static struct io_id
get_io_id(uint64_t pos, uint32_t order)
{
    struct io_id res = {pos >> order, pos ^ (res.index << order)};
    return res;
}

void
ssd_chunk_print(const struct ssd_chunk *chunk)
{
    printf("==========\n");
    printf("chunk storage file: %d\n", chunk->fd);

	uint32_t block_order = ssd_cache_get_info(chunk->global_cache)->block_order;
    printf("chunk capacity: %lu bytes\n", chunk->capacity << block_order);
    printf("chunk block size: %u bytes\n", 1 << block_order);
    
	printf("chunk offset: %lu bytes\n", chunk->offset);
    printf("chunk local cache:\n");
	printf("\t hit prediction rate: %.2f\n", chunk->cache_prediction_rate / 2.55);
    printf("\t actual size: %u lines\n", chunk->cache_actual_size);
	printf("\t current cacheline: %u\n", chunk->cache_current_line);
    printf("\t blocks in cache:\n");
    for (uint8_t i = 0; i < chunk->cache_actual_size; i++) {
        void* buf = chunk->cache_iovecs[i].iov_base;
        printf("\t\t line %u block %u address %p ", i, chunk->cache_ids[i], buf);
		uint8_t stat = chunk->cache_pending[i];
        printf("pending ? %c\n", stat == READY ? 'n' : stat == PENDING_STORE ? 'w' : 'r');
    }
}

void
ssd_chunk_free(struct ssd_chunk *chunk)
{
    ssd_chunk_sync(chunk);

   	for (uint8_t k = 0; k < chunk->cache_actual_size; k++) {
        void *buf = chunk->cache_iovecs[k].iov_base;
        if (buf) {
            ssd_cache_push(chunk->global_cache, buf);
        }
    }
    
    free(chunk);
}

struct ssd_chunk*
ssd_chunk_init(struct io_uring 		*uring,
			   struct ssd_storage	*storage,
			   struct ssd_cache		*global_cache,
			   off_t 				capacity,
			   uint8_t				initial_hpr)
{
	if (!uring) {
		fprintf(stderr, "null uring in ssd_chunk\n");
		return NULL;
	}

	if (!global_cache) {
		fprintf(stderr, "null RAM global cache in ssd_chunk\n");
		return NULL;
	}

	struct ssd_chunk *chunk = (struct ssd_chunk*)malloc(sizeof(struct ssd_chunk));
	if (!chunk) {
		fprintf(stderr, "malloc error allocating heap structure ssd_chunk\n");
		return NULL;
	}

	uint32_t io_order = ssd_cache_get_info(global_cache)->block_order;
	uint32_t io_size = 1 << io_order;
	uint32_t ios_needed = capacity >> io_order;
	if (capacity ^ (ios_needed << io_order)) {
    	ios_needed++;
		capacity = ios_needed << io_order;
    }

	off_t offset = ssd_storage_allocate(storage, capacity);
	if (offset < 0) {
		fprintf(stderr, "memory error allocating %lu bytes in file %d\n", capacity, storage->fd);
		free(chunk);
		return NULL;
	}

	chunk->uring = uring;
	chunk->global_cache = global_cache;

	chunk->offset = offset;
	chunk->capacity = ios_needed;
	chunk->fd = storage->fd;

	chunk->cache_current_line = 0;
	chunk->cache_prediction_rate = initial_hpr;
	chunk->cache_usage = 0;

	uint8_t cachesize = 0; 
	uint8_t max_cachesize = min(ios_needed, CHUNK_CACHE_MAXSIZE);

	for (uint8_t k = 0; k < max_cachesize; k++) {

		void *buf = ssd_cache_pop(chunk->global_cache);

		if (buf) {
			chunk->cache_iovecs[cachesize].iov_base = buf;
			chunk->cache_iovecs[cachesize].iov_len = io_size;
			chunk->cache_ids[cachesize] = cachesize;
			chunk->cache_pending[cachesize] = READY;
			chunk->cache_dirty[cachesize] = CLEAN;
			cachesize++;
		}
	}

	if (cachesize == 0) {
		fprintf(stderr, "no cache pages available for chunk, retry later\n");
		free(chunk);
		return NULL;
	}
	chunk->cache_actual_size = cachesize;	

	int rc = io_uring_register_buffers(uring, chunk->cache_iovecs, cachesize);
	if (rc < 0) {
		fprintf(stderr, "failed to register fixed RAM buffers for chunk\n");
		free(chunk);
	}

	//TAILQ_INIT(&chunk->write_queue_entries);
	//chunk->write_queue_size = 0;

	return chunk;
}

static uint8_t
search_cacheline(struct ssd_chunk *chunk, uint32_t id)
{
    uint8_t k = 0;
    while (k < chunk->cache_actual_size) {
        if (chunk->cache_ids[k] == id) {
            break;
        }
		k++;
    }
    return k;
}

static void
process_data(ssd_chunk *chunk, uint64_t cache_data)
{
	uint8_t cacheline = (uint8_t)cache_data;
	chunk->cache_pending[cacheline] = READY;
	chunk->cache_dirty[cacheline] = CLEAN;
	chunk->cache_usage--;
}

static int
process_completions(struct ssd_chunk *chunk, uint8_t counter = 0)
{
	unsigned head;
	struct io_uring_cqe *cqe;

	uint8_t processed = 0;

	io_uring_for_each_cqe(chunk->uring, head, cqe) {
		
		if (cqe->res < 0) {
			fprintf(stderr, "error in completion %u: data = %lld, res = %d\n", head, cqe->user_data, cqe->res);
			return -1;
		}
		process_data(chunk, cqe->user_data);
		io_uring_cqe_seen(chunk->uring, cqe);

		if (++processed == counter) {
			break;
		}
	}
	
	return 0;
}

static int
_load_io_async(struct ssd_chunk *chunk, const uint32_t id, const uint32_t offset, const uint32_t size, const uint8_t cacheline)
{
	void *base = chunk->cache_iovecs[cacheline].iov_base;
	uint32_t block_size = chunk->cache_iovecs[cacheline].iov_len;

	struct io_uring_sqe *sqe = io_uring_get_sqe(chunk->uring);
	if (!sqe) {
		fprintf(stderr, "unable to get sqe instance\n");
		return -1;
	}

	io_uring_prep_read_fixed(sqe, chunk->fd, (uint8_t*)base + offset, size, chunk->offset + id * block_size + offset, cacheline);
	io_uring_sqe_set_data(sqe, (void*)(uint64_t)cacheline);
	
	int rc = io_uring_submit(chunk->uring);
	if (uring_unlikely(rc < 0)) {
		fprintf(stderr, "io_uring_submit fail: %s\n", strerror(-rc));
		return -1;
	}

	return 0;
}

static uint8_t
_mask_pos(struct ssd_chunk *chunk, const uint32_t offset)
{
	return (uint8_t)(16.f * offset / chunk->cache_iovecs[0].iov_len);
}

static int
load_io(struct ssd_chunk *chunk, const uint32_t id, uint32_t offset, const uint8_t cacheline, const uint8_t sync_flag)
{
	uint32_t block_size = chunk->cache_iovecs[cacheline].iov_len;

	if (sync_flag == ASYNC) {
		//chunk->cache_pending[cacheline] += 16;
		return _load_io_async(chunk, id, 0, block_size, cacheline);
	}

	void *base = chunk->cache_iovecs[cacheline].iov_base;

	//uint32_t part_size = block_size >> 4;
	//offset &= ~(part_size - 1);
	//uint8_t mask_pos = _mask_pos(chunk, offset);
	//chunk->cache_mask[cacheline] |= (1 << mask_pos);

	size_t fetched = pread(chunk->fd, (uint8_t*)base, block_size, chunk->offset + id * block_size);

	if (fetched != block_size) {
		fprintf(stderr, "unsuccessful sync read\n");
		return -1;
	}

	/*
	int rc = 0;
	if (mask_pos < 15) {
		chunk->cache_pending[cacheline] += 15 - mask_pos;
		rc |= _load_io_async(chunk, id, offset + part_size, block_size - offset - part_size, cacheline);
	}
	*/

	return 0;
}

static int
store_io(struct ssd_chunk *chunk, const uint8_t cacheline, const uint8_t sync_flag)
{	
	uint32_t id = chunk->cache_ids[cacheline];
	void *base = chunk->cache_iovecs[cacheline].iov_base;
	uint32_t size = chunk->cache_iovecs[cacheline].iov_len;

	struct io_uring_sqe *sqe = io_uring_get_sqe(chunk->uring);
	if (uring_unlikely(!sqe)) {
		fprintf(stderr, "unable to get sqe instance\n");
		return -1;
	}
	
	io_uring_prep_write_fixed(sqe, chunk->fd, base, size, chunk->offset + id * size, cacheline); 
	io_uring_sqe_set_data(sqe, (void*)(uint64_t)cacheline);
	
	int rc = io_uring_submit(chunk->uring);
	if (uring_unlikely(rc < 0)) {
		fprintf(stderr, "io_uring_submit fail: %s\n", strerror(-rc));
		return -1;
	} else {
		while (sync_flag && chunk->cache_pending[cacheline]) {
			process_completions(chunk);
		}
	}

    return 0;
}

static int
prepare_store_io(struct ssd_chunk *chunk, const uint8_t cacheline, const uint8_t sync_flag)
{
	if (sync_flag) {
		chunk->cache_pending[cacheline] = PENDING_STORE;
		return store_io(chunk, cacheline, sync_flag);
	}

	const float limit_usage_rate = 0.8f;

	if (1.f * chunk->cache_usage > limit_usage_rate * chunk->cache_actual_size) {
		
		for (uint8_t k = 0; k < chunk->cache_actual_size; k++) {
			if (chunk->cache_pending[k] == AWAITING_STORE) {
				chunk->cache_pending[k] = PENDING_STORE;
				store_io(chunk, k, ASYNC);
			}
		}

	}

	return 0;
}

void
ssd_chunk_sync(struct ssd_chunk *chunk)
{	
	uint8_t cache_pending;

    do {

		process_completions(chunk);

		cache_pending = 0;
        for (uint8_t k = 0; k < chunk->cache_actual_size; k++) {
            if (chunk->cache_pending[k] ) {
                cache_pending = 1;
				if (chunk->cache_pending[k] == AWAITING_STORE) {
					store_io(chunk, k, ASYNC);
				}
            }
        }

    } while (cache_pending);
}

static int
cache_update(struct ssd_chunk *chunk, const uint32_t id, const uint32_t offset, uint8_t *cacheline, const uint8_t sync_flag)
{
	// Nice moment to try augmenting the cache size
    if (chunk->cache_actual_size < min(CHUNK_CACHE_MAXSIZE, chunk->capacity)) {

        void *buf = ssd_cache_pop(chunk->global_cache);

        if (uring_unlikely(buf)) {

			uint8_t& size = chunk->cache_actual_size;
            
			chunk->cache_iovecs[size].iov_base = buf;
			chunk->cache_iovecs[size].iov_len = chunk->cache_iovecs[0].iov_len; 
            
			chunk->cache_ids[size] = id;
			chunk->cache_pending[size] = PENDING_LOAD;
			chunk->cache_usage++;

			*cacheline = size++;

			return load_io(chunk, id, offset, *cacheline, sync_flag);
        }
    }

	// TODO: add mechanism for pushing back the unused cachelines
	
    do {
		
		process_completions(chunk);
        
		for (uint8_t k = 0; k < chunk->cache_actual_size; k++) {

            if (chunk->cache_pending[k] == READY) {

                chunk->cache_ids[k] = id;
				chunk->cache_pending[k] = PENDING_LOAD;
				chunk->cache_usage++;

				*cacheline = k;
				return load_io(chunk, id, offset, *cacheline, sync_flag);
            }
       	}

		// Only do that once for asynchronous task
    } while (sync_flag != ASYNC);

	return 0;
}

static void*
fetch_lb(struct ssd_chunk *chunk, uint32_t new_id, uint32_t offset)
{
    uint8_t  old_cacheline = chunk->cache_current_line;
	uint8_t  new_cacheline;
    
	uint32_t old_id = chunk->cache_ids[old_cacheline];
    void*    old_lb = chunk->cache_iovecs[old_cacheline].iov_base;
    
    if (old_id == new_id) {
		update_prediction_rate(chunk, 1);	
		return old_lb;
    }

	// printf("old = %u, new = %u\n", old_id, new_id);

	
	if (chunk->cache_pending[old_cacheline] == READY) {
		chunk->cache_usage++;
	}

	if (chunk->cache_dirty[old_cacheline]) {
    	chunk->cache_pending[old_cacheline] = AWAITING_STORE;
	}

	// This case is special, because a singleline cache is obliged to
	// complete the store operation before the next load. In the case
	// of multiline cache it could be more convenient to try load first
	// and flush the old data next. It is done to prevent overflowing the
	// cache qpair by irrelevant completions
    if (chunk->cache_actual_size == 1) {
		
        if (uring_unlikely(prepare_store_io(chunk, old_cacheline, ASYNC))) {
            fprintf(stderr, "failed to update cache: store error\n");
            return NULL;
        }

        if (uring_unlikely(cache_update(chunk, new_id, offset, &old_cacheline, SYNC))) {
            fprintf(stderr, "failed to update cache: load error\n");
            return NULL;
        }
        
        return chunk->cache_iovecs[0].iov_base;
    }

	new_cacheline = search_cacheline(chunk, new_id);
	
    if (new_cacheline == chunk->cache_actual_size) { // Cache miss
		
		update_prediction_rate(chunk, -1);

		// Performing a forced cache load on ready cacheline
        if (uring_unlikely(cache_update(chunk, new_id, offset, &new_cacheline, SYNC))) {
            fprintf(stderr, "failed to update cache: load error\n");
            return NULL;
        }

    } else { // Cache hit

		update_prediction_rate(chunk, 1);

		do {
			process_completions(chunk);
		} while (chunk->cache_pending[new_cacheline] >= PENDING_LOAD);
	}

    if (uring_unlikely(prepare_store_io(chunk, old_cacheline, ASYNC))) {
        fprintf(stderr, "failed to update cache: store error\n");
        return NULL;
    }

	chunk->cache_current_line = new_cacheline;
	
	// Prediction part
	new_id++;
	if 	(uring_likely(new_id < chunk->capacity) &&
		(chunk->cache_prediction_rate >= 200) &&
		(search_cacheline(chunk, new_id) == chunk->cache_actual_size))
	{
		// Cache never blocks on prediction steps (ASYNC flag is used)
		uint8_t status_backup = chunk->cache_pending[chunk->cache_current_line]; 

		chunk->cache_pending[chunk->cache_current_line] = PENDING_STORE;
		cache_update(chunk, new_id, 0, &new_cacheline, ASYNC);
		chunk->cache_pending[chunk->cache_current_line] = status_backup;
	}

	return chunk->cache_iovecs[chunk->cache_current_line].iov_base;
}

double
ssd_chunk_read_double(struct ssd_chunk* chunk, uint64_t pos)
{   
    uint32_t block_order = ssd_cache_get_info(chunk->global_cache)->block_order; 
	struct io_id id = get_io_id(pos, block_order - DOUBLE_ORDER);
    double *lb = (double*)fetch_lb(chunk, id.index, id.offset << DOUBLE_ORDER);

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
	uint32_t block_order = ssd_cache_get_info(chunk->global_cache)->block_order; 
    struct io_id id = get_io_id(pos, block_order - DOUBLE_ORDER);
    double *lb = (double*)fetch_lb(chunk, id.index, id.offset);

    if (lb) {
        lb[id.offset] = value;
    } else {
        fprintf(stderr, "cannot fetch cache block for write_double() at pos %lu\n", pos);
    }
}
