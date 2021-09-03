#include <ssd_chunk.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>

#define SSD_INT_ORDER 2
#define SSD_DOUBLE_ORDER 3

#define SSD_NOT_YET_REUSED 0
#define SSD_ALREADY_REUSED 1

static uint64_t
chunk_fetch_guid() {
	
	static volatile _Atomic uint64_t guid = 0;
		
	uint64_t old = guid;
	uint64_t new;

	do {
		new = old + 1;
	} while (!atomic_compare_exchange_weak_explicit(
		&guid,
		&old,
		new,
		memory_order_release,
		memory_order_relaxed
	));

	return guid;
}

int
ssd_chunk_init(ssd_heap_pool_t *pool, ssd_chunk_t *chunk, uint32_t size)
{
	uint64_t guid = chunk_fetch_guid();

	char filename[64];
	sprintf(filename, "ssd/chunk_%lu", guid);

	chunk->fd = open(filename, O_CREAT|O_TRUNC|O_RDWR, S_IRUSR|S_IWUSR|S_IRGRP|S_IWGRP);
	if (chunk->fd < 0) {
		fprintf(stderr, "cannot open file %s\n", filename);
		return -1;
	}

	chunk->superblock = 0;
	chunk->block = 0;
	int rc = ssd_heap_allocate(pool, &chunk->superblock, &chunk->block, size);
	if (rc < 0) {
		return rc;
	}

	chunk->pool = pool;
	chunk->block->owner = chunk;
	chunk->size = size;
	chunk->data = (void*)(chunk->block + 1);
	return 0;
}

double
ssd_chunk_read_double(ssd_chunk_t *chunk, uint32_t pos)
{
	if (pos >= (chunk->size >> SSD_DOUBLE_ORDER)) {
		//fprintf(stderr, "invalid arg: pos at read_double()\n");
		return 0;
	}

	while (1) {

		if (ssd_rwlock_tryrdlock(&chunk->superblock->rwlock)) {
	
			// Creator of the chunk doesn't perform GC on the current superblock at the moment
			if (chunk->data) {

				// It didn't perform GC on this block before neither
				double *doubles = (double*)SSD_PTR_MASK(chunk->data);
				double value = doubles[pos];
				ssd_rwlock_rdunlock(&chunk->superblock->rwlock);
				return value;	
			}
		
			// If GC has been aleady performed, there is a need in a new allocation
			ssd_rwlock_rdunlock(&chunk->superblock->rwlock);
		}

		
		// Only one thread should do the next chunk allocation

		if (pthread_mutex_trylock(&chunk->mutex) == 0) {

			ssd_superblock_header_ptr superblock;
			ssd_block_header_ptr block;

			// Proceed to new allocation
			int rc = ssd_heap_allocate(chunk->pool, &superblock, &block, chunk->size);
			if (rc < 0) {
				fprintf(stderr, "cannot allocate new chunk\n");
				return 0;
			}
			
			void *old_data = (void*)(chunk->block + 1);
			void *new_data = (void*)(block + 1);

			// While the thread execution allocation is in this point, the creator thread
			// may still flush the previous block of memory. Wait for the flush to complete
			ssd_rwlock_rdlock(&chunk->superblock->rwlock);
			if (!chunk->superblock->used) {
				memcpy(old_data, new_data, chunk->size);
				ssd_rwlock_rdunlock(&chunk->superblock->rwlock);
			} else {
				printf("WOW\n");
				ssd_rwlock_rdunlock(&chunk->superblock->rwlock);
				// At this point the flush is completed, executing thread may read the SSD disk 
				read(chunk->fd, new_data, chunk->size);
			}

			chunk->superblock = superblock;
			chunk->block = block;
			chunk->data = new_data;

			// Executor itself allocated the superblock, so no other thread could possibly flush it 
			// Executor becomes a new creator of the chunk's memory block
			double *doubles = (double*)chunk->data;
			double value = doubles[pos];
			pthread_mutex_unlock(&chunk->mutex);
			return value;
		
		} else {

			// Wait for the holder of the chunk's lock to become new creator
			pthread_mutex_lock(&chunk->mutex);
			pthread_mutex_unlock(&chunk->mutex);
		}
	}
}

void
ssd_chunk_write_double(ssd_chunk_t *chunk, uint32_t pos, double value)
{

	if (pos >= (chunk->size >> SSD_DOUBLE_ORDER)) {
		fprintf(stderr, "invalid arg: pos at read_double()\n");
		return;
	}

	while (1) {

		if (ssd_rwlock_tryrdlock(&chunk->superblock->rwlock)) {

			if (chunk->data) {

				double *doubles = (double*)SSD_PTR_MASK(chunk->data);
				doubles[pos] = value;

				// The block is marked as dirty. When GC is performing the flush,
				// it never flushes the clean pages
				chunk->data = SSD_CHUNK_DATA_DIRTY(chunk->data);
				ssd_rwlock_rdunlock(&chunk->superblock->rwlock);
				return;	
			}
		
			ssd_rwlock_rdunlock(&chunk->superblock->rwlock);
		}

		printf("wow\n");

		if (pthread_mutex_trylock(&chunk->mutex) == 0) {

			ssd_superblock_header_ptr superblock;
			ssd_block_header_ptr block;

			// Proceed to new allocation
			int rc = ssd_heap_allocate(chunk->pool, &superblock, &block, chunk->size);
			if (rc < 0) {
				fprintf(stderr, "cannot allocate newt chunk\n");
				return;
			}
			
			void *old_data = (void*)(chunk->block + 1);
			void *new_data = (void*)(block + 1);

			// While the thread execution allocation is in this point, the creator thread
			// may still flush the previous block of memory. Wait for the flush to complete
			ssd_rwlock_rdlock(&chunk->superblock->rwlock);
			if (!chunk->superblock->used) {
				memcpy(old_data, new_data, chunk->size);
				ssd_rwlock_rdunlock(&chunk->superblock->rwlock);
			} else {
				printf("WOW\n");
				ssd_rwlock_rdunlock(&chunk->superblock->rwlock);
				// At this point the flush is completed, executing thread may read the SSD disk 
				read(chunk->fd, new_data, chunk->size);
			}

			chunk->superblock = superblock;
			chunk->block = block;
			chunk->data = new_data;

			// Executor itself allocated the superblock, so no other thread could possibly flush it 
			// Executor becomes a new creator of the chunk's memory block
			double *doubles = (double*)chunk->data;
			doubles[pos] = value;
			pthread_mutex_unlock(&chunk->mutex);
			return;
		
		} else {
			pthread_mutex_lock(&chunk->mutex);
			pthread_mutex_unlock(&chunk->mutex);
		}
	}
}

void
ssd_chunk_free(ssd_chunk_t *chunk)
{
	ssd_rwlock_rdlock(&chunk->superblock->rwlock);
	if (chunk->data) {
		chunk->data = 0;
		ssd_heap_deallocate(chunk->superblock, chunk->block);
	}
	ssd_rwlock_rdunlock(&chunk->superblock->rwlock);

	close(chunk->fd);
}
