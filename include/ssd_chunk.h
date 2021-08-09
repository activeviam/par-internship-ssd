#ifndef __SSD_CHUNK_H__

#include <spdk_context.h>
#include <ssd_cache.h>
#include <stdint.h>

#define MAX_CHUNK_CACHESIZE 16

struct ssd_chunk_cache {
	ssd_cache_handle_t	lbs[MAX_CHUNK_CACHESIZE];
	uint32_t			ids[MAX_CHUNK_CACHESIZE];
	uint8_t				pending[MAX_CHUNK_CACHESIZE];
	uint8_t				actual_size;
	uint8_t				curr_cacheline;
	uint8_t				hit_prediction_rate;
};

struct ssd_lbid {
	uint32_t index;
	uint32_t offset;
};

struct ssd_chunk {
	struct ns_entry			*ns;
	struct spdk_nvme_qpair	*qpair;
	struct ssd_cache		*global_cache;
	ssd_chunk_cache			local_cache;
	uint32_t				lb_offset;
	uint32_t				capacity;
};

struct ssd_chunk*
ssd_chunk_init(struct ctrlr_entry 		*ctrlr,
			   struct spdk_nvme_qpair 	*qpair,
			   struct ssd_cache			*cache,
			   uint64_t 				capacity,
			   uint8_t					initial_hpr = 100);

double
ssd_chunk_read_double(struct ssd_chunk *chunk, uint64_t pos);

void
ssd_chunk_write_double(struct ssd_chunk *chunk, uint64_t pos, double value);

void
ssd_chunk_free(struct ssd_chunk *chunk);

void
ssd_chunk_print(const struct ssd_chunk *chunk);

void
ssd_chunk_sync(struct ssd_chunk *chunk);

#endif
