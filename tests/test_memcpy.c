#include "tests.h"
#include <unistd.h>
#include <stdio.h>
#include <ssd_chunk.h>

ssd_heap_pool_t heaps;

#define NUM_CHUNKS_PER_THREAD 10

ssd_chunk_t chunk1, chunk2;

_Atomic int cond_memcpy1 = 0;
_Atomic int cond_memcpy2 = 0;

static void*
test_memcpy_worker_0()
{
	ssd_chunk_init(&heaps, &chunk1, 512);
	for (uint16_t i = 0; i < 64; i++) {
		ssd_chunk_write_double(&chunk1, i, 0.1 * i);
	}

	atomic_store_explicit(&cond_memcpy1, 1, memory_order_seq_cst);

	ssd_chunk_init(&heaps, &chunk2, 1024);
	for (uint16_t i = 0; i < 128; i++) {
		ssd_chunk_write_double(&chunk2, i, 0.1 * i);
	}

	ssd_chunk_free(&chunk2);

	while (!cond_memcpy2) { continue; }

	ssd_chunk_free(&chunk1);

	return 0;
}

static void*
test_memcpy_worker_1()
{
	while (!cond_memcpy1) { continue; }

	for (uint8_t k = 0; k < 50; k++) {
		printf("data[42] = %f\n", ssd_chunk_read_double(&chunk1, 42));
	}

	atomic_store_explicit(&cond_memcpy2, 1, memory_order_seq_cst);

	return 0;
}

void
test_memcpy()
{
	ssd_thread_add_trid();

	ssd_virtmem_pool_t virtmem_pool;
	ssd_virtmem_init(&virtmem_pool, 8 * (sizeof(ssd_superblock_header_t) + SSD_SUPERBLOCK_CAPACITY));

	ssd_heap_init(&heaps, &virtmem_pool);

	printf("mempool = %p\n", virtmem_pool.mapping);
	
	pthread_t thread;
	pthread_create(&thread, 0, ssd_thread_register_workload, (void*)test_memcpy_worker_0);

	test_memcpy_worker_1();	
	
	void* res;
	pthread_join(ssd_thread_pthread_wrapper(1), &res);

	ssd_heap_free(&heaps);
	ssd_virtmem_free(&virtmem_pool);
}
