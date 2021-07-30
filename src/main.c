#include <ssd_cache.h>
#include <ssd_chunk.h>
#include <pthread.h>
#include <stdio.h>

#define NUM_THREADS 1

int
main()
{
	/* Initialize SPDK environment */
	struct spdk_env_opts opts;
	spdk_env_opts_init(&opts);
	opts.name = "app";
	opts.shm_id = 0;
	if (spdk_env_init(&opts) < 0) {
		fprintf(stderr, "Unable to initialize SPDK env\n");
		return 1;
	}

	/* Initialize SSD NVMe controller */
	struct ctrlr_entry *ctrlr = ctrlr_entry_init(&opts, 21);
	struct ns_entry *ns = TAILQ_FIRST(&ctrlr->ns);

	uint32_t block_number = MAX_CHUNK_CACHESIZE;
	uint32_t block_size = 1 << ns->lb_order;

	/* Initialize RAM storage */
	void *membuf = spdk_dma_malloc(block_number * block_size, 0, NULL);

	/* Initialize RAM allocator */
	struct ssd_cache *cache = ssd_cache_init(block_number, block_size, membuf);
	
	/* Initialize SPDK IO queues */
	struct spdk_nvme_qpair *qpair = spdk_nvme_ctrlr_alloc_io_qpair(ctrlr->ctrlr, NULL, 0);
	
	/* Initialize a chunk */
	uint64_t capacity = 1 << 27;

	ssd_chunk *chunk = ssd_chunk_init(ctrlr, qpair, cache, sizeof(double) * capacity);
	
	/* Dump chunk info */
	ssd_chunk_print(chunk);

	{
		uint64_t beg = spdk_get_ticks();
		// Ordered write
		for (uint64_t i = 0; i < capacity; i++) {
			ssd_chunk_write_double(chunk, i, 0.1 * i);
		}
		uint64_t end = spdk_get_ticks();
		printf("test ord. write: 1 GiB / %5.3e ms\n", 1.0 * (end - beg) / spdk_get_ticks_hz() * 1000);
	}

	/* Free a chunk */
	ssd_chunk_free(chunk);
	/* Free SPDK IO queues */
	spdk_nvme_ctrlr_free_io_qpair(qpair);
	/* Free RAM allocator */
	ssd_cache_free(cache);
	/* Free RAM storage */
	spdk_free(membuf);
	/* Free SSD NVMe controller */
	ctrlr_entry_free(ctrlr);

	return 0;
}
