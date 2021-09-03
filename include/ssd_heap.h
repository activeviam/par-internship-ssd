#ifndef __SSD_HEAP_H__
#define __SSD_HEAP_H__

#include <ssd_thread.h>
#include <ssd_virtmem.h>
#include <ssd_superblock_hmap.h>

#include <sys/queue.h>

typedef struct ssd_superblock_tailq_s {
	TAILQ_HEAD(, ssd_superblock_header_s)	head;
	size_t									size;
} ssd_superblock_tailq_t;

typedef struct ssd_heap_s {
	ssd_superblock_hmap_t	hmap;
	ssd_superblock_tailq_t	hot_tailq;
	ssd_superblock_tailq_t	cold_tailq;
} ssd_heap_t;

typedef struct ssd_heap_pool_s {
	ssd_virtmem_pool_t	*vmem;
	ssd_heap_t			heaps[SSD_MAX_NUM_THREADS];
} ssd_heap_pool_t; 

int
ssd_heap_init(ssd_heap_pool_t *pool, ssd_virtmem_pool_t *vmem);

int
ssd_heap_allocate(ssd_heap_pool_t			*pool,
				  ssd_superblock_header_ptr *superblock_ptr,
				  ssd_block_header_ptr 		*block_ptr,
				  uint32_t 					capacity);

void
ssd_heap_deallocate(ssd_superblock_header_ptr superblock, ssd_block_header_ptr block);

void
ssd_heap_free(ssd_heap_pool_t *pool);

#endif
