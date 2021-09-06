#ifndef __SSD_VIRTMEM_H__
#define __SSD_VIRTMEM_H__

#define SSD_SUPERBLOCK_SIZE (1 << 21)

#include <ssd_superblock_hmap.h>

typedef struct ssd_virtmem_pool_s {

	ssd_superblock_header_ptr	unused;

	ssd_superblock_hmap_t		hmap;

	pthread_mutex_t				mutex;

	void 						*mapping;
	uint32_t					num_superblocks;

} ssd_virtmem_pool_t;

int
ssd_virtmem_init(ssd_virtmem_pool_t *pool, uint64_t capacity);

void
ssd_virtmem_acquire(ssd_virtmem_pool_t *pool,
				uint32_t bsize,
				ssd_superblock_header_ptr *superblock_ptr,
				ssd_block_header_ptr *block_ptr);

void
ssd_virtmem_release(ssd_virtmem_pool_t *pool, ssd_superblock_header_ptr superblock);

void
ssd_virtmem_free(ssd_virtmem_pool_t *pool);

#endif
