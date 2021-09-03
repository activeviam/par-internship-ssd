#ifndef __SSD_VIRTMEM_H__
#define __SSD_VIRTMEM_H__

#define SSD_SUPERBLOCK_SIZE (1 << 21)

#include <ssd_concurrent_stack.h>

typedef struct ssd_virtmem_pool_s {
	ssd_concurrent_stack_t	stack;
	void 					*mapping;
	uint64_t				capacity;
} ssd_virtmem_pool_t;

int
ssd_virtmem_init(ssd_virtmem_pool_t *pool, uint64_t capacity);

void
ssd_virtmem_free(ssd_virtmem_pool_t *pool);

#endif
