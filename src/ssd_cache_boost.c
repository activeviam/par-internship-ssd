#include <boost/lockfree/stack.hpp>
#include <ssd_cache.h>

typedef boost::lockfree::stack<void*, boost::lockfree::fixed_sized<true>> stack_t;

struct ssd_cache {
	struct ssd_cache_info	info;
	stack_t 				*st;
};

const struct ssd_cache_info*
ssd_cache_get_info(ssd_cache *cache)
{
	return &cache->info;
}

struct ssd_cache*
ssd_cache_init(uint32_t block_number, uint32_t block_order, void *membuf)
{
	struct ssd_cache *cache = static_cast<struct ssd_cache*>(malloc(sizeof(struct ssd_cache)));
	
	if (cache) {

		cache->info.membuf = membuf;
		cache->info.block_number = block_number;
		cache->info.block_order = block_order;

		cache->st = new stack_t(block_number);
		char *bytebuf = (char*)membuf;
		uint32_t block_size = 1 << block_order;
		for (uint32_t i = 0; i < block_number; i++) {
			void *addr = (void*)(bytebuf + i * block_size);
			cache->st->unsynchronized_push(addr);
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

void*
ssd_cache_pop(struct ssd_cache *cache)
{
	void *value;
	if (cache->st->pop(value)) {
		return value;
	} else {
		return NULL;
	}
}

void
ssd_cache_push(struct ssd_cache *cache, void *new_head)
{	
	cache->st->bounded_push(new_head);
}

int
ssd_cache_empty(struct ssd_cache *cache)
{
	return cache->st->empty();
}
