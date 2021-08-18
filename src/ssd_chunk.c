#include <ssd_chunk.h>
#include <ssd_allocator.h>
#include <stdio.h>
#include <string.h>

#define INT_ORDER		2
#define DOUBLE_ORDER	3

#define ASYNC			0
#define SYNC			1

#define READY			0
#define	PENDING_STORE	1
#define PENDING_LOAD	2
#define BUSY			3

static inline int64_t min(int64_t a, int64_t b) { return a < b ? a : b ; }
static inline int64_t max(int64_t a, int64_t b) { return a > b ? a : b ; }

static void
update_prediction_rate(struct ssd_chunk *chunk, const uint32_t old_id, const uint32_t new_id)
{
	int64_t hit_prediction_rate = (int64_t)chunk->cache_prediction_rate;

	if (old_id + 1 == new_id) {
		hit_prediction_rate = min(255, hit_prediction_rate + 8);
	} else {
		hit_prediction_rate = max(0, hit_prediction_rate - 8);
	}

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

	TAILQ_INIT(&chunk->write_queue_entries);
	chunk->write_queue_size = 0;

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
	//if (chunk->cache_pending[(uint8_t)cacheline] == PENDING_STORE) {}

	if (cache_data < 255) {
		chunk->cache_pending[(uint8_t)cache_data] = READY;
		chunk->cache_usage--;
	} else {
		uint8_t *cachelines = (uint8_t*)cache_data;
		struct iovec* iovec = *(struct iovec**)(void*)(cachelines + 1);
		for (uint8_t k = 1 + sizeof(struct iovec*); k <= cachelines[0] + sizeof(struct iovec*); k++) {
			chunk->cache_pending[cachelines[k]] = READY;
		}
		chunk->cache_usage -= cachelines[0];
		free(cachelines);
		free(iovec);
	}
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
load_io(struct ssd_chunk *chunk, const uint32_t value, const uint8_t cacheline, const uint8_t sync_flag)
{
	struct io_uring_sqe *sqe = io_uring_get_sqe(chunk->uring);
	if (!sqe) {
		fprintf(stderr, "unable to get sqe instance\n");
		return -1;
	}

	void *base = chunk->cache_iovecs[cacheline].iov_base;
	uint32_t len = chunk->cache_iovecs[cacheline].iov_len;

	io_uring_prep_read_fixed(sqe, chunk->fd, base, len, value * len, cacheline);  
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

static struct io_block*
write_queue_push_block(struct ssd_chunk *chunk, const uint32_t id, const uint8_t cacheline)
{
	struct io_batch *batch, *batch_tmp;
	struct ssd_chunk::io_batch_head *head = &chunk->write_queue_entries;
    
	for (batch = TAILQ_FIRST(head); batch != NULL; batch = batch_tmp) {
    	
		batch_tmp = TAILQ_NEXT(batch, link);

		struct io_block *first = TAILQ_FIRST(&batch->blocks);
		struct io_block *first_tmp = NULL;
		if (batch_tmp) {
			first_tmp = TAILQ_FIRST(&batch_tmp->blocks);
		}

			
		// the block can be appended to the left of the current batch
		if (id == first->id - 1) {
			struct io_block *block = (struct io_block*)malloc(sizeof(struct io_block));
			block->id = id;
			block->cacheline = cacheline;
			TAILQ_INSERT_HEAD(&batch->blocks, block, link);
			batch->size++;
			return block;
		}

		// the block can be appended to the right of the current batch
		if (id == first->id + batch->size) {
			struct io_block *block = (struct io_block*)malloc(sizeof(struct io_block));
			block->id = id;
			block->cacheline = cacheline;
			TAILQ_INSERT_TAIL(&batch->blocks, block, link);
			batch->size++;

			// coalescing may appear if there was a 1-element space between 2 consecutive
			// batches before the last insertion
			if (first_tmp && id == first_tmp->id - 1) {
				TAILQ_CONCAT(&batch->blocks, &batch_tmp->blocks, link);
				batch->size += batch_tmp->size;
				batch = batch_tmp;
				batch_tmp = TAILQ_NEXT(batch_tmp, link);
				TAILQ_REMOVE(head, batch, link);
			}

			return block;
		}
		
		// there is no batch to append to, procedd to batch creation
		if (id > first->id + batch->size && (!first_tmp || id < first_tmp->id - 1)) {
			break;
		}

		// the block is already in the queue
		if (id >= first->id && id < first->id + batch->size) {
			uint8_t nb_steps = id - first->id;
			while (nb_steps--) {
				first = TAILQ_NEXT(first, link);
			}
			return first;
		}
    }	


	// at this point it is obvious that the new block cannot be appended to any existing
	// batch, and it should be inserted between the "batch" and the "batch_tmp"
	// create new batch
	struct io_batch *new_batch = (struct io_batch*)malloc(sizeof(struct io_batch));
	new_batch->size = 1;
	TAILQ_INIT(&new_batch->blocks);

	// at this point with a single block
	struct io_block *new_block = (struct io_block*)malloc(sizeof(struct io_block));
	new_block->id = id;
	new_block->cacheline = cacheline;

	TAILQ_INSERT_HEAD(&new_batch->blocks, new_block, link);
	// add the new batch at suitable position in queue with respect to the block numeration
	
	if (batch) {
		TAILQ_INSERT_AFTER(head, batch, new_batch, link);
	} else {
		TAILQ_INSERT_HEAD(head, new_batch, link);
	}

	chunk->write_queue_size++;
	return new_block;
}

static int
write_queue_pop_batch(struct ssd_chunk *chunk)
{
	uint32_t len = chunk->cache_iovecs[0].iov_len;

	struct io_batch *batch = TAILQ_FIRST(&chunk->write_queue_entries);
	uint8_t size = batch->size;

	struct io_batch::io_block_head *head = &batch->blocks;
	struct io_block *block, *block_tmp;

	struct iovec *iovecs = (struct iovec*)malloc(batch->size * sizeof(struct iovec));
	uint8_t *cachelines = (uint8_t*)malloc(batch->size + 1 + sizeof(struct iovec*));
	cachelines[0] = batch->size;
	struct iovec **ptr_to_vec = (struct iovec**)(void*)(cachelines + 1);
	*ptr_to_vec = iovecs;

	uint8_t k = 0;
	block = TAILQ_FIRST(head);

	uint64_t offset = (uint64_t)block->id * len;

	for (; block != NULL; block = block_tmp) {
		
		block_tmp = TAILQ_NEXT(block, link);
		
		iovecs[k].iov_base = chunk->cache_iovecs[block->cacheline].iov_base;
		iovecs[k].iov_len = len;

		cachelines[k + 1 + sizeof(struct iovec*)] = block->cacheline;

		k++;

		TAILQ_REMOVE(head, block, link);
		free(block);
	}

	TAILQ_REMOVE(&chunk->write_queue_entries, batch, link);
	free(batch);
	chunk->write_queue_size--;
	
	struct io_uring_sqe *sqe = io_uring_get_sqe(chunk->uring);
	if (uring_unlikely(!sqe)) {
		fprintf(stderr, "unable to get sqe instance\n");
		return -1;
	}

	io_uring_prep_writev(sqe, chunk->fd, iovecs, size, offset);
	io_uring_sqe_set_data(sqe, (void*)cachelines);
	
	int rc = io_uring_submit(chunk->uring);
	if (uring_unlikely(rc < 0)) {
		fprintf(stderr, "io_uring_submit fail: %s\n", strerror(-rc));
		return -1;
	}
}

void
ssd_chunk_sync(struct ssd_chunk *chunk)
{
	while (!TAILQ_EMPTY(&chunk->write_queue_entries)) {
		write_queue_pop_batch(chunk);
	}
		
	uint8_t cache_pending;

    do {

		process_completions(chunk);

		cache_pending = 0;
        for (uint8_t k = 0; k < chunk->cache_actual_size; k++) {
            if (chunk->cache_pending[k]) {
                cache_pending = 1;
            }
        }

    } while (cache_pending);
}

static int
store_io(struct ssd_chunk *chunk, const uint8_t cacheline, const uint8_t sync_flag)
{	
	uint32_t value = chunk->cache_ids[cacheline];

	if (sync_flag == ASYNC) {			
		write_queue_push_block(chunk, value, cacheline);
		return 0;
	}

	struct io_uring_sqe *sqe = io_uring_get_sqe(chunk->uring);
	if (uring_unlikely(!sqe)) {
		fprintf(stderr, "unable to get sqe instance\n");
		return -1;
	}

	void *base = chunk->cache_iovecs[cacheline].iov_base;
	uint32_t len = chunk->cache_iovecs[cacheline].iov_len;
	
	io_uring_prep_write_fixed(sqe, chunk->fd, base, len, value * len, cacheline); 
	io_uring_sqe_set_data(sqe, (void*)(uint64_t)cacheline);
	
	int rc = io_uring_submit(chunk->uring);
	if (uring_unlikely(rc < 0)) {
		fprintf(stderr, "io_uring_submit fail: %s\n", strerror(-rc));
		return -1;
	} else {
		while (chunk->cache_pending[cacheline]) {
			process_completions(chunk);
		}
	}

    return 0;
}

static int
cache_update(struct ssd_chunk *chunk, const uint32_t id, uint8_t *cacheline, const uint8_t sync_flag)
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

			return load_io(chunk, id, *cacheline, sync_flag);
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
				return load_io(chunk, id, *cacheline, sync_flag);
            }
       	}

		// Only do that once for asynchronous task
    } while (sync_flag != ASYNC);

	return 0;
}

