#include <ssd_cache.h>
#include <stdatomic.h>

struct ssd_cache_node {
	void					*entry;
	struct ssd_cache_node 	*next;
} *ssd_cache_nodeptr_t;

struct ssd_cache {
	struct ssd_cache_info		info;
	_Atomic ssd_cache_nodeptr_t head;
	ssd_cache_nodeptr_t 		stackarray;
};

const struct ssd_cache_info*
ssd_cache_get_info(ssd_cache *cache)
{
	return &cache->info;
}

struct ssd_cache*
ssd_cache_init(uint32_t block_order, uint32_t block_size, void *membuf)
{
	struct ssd_cache *stack = malloc(sizeof(struct ssd_cache));
	
	if (stack) {

		stack->info.membuf = membuf;
		stack->info.block_number = block_number;
		stack->info.block_order = block_order;

		stack->stackarray = malloc(nb_blocks * sizeof(struct ssd_cache_node));
		atomic_store_explicit(&stack->head, stack->stackarray, memory_order_relaxed);
		if (!stack->stackarray) {
			ssd_cache_free(stack);
			return NULL;
		}

		ssd_cache_handle_t head;

		head = atomic_load_explicit(&stack->head, memory_order_relaxed);

		char *bytebuf = (char*)membuf;
		uint32_t block_size = 1 << block_number;
		for (uint32_t i = 0; i < block_number - 1; i++) {
			head->entry = (void*)(bytebuf + i * block_size);
			head->next = head + 1;
			head = head->next;
		}

		head->entry = (void*)(bytebuf + (block_number - 1) * block_size);
		head->next = NULL;
	}

	return stack;
}

void
ssd_cache_free(struct ssd_cache *stack)
{
	if (stack->stackarray) {
		free(stack->stackarray);
	}

	free(stack);
}

void*
ssd_cache_pop(struct ssd_cache *stack)
{
	ssd_cache_nodeptr_t old_head, new_head;
	
	do {
		old_head = atomic_load_explicit(&stack->head, memory_order_acquire);
		if (!old_head) {
			return NULL;
		}
		new_head = old_head->next;
	} while (
		!atomic_compare_exchange_weak_explicit(
			&stack->head,
			&old_head,
			new_head,
			memory_order_release,
			memory_order_relaxed));

	void *entry = old_head->entry;
	old_head->entry = NULL;
	return entry;
}

void
ssd_cache_push(struct ssd_cache *stack, void *new_head_entry)
{	
	ssd_cache_nodeptr_t old_head, new_head;

	uint64_t offset = (uint64_t)new_head_entry - (uint64_t)stack->info.membuf;
	new_head = stack->stackarray + (offset / sizeof(struct ssd_cache_node);
	
	do {
		old_head = atomic_load_explicit(&stack->head, memory_order_acquire);
		new_head->next = old_head;
	} while (
		!atomic_compare_exchange_weak_explicit(
			&stack->head,
			&old_head,
			new_head,
			memory_order_release,
			memory_order_relaxed));
}

int
ssd_cache_empty(struct ssd_cache *stack)
{
	return stack->head == NULL;
}
