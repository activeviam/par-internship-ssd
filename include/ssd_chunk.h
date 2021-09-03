#ifndef __SSD_CHUNK_H__
#define __SSD_CHUNK_H__

#include <ssd_superblock.h>
#include <ssd_heap.h>

typedef struct ssd_chunk_s {
	
	ssd_superblock_header_ptr	superblock;
	ssd_block_header_ptr		block;
	ssd_heap_pool_t				*pool;
	
	int32_t						fd;
	uint32_t					size;

	void						*data;

	pthread_mutex_t				mutex;

} ssd_chunk_t;

int
ssd_chunk_init(ssd_heap_pool_t *pool, ssd_chunk_t *chunk, uint32_t size);

double
ssd_chunk_read_double(ssd_chunk_t *chunk, uint32_t pos);

void
ssd_chunk_write_double(ssd_chunk_t *chunk, uint32_t pos, double value);

void
ssd_chunk_free(ssd_chunk_t *chunk);

void
ssd_chunk_print(const ssd_chunk_t *chunk);

#endif
