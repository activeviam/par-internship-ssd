#ifndef __SSD_CHUNK_H__
#define __SSD_CHUNK_H__

#include <liburing.h>
#include <ssd_cache.h>
#include <stdint.h>

#define MAX_CHUNK_CACHESIZE 16

struct ssd_chunk {

	struct io_uring		*uring;
	struct ssd_cache	*global_cache;
	
	uint64_t			offset;
	uint64_t			capacity;
	
	int32_t				fd;
	uint8_t				cache_actual_size;
	uint8_t				cache_current_line;
	uint8_t				cache_prediction_rate;
	uint8_t				cache_timeout;

	struct iovec		cache_iovecs[MAX_CHUNK_CACHESIZE];
	uint32_t			cache_ids[MAX_CHUNK_CACHESIZE];
	uint8_t				cache_pending[MAX_CHUNK_CACHESIZE];
};

struct ssd_chunk*
ssd_chunk_init(struct io_uring 		*uring,
			   struct ssd_storage	*storage,
			   struct ssd_cache		*global_cache,
			   off_t 				capacity,
			   uint8_t				initial_hpr = 100);

double
ssd_chunk_read_double(struct ssd_chunk *chunk, uint64_t pos);

void
ssd_chunk_write_double(struct ssd_chunk *chunk, uint64_t pos, double value);

void
ssd_chunk_free(struct ssd_chunk *chunk);

void
ssd_chunk_sync(struct ssd_chunk *chunk);

void
ssd_chunk_print(const struct ssd_chunk *chunk);

#endif
