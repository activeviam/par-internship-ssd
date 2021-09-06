#ifndef __SSD_SUPERBLOCK_HMAP_H__
#define __SSD_SUPERBLOCK_HMAP_H__

#include <ssd_superblock.h>

typedef struct ssd_superblock_hmap_s {
	ssd_superblock_header_ptr	*buckets;
	uint32_t					size;
	uint32_t					order;
} ssd_superblock_hmap_t;

int
ssd_superblock_hmap_init(ssd_superblock_hmap_t *hmap);

void
ssd_superblock_hmap_insert(ssd_superblock_hmap_t *hmap, ssd_superblock_header_ptr block);

ssd_superblock_header_ptr *
ssd_superblock_hmap_find(ssd_superblock_hmap_t *hmap, ssd_superblock_header_ptr superblock);

void
ssd_superblock_hmap_suggest(ssd_superblock_hmap_t 		*hmap,
						 	uint32_t					bsize,
						 	ssd_superblock_header_ptr	**it_ptr,
						 	ssd_block_header_ptr		*block_ptr);

ssd_superblock_header_ptr
ssd_superblock_hmap_remove(ssd_superblock_hmap_t *hmap, ssd_superblock_header_ptr *curr);

uint32_t
ssd_superblock_hmap_capacity(const ssd_superblock_hmap_t *hmap);

void
ssd_superblock_hmap_free(ssd_superblock_hmap_t *hmap);

#endif
