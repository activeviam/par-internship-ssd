#ifndef __SSD_CACHE_H__

#ifdef __cplusplus
extern "C" {
#endif

#include <stdlib.h>
#include <stdint.h>

struct ssd_cache;

#ifdef INTELTBB
	#include <ssd_inteltbb_cache.h>
#endif

#ifdef CUSTOM_LOCKFREE
struct ssd_cache_node {
	struct ssd_cache_node 	*next;
	uint32_t				entry;
};

typedef struct ssd_cache_node *ssd_cache_handle_t;
#endif

#ifdef CUSTOM_SPINLOCK
typedef uint32_t ssd_cache_handle_t;
#endif

#ifdef BOOST
typedef uint32_t ssd_cache_handle_t;
#endif

void*
ssd_cache_get_page(struct ssd_cache *stack, ssd_cache_handle_t handle);

struct ssd_cache*
ssd_cache_init(uint32_t block_number, uint32_t block_size, void *membuf);

void
ssd_cache_free(struct ssd_cache *stack);

void
ssd_cache_push(struct ssd_cache *stack, ssd_cache_handle_t new_head);

ssd_cache_handle_t
ssd_cache_pop(struct ssd_cache *stack);

int
ssd_cache_empty(struct ssd_cache *stack);

int
ssd_cache_valid_handle(struct ssd_cache *stack, ssd_cache_handle_t handle);

#ifdef __cplusplus
}
#endif

#endif
