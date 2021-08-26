#ifndef __SSD_CHUNK_H__
#define __SSD_CHUNK_H__

#include <ssd_cache.h>
#include <stdint.h>
#include <sys/queue.h>

struct ssd_chunk {
	ssd_block_header_ptr		header;
	void						*data;
	volatile _Atomic int32_t	state;
	int32_t						fd;
	uint32_t					size;
};

void
ssd_chunk_init(struct ssd_chunk *chunk, uint32_t size);

double
ssd_chunk_read_double(struct ssd_chunk *chunk, uint32_t pos);

void
ssd_chunk_write_double(struct ssd_chunk *chunk, uint32_t pos, double value);

void
ssd_chunk_free(struct ssd_chunk *chunk);

void
ssd_chunk_print(const struct ssd_chunk *chunk);

#endif
