#include <ssd_cache.h>
#include <stdatomic.h>

struct ssd_cache {
	
	_Atomic ssd_cache_handle_t 	head;
	ssd_cache_handle_t			stackarray;

	void 						*membuf;
	uint32_t					nb_blocks;
	uint32_t					block_size;
};

void*
ssd_cache_get_page(ssd_cache_handle_t handle)
{
	return handle->entry;
}

struct ssd_cache*
ssd_cache_init(uint32_t nb_blocks, uint32_t block_size, void *membuf)
{
	struct ssd_cache *stack = malloc(sizeof(struct ssd_cache));
	
	if (stack) {

		stack->membuf = membuf;
		stack->nb_blocks = nb_blocks;
		stack->block_size = block_size;

		stack->stackarray = malloc(nb_blocks * sizeof(struct ssd_cache_node));
		atomic_store_explicit(&stack->head, stack->stackarray, memory_order_relaxed);
		if (!stack->stackarray) {
			ssd_cache_free(stack);
			return NULL;
		}

		ssd_cache_handle_t head;

		head = atomic_load_explicit(&stack->head, memory_order_relaxed);

		char *bytebuf = (char*)membuf;
		for (uint32_t i = 0; i < nb_blocks - 1; i++) {
			head->entry = (void*)(bytebuf + i * block_size);
			head->next = head + 1;
			head = head->next;
		}

		head->entry = (void*)(bytebuf + (nb_blocks - 1) * block_size);
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

ssd_cache_handle_t
ssd_cache_pop(struct ssd_cache *stack)
{
	ssd_cache_handle_t old_head, new_head;
	
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

	return old_head;
}

void
ssd_cache_push(struct ssd_cache *stack, ssd_cache_handle_t new_head)
{	
	ssd_cache_handle_t old_head;
	
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

int
ssd_cache_valid_handle(ssd_cache_handle_t handle)
{	
	return handle != 0;
}