static void*
fetch_lb(struct ssd_chunk *chunk, uint32_t new_id)
{
    uint8_t  old_cacheline = chunk->cache_current_line;
    uint32_t old_id = chunk->cache_ids[old_cacheline];
    void*    old_lb = chunk->cache_iovecs[old_cacheline].iov_base;
    
    if (old_id == new_id) {
        return old_lb;
    }
	
	if (chunk->cache_usage >= chunk->cache_actual_size - 2) {
		write_queue_pop_batch(chunk);
	}

	// printf("old = %u, new = %u\n", old_id, new_id);

	update_prediction_rate(chunk, old_id, new_id);
	
    chunk->cache_pending[old_cacheline] = PENDING_STORE;
	chunk->cache_usage++;
	
	// This case is special, because a singleline cache is obliged to
	// complete the store operation before the next load. In the case
	// of multiline cache it could be more convenient to try load first
	// and flush the old data next. It is done to prevent overflowing the
	// cache qpair by irrelevant completions
    if (chunk->cache_actual_size == 1) {
		
        if (uring_unlikely(store_io(chunk, old_cacheline, ASYNC))) {
            fprintf(stderr, "failed to update cache: store error\n");
            return NULL;
        }

        if (uring_unlikely(cache_update(chunk, new_id, &old_cacheline, SYNC))) {
            fprintf(stderr, "failed to update cache: load error\n");
            return NULL;
        }
        
        return chunk->cache_iovecs[0].iov_base;
    }

	uint8_t new_cacheline = search_cacheline(chunk, new_id);
	
    if (new_cacheline == chunk->cache_actual_size) { // Cache miss
		
		// Performing a forced cache load on ready cacheline
        if (uring_unlikely(cache_update(chunk, new_id, &new_cacheline, SYNC))) {
            fprintf(stderr, "failed to update cache: load error\n");
            return NULL;
        }

    } else { // Cache hit
		do {
			process_completions(chunk);
		} while (chunk->cache_pending[new_cacheline] == PENDING_LOAD); 
    }

    if (uring_unlikely(store_io(chunk, old_cacheline, ASYNC))) {
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
		cache_update(chunk, new_id, &new_cacheline, ASYNC);
		chunk->cache_pending[chunk->cache_current_line] = status_backup;
	}

	return chunk->cache_iovecs[chunk->cache_current_line].iov_base;
}

double
ssd_chunk_read_double(struct ssd_chunk* chunk, uint64_t pos)
{   
    uint32_t block_order = ssd_cache_get_info(chunk->global_cache)->block_order; 
	struct io_id id = get_io_id(pos, block_order - DOUBLE_ORDER);
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
	uint32_t block_order = ssd_cache_get_info(chunk->global_cache)->block_order; 
    struct io_id id = get_io_id(pos, block_order - DOUBLE_ORDER);
    double *lb = (double*)fetch_lb(chunk, id.index);
    if (lb) {
        lb[id.offset] = value;
    } else {
        fprintf(stderr, "cannot fetch cache block for write_double() at pos %lu\n", pos);
    }
}
