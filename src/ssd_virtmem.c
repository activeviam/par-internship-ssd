#include <ssd_env.h>
#include <ssd_virtmem.h>
#include <ssd_superblock.h>
#include <sys/mman.h>
#include <stdio.h>

int
ssd_virtmem_init(ssd_virtmem_pool_t *pool, uint64_t capacity)
{
	pthread_mutex_init(&pool->mutex, 0);
	ssd_superblock_hmap_init(&pool->hmap);

	uint64_t offset = SSD_SUPERBLOCK_CAPACITY + sizeof(ssd_superblock_header_t);
	
	pool->num_superblocks = capacity / offset;

	if (pool->num_superblocks < SSD_MAX_NUM_THREADS) {
		fprintf(stderr, "requested vmem space is too small, superblock starvation is possible\n");
		return -1;
	}

	if (pool->num_superblocks >= SSD_MAX_NUM_THREADS + (1 << 16)) {
		fprintf(stderr, "current stack implementation is inappropriate for the vmem of huge capacity\n");
		return -1;
	}

	capacity = pool->num_superblocks * offset;

	int flags = MAP_PRIVATE|MAP_ANONYMOUS;

	pool->mapping = mmap(0, capacity, PROT_READ|PROT_WRITE, flags, -1, 0);
	
	int rc = mlock(pool->mapping, capacity);
	if (rc < 0) {
		fprintf(stderr, "Not enough space in RAM for the cache of the size %lu\n", capacity);
		munmap(pool->mapping, capacity);
		return -1;
	}
	for (uint32_t i = 0; i < pool->num_superblocks; i++) {
	
		ssd_superblock_header_ptr superblock;
		superblock = (ssd_superblock_header_ptr)((uint8_t*)pool->mapping + i * offset);
		
		if (i < SSD_MAX_NUM_THREADS || i + 1 == pool->num_superblocks) {
			superblock->next = 0;
		} else {
			superblock->next = (ssd_superblock_header_ptr)((uint8_t*)superblock + offset);
		}
		
		superblock->bsize = 0;
		superblock->usage = 0.f;
		ssd_rwlock_init(&superblock->rwlock);
	}

	if (pool->num_superblocks == SSD_MAX_NUM_THREADS) {
		pool->unused = 0;
	} else {
		pool->unused = (ssd_superblock_header_ptr)((uint8_t*)pool->mapping + offset * SSD_MAX_NUM_THREADS);
	}

	return 0;
}

void
ssd_virtmem_acquire(ssd_virtmem_pool_t *pool,
				uint32_t bsize,
				ssd_superblock_header_ptr *superblock_ptr,
				ssd_block_header_ptr *block_ptr)
{
	*superblock_ptr = 0;
	*block_ptr = 0;

	pthread_mutex_lock(&pool->mutex);

	ssd_superblock_header_ptr *it;

	ssd_superblock_hmap_suggest(&pool->hmap, bsize, &it, block_ptr);

	if (it) {
		
		ssd_superblock_hmap_remove(&pool->hmap, it);
		*superblock_ptr = *it;

	} else if (pool->unused) {

		*superblock_ptr = pool->unused;	
		pool->unused = pool->unused->next;
		pthread_mutex_unlock(&pool->mutex);
		
		*block_ptr = ssd_concurrent_stack_pop(&((*superblock_ptr)->stack));

		return;

	} else if (pool->hmap.size > 0) {

		uint32_t capacity = ssd_superblock_hmap_capacity(&pool->hmap);
		uint32_t i = bsize % capacity;

		while (!(*superblock_ptr = pool->hmap.buckets[i])) {
			i = (i + 1) % capacity;
		}

		*superblock_ptr = ssd_superblock_hmap_remove(&pool->hmap, superblock_ptr);
	}

	pthread_mutex_unlock(&pool->mutex);
}

void
ssd_virtmem_release(ssd_virtmem_pool_t *pool, ssd_superblock_header_ptr superblock)
{
	pthread_mutex_lock(&pool->mutex);
	ssd_superblock_hmap_insert(&pool->hmap, superblock);
	pthread_mutex_unlock(&pool->mutex);
}

void
ssd_virtmem_free(ssd_virtmem_pool_t *pool)
{
	uint64_t offset = SSD_SUPERBLOCK_CAPACITY + sizeof(ssd_superblock_header_t);
	uint64_t capacity = pool->num_superblocks * offset;
	munlock(pool->mapping, capacity);
	munmap(pool->mapping, capacity);
}
