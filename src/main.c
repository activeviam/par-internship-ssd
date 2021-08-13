#include <ssd_cache.h>
#include <ssd_allocator.h>
#include <ssd_chunk.h>
#include <pthread.h>
#include <stdio.h>
#include <string.h>
#include <liburing.h>
#include <time.h>

#define NUM_THREADS 1U
#define QUEUE_DEPTH 64U
#define IO_BLOCK_ORDER 20U

int
main()
{
	int rc;

	uint32_t block_number = MAX_CHUNK_CACHESIZE;
	uint32_t block_order = IO_BLOCK_ORDER;

	/* Initialize RAM storage */
	void *membuf = malloc(block_number << block_order);

	/* Initialize RAM allocator */
	struct ssd_cache *ram_cache = ssd_cache_init(block_number, block_order, membuf);

	const char *filename = "../../hugetlbfs/chunk.ssd";
	struct ssd_storage storage;
	ssd_storage_init(&storage, filename, 1 << 30);
	
	struct io_uring uring;
	rc = io_uring_queue_init(QUEUE_DEPTH, &uring, 0);
	if (rc < 0) {
		fprintf(stderr, "queue init fali: %s\n", strerror(-rc));
		return -1;
	}

	// Setup
	uint32_t n = 1 << 22;
	off_t capacity = n * sizeof(double);
	ssd_chunk *chunk = ssd_chunk_init(&uring, &storage, ram_cache, capacity);
	
	/*
	uint32_t nb = 4;
	uint32_t len = n / nb;
	for (uint32_t k = 0; k < nb; k++) {
		for (uint64_t i = k * len; i < (k+1) * len; i++) {
			ssd_chunk_write_double(chunk, i, 42.);
		}ssd_chunk_print(chunk);
	}*/

	for (uint64_t i = 0; i < n; i++) {
		ssd_chunk_write_double(chunk, i, 42.);
	}
	ssd_chunk_sync(chunk);
	ssd_chunk_print(chunk);
	
	double avg = 0;
	double rec;

	// Tests
	for (int num_iter = 0; num_iter < 10; num_iter++) {
		clock_t beg = clock();
		for (uint64_t i = 0, j = 0; i < n; i++) {
			j = (j + 999999) % capacity;
			rec = ssd_chunk_read_double(chunk, j);
		}
		ssd_chunk_sync(chunk);
		clock_t end = clock();
		
		double duration = 100.0 * (end - beg) / CLOCKS_PER_SEC;
		printf("test ord. read #%d, speed = 1 GiB / %5.5e ms\n", num_iter, duration * 20);
		
		if (num_iter >= 5) {
			avg += duration * 2;
		}
	}
	printf("test ord. read: average speed = 1 GiB / %5.3e ms = %5.3e GiB / s\n", avg, 1000. / avg);

	if (rec != 42.) {
		fprintf(stderr, "error, rec = %f\n", rec);
	}

	ssd_chunk_print(chunk);

	// Free a chunk
	ssd_chunk_free(chunk);

	// Free RAM allocator
	ssd_cache_free(ram_cache);
	// Stop io_uring context 
	io_uring_queue_exit(&uring);
	// Free RAM storage
	free(membuf);
	// Free SSD storage 
	ssd_storage_exit(&storage);

	return 0;
}
