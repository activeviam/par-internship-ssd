#include "tests.h"
#include <unistd.h>
#include <stdio.h>
#include <ssd_chunk.h>

ssd_heap_pool_t heaps;

#define NUM_CHUNKS_PER_THREAD 10

static void*
test_arbitration_worker()
{
	ssd_chunk_t chunks[NUM_CHUNKS_PER_THREAD];
	
	uint8_t trid = ssd_thread_get_trid();

	for (uint8_t i = 0; i < NUM_CHUNKS_PER_THREAD; i++) {
		ssd_chunk_t *chunk = &chunks[i];
		uint32_t size = 64 << i;
		ssd_chunk_init(&heaps, chunk, size);
		printf("thread %u: chunk %u: superblock = %p, block = %lu\n", trid, i, chunk->superblock, (uint64_t)chunk->block);
	}
	
	for (uint8_t i = 0; i < NUM_CHUNKS_PER_THREAD; i++) {
		ssd_chunk_t *chunk = &chunks[i];
		ssd_chunk_free(chunk);
	}

	return 0;
}

void
test_arbitration()
{
	ssd_thread_add_trid();

	ssd_virtmem_pool_t virtmem_pool;
	ssd_virtmem_init(&virtmem_pool, 24 * (sizeof(ssd_superblock_header_t) + SSD_SUPERBLOCK_CAPACITY));

	ssd_heap_init(&heaps, &virtmem_pool);

	printf("mempool = %p\n", virtmem_pool.mapping);
	
	for (uint8_t trid = 1; trid < SSD_MAX_NUM_THREADS; trid++) {
		pthread_t thread;
		pthread_create(&thread, 0, ssd_thread_register_workload, (void*)test_arbitration_worker);
	}

	test_arbitration_worker();	
	
	void* res;
	for (uint8_t trid = 1; trid < SSD_MAX_NUM_THREADS; trid++) {
		pthread_join(ssd_thread_pthread_wrapper(trid), &res);
	}

	ssd_heap_free(&heaps);
	ssd_virtmem_free(&virtmem_pool);
}
