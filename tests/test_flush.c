#include "tests.h"
#include <stdio.h>
#include <ssd_chunk.h>

ssd_heap_pool_t heaps;

ssd_chunk_t chunk1;
_Atomic int cond = 0;

static void*
test_flush_worker_0()
{
	ssd_chunk_init(&heaps, &chunk1, 4096);
	printf("chunk1: superblock = %p, block = %p\n", chunk1.superblock, chunk1.block);

	for (uint32_t i = 0; i < 512; i++) {
		ssd_chunk_write_double(&chunk1, i, 0.1 * i);
	}

	ssd_chunk_t chunk2;
	ssd_chunk_init(&heaps, &chunk2, 2048);
	printf("chunk2: superblock = %p, block = %p\n", chunk2.superblock, chunk2.block);
	
	atomic_store_explicit(&cond, 1, memory_order_release);


	return 0;
}

static void*
test_flush_worker_1()
{
	while (!cond) { continue; }

	double val;

	for (uint32_t i = 0; i < 512; i++) {
		val = ssd_chunk_read_double(&chunk1, i);
		printf("data[%u] = %f\n", i, val);
	}

	ssd_chunk_free(&chunk1);

	return 0;
}

void
test_flush()
{
	ssd_thread_add_trid();

	ssd_virtmem_pool_t virtmem_pool;
	ssd_virtmem_init(&virtmem_pool, 2 * (sizeof(ssd_superblock_header_t) + SSD_SUPERBLOCK_CAPACITY));

	ssd_heap_init(&heaps, &virtmem_pool);

	printf("mempool = %p\n", virtmem_pool.mapping);

	for (uint8_t trid = 1; trid < 2; trid++) {
		pthread_t thread;
		pthread_create(&thread, 0, ssd_thread_register_workload, (void*)test_flush_worker_1);
	}

	test_flush_worker_0();	
	
	void* res;
	for (uint8_t trid = 1; trid < 2; trid++) {
		pthread_join(ssd_thread_pthread_wrapper(trid), &res);
	}

	ssd_heap_free(&heaps);
	ssd_virtmem_free(&virtmem_pool);
}
