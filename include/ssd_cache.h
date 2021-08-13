#ifndef __SSD_CACHE_H__
#define __SSD_CACHE_H__

#ifdef __cplusplus
extern "C" {
#endif

#include <stdlib.h>
#include <stdint.h>

struct ssd_cache_info {
	void		*membuf;
	uint32_t 	block_number;
	uint32_t 	block_order;
};

struct ssd_cache;

const struct ssd_cache_info*
ssd_cache_get_info(ssd_cache *cache);

struct ssd_cache*
ssd_cache_init(uint32_t block_number, uint32_t block_order, void *membuf);

void
ssd_cache_free(struct ssd_cache *cache);

void
ssd_cache_push(struct ssd_cache *cache, void *new_head);

void*
ssd_cache_pop(struct ssd_cache *cache);

int
ssd_cache_empty(struct ssd_cache *cache);

#ifdef __cplusplus
}
#endif

#endif
