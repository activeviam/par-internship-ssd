#include <boost/lockfree/stack.hpp>
#include <ssd_cache.h>

typedef boost::lockfree::stack<ssd_cache_handle_t, boost::lockfree::fixed_sized<true>> stack_t;

struct ssd_cache {
	stack_t 	*st;
	void 		*membuf;
	uint32_t	nb_blocks;
	uint32_t	block_size;
};

void*
ssd_cache_get_page(struct ssd_cache *cache, ssd_cache_handle_t handle)
{
	unsigned char *const byte_buf = static_cast<unsigned char*>(cache->membuf);
	uint64_t index = cache->block_size;
	index *= handle;
	return (void*)(byte_buf + index);
}

struct ssd_cache*
ssd_cache_init(uint32_t nb_blocks, uint32_t block_size, void *membuf)
{
	struct ssd_cache *cache = static_cast<struct ssd_cache*>(malloc(sizeof(struct ssd_cache)));
	
	if (cache) {

		cache->st = new stack_t(nb_blocks);
		cache->membuf = membuf;
		cache->nb_blocks = nb_blocks;
		cache->block_size = block_size;

		for (ssd_cache_handle_t i = 0; i < nb_blocks; i++) {
			cache->st->unsynchronized_push(i);
		}
	}

	return cache;
}

void
ssd_cache_free(struct ssd_cache *cache)
{
	delete cache->st;
	free(cache);
}

ssd_cache_handle_t
ssd_cache_pop(struct ssd_cache *cache)
{
	ssd_cache_handle_t value;
	if (cache->st->pop(value)) {
		return value;
	} else {
		return cache->nb_blocks;
	}
}

void
ssd_cache_push(struct ssd_cache *cache, ssd_cache_handle_t new_head)
{	
	cache->st->bounded_push(new_head);
}

int
ssd_cache_empty(struct ssd_cache *cache)
{
	return cache->st->empty();
}

int
ssd_cache_valid_handle(struct ssd_cache *cache, ssd_cache_handle_t handle)
{	
	return handle < cache->nb_blocks;
}
