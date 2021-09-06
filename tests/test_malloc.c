#include "tests.h"
#include <unistd.h>
#include <stdio.h>
#include <ssd_chunk.h>

ssd_heap_pool_t heaps;

ssd_chunk_t chunk1, chunk2, chunk3, chunk4;

_Atomic int cond0 = 0;
_Atomic int cond1 = 0;

static void*
test_malloc_worker_0()
{
	ssd_chunk_init(&heaps, &chunk1, 4096);
	printf("chunk1: superblock = %p, block = %lu\n", chunk1.superblock, (uint64_t)chunk1.block);

	ssd_chunk_init(&heaps, &chunk2, 4096);
	printf("chunk2: superblock = %p, block = %lu\n", chunk2.superblock, (uint64_t)chunk2.block);
	
	atomic_store_explicit(&cond1, 1, memory_order_seq_cst);

	while (!cond0) { sleep(1); continue; }
	
	ssd_chunk_free(&chunk3);
	ssd_chunk_free(&chunk4);

	return 0;
}

static void*
test_malloc_worker_1()
{
	ssd_chunk_init(&heaps, &chunk3, 4096);
	printf("chunk3: superblock = %p, block = %lu\n", chunk3.superblock, (uint64_t)chunk3.block);

	ssd_chunk_init(&heaps, &chunk4, 4096);
	printf("chunk4: superblock = %p, block = %lu\n", chunk4.superblock, (uint64_t)chunk4.block);
	
	atomic_store_explicit(&cond0, 1, memory_order_seq_cst);

	while (!cond1) { sleep(1); continue; }
	
	ssd_chunk_free(&chunk1);
	ssd_chunk_free(&chunk2);

	return 0;
}

void
test_malloc()
{
	ssd_thread_add_trid();

	ssd_virtmem_pool_t virtmem_pool;
	ssd_virtmem_init(&virtmem_pool, 8 * (sizeof(ssd_superblock_header_t) + SSD_SUPERBLOCK_CAPACITY));

	ssd_heap_init(&heaps, &virtmem_pool);

	printf("mempool = %p\n", virtmem_pool.mapping);

	for (uint8_t trid = 1; trid < 2; trid++) {
		pthread_t thread;
		pthread_create(&thread, 0, ssd_thread_register_workload, (void*)test_malloc_worker_0);
	}

	test_malloc_worker_1();	
	
	void* res;
	for (uint8_t trid = 1; trid < 2; trid++) {
		pthread_join(ssd_thread_pthread_wrapper(trid), &res);
	}

	ssd_heap_free(&heaps);
	ssd_virtmem_free(&virtmem_pool);
}
