#include <ssd_env.h>
#include <ssd_virtmem.h>
#include <ssd_superblock.h>
#include <sys/mman.h>
#include <stdio.h>

int
ssd_virtmem_init(ssd_virtmem_pool_t *pool, uint64_t capacity)
{
	uint64_t offset = SSD_SUPERBLOCK_CAPACITY + sizeof(ssd_superblock_header_t);
	
	pool->capacity = capacity / offset;

	if (pool->capacity < SSD_MAX_NUM_THREADS) {
		fprintf(stderr, "requested vmem space is too small, superblock starvation is possible\n");
		return -1;
	}

	if (pool->capacity >= SSD_MAX_NUM_THREADS + (1 << 16)) {
		fprintf(stderr, "current stack implementation is inappropriate for the vmem of huge capacity\n");
		return -1;
	}

	capacity = pool->capacity * offset;

	int flags = MAP_PRIVATE|MAP_ANONYMOUS;

	pool->mapping = mmap(0, capacity, PROT_READ|PROT_WRITE, flags, -1, 0);
	
	int rc = mlock(pool->mapping, capacity);
	if (rc < 0) {
		fprintf(stderr, "Not enough space in RAM for the cache of the size %lu\n", capacity);
		munmap(pool->mapping, capacity);
		return -1;
	}

	uint64_t val;

	val  = (uint64_t)((uint8_t*)pool->mapping + offset * SSD_MAX_NUM_THREADS);
	val |= (uint64_t)((uint16_t)(pool->capacity - SSD_MAX_NUM_THREADS) << ADDRESS_SPACE_ORDER);
	pool->stack.head = (void*)val;

	for (uint32_t i = 0; i < pool->capacity; i++) {
	
		ssd_superblock_header_ptr superblock;
		superblock = (ssd_superblock_header_ptr)((uint8_t*)pool->mapping + i * offset);
		
		if (i < SSD_MAX_NUM_THREADS || i + 1 == pool->capacity) {
			
			superblock->next = 0;

		} else {

			val =  (uint64_t)((uint8_t*)superblock + offset);
			val |= (uint64_t)((uint16_t)(pool->capacity - i - 1) << ADDRESS_SPACE_ORDER);
			superblock->next = (ssd_superblock_header_ptr)val; 

		}

		superblock->status = SSD_SUPERBLOCK_STATUS_EMPTY;
		superblock->usage = 0.f;
		ssd_rwlock_init(&superblock->rwlock);
	}

	return 0;
}

void
ssd_virtmem_free(ssd_virtmem_pool_t *pool)
{
	munlock(pool->mapping, pool->capacity);
	munmap(pool->mapping, pool->capacity);
}
