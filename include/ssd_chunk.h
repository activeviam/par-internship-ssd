#ifndef __SSD_CHUNK_H__
#define __SSD_CHUNK_H__

#include <liburing.h>
#include <ssd_cache.h>
#include <stdint.h>
#include <sys/queue.h>

#define CHUNK_CACHE_MAXSIZE 32

struct io_ops_vec {
	double					*values;
	uint32_t				*keys;
	uint32_t				capacity;
	uint32_t				size;
};

struct io_block {
	TAILQ_ENTRY(io_block)	link;
	struct io_ops_vec		ops;
	uint32_t				id;
	uint8_t					pending;
	uint8_t					cacheline;
};

struct io_batch {
	TAILQ_ENTRY(io_batch) 				link;
	TAILQ_HEAD(io_block_head, io_block)	blocks;
	uint8_t								size;
};

struct ssd_chunk {
	
	struct io_uring			*uring;
	struct ssd_cache		*global_cache;
	
	uint64_t				offset;
	uint64_t				capacity;
	
	int32_t					fd;
	uint8_t					cache_actual_size;
	uint8_t					cache_current_line;
	uint8_t					cache_prediction_rate;
	uint8_t					cache_usage;

	struct iovec			cache_iovecs[CHUNK_CACHE_MAXSIZE];
	uint32_t				cache_ids[CHUNK_CACHE_MAXSIZE];
	uint8_t					cache_pending[CHUNK_CACHE_MAXSIZE];

	TAILQ_HEAD(io_batch_head, io_batch)	write_queue_entries;
	uint8_t								write_queue_size;
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
