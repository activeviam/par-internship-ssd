#ifndef __SSD_HEAP_H__
#define __SSD_HEAP_H__

#include <ssd_virtmem.h>
#include <ssd_superblock_hmap.h>

TAILQ_HEAD(ssd_superblock_tailq_s, ssd_superblock_s);
typedef struct ssd_superblock_tailq_s ssd_superblock_tailq_t;

typedef struct ssd_heap_pool_s ssd_heap_pool_t;

typedef struct ssd_heap_s {
	ssd_superblock_hmap_t	hmap;
	ssd_superblock_tailq_t	hot_tailq;
	ssd_superblock_tailq_t	cold_tailq;
	ssd_heap_pool_t			*pool;
} ssd_heap_t;

struct ssd_heap_pool_s {
	ssd_virtmem_pool_t	*vmem;
	ssd_heap_t			heap_pool[MAX_NUM_THREADS];
}; 

void
ssd_heap_init(ssd_heap_pool_t *pool);

int
ssd_heap_allocate(ssd_heap_pool_t *pool, ssd_chunk *chunk);

void
ssd_heap_deallocate(ssd_heap_pool_t *pool, ssd_chunk *chunk);

void
ssd_heap_free(ssd_heap_pool_t *pool);

#endif
