#ifndef __SSD_SUPERBLOCK_HMAP_H__
#define __SSD_SUPERBLOCK_HMAP_H__

#include <ssd_superblock.h>

typedef struct ssd_superblock_hmap_s {
	ssd_superblock_header_ptr	*buckets;
	uint32_t					size;
	uint32_t					order;
} ssd_superblock_hmap_t;

ssd_superblock_header_ptr
ssd_superblock_hmap_insert(ssd_superblock_hmap_t *hmap, ssd_superblock_header_ptr block);

ssd_superblock_header_ptr*
ssd_superblock_hmap_find(ssd_superblock_hmap_t *hmap, uint32_t bsize);

ssd_superblock_header_ptr
ssd_superblock_hmap_remove(ssd_superblock_hmap_t *hmap, ssd_superblock_header_ptr *curr);

#endif
