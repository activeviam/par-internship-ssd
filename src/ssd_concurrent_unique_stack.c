#include <ssd_concurrent_unique_stack.h>
#include <stdatomic.h>

int
ssd_concurrent_unique_stack_init(ssd_concurrent_stack_t *stack, uint32_t capacity)
{
	stack->table = malloc(sizeof(uint32_t) * capacity);
	if (!stack->table) {
		return -1;
	}
	stack->capacity = capacity;

	for (uint32_t i = 0; i < capacity; i++) {
		stack->table[i] = i + 1;
	}

	stack->head = 1;
	return 0;
}

void
ssd_concurrent_unique_stack_free(ssd_concurrent_stack_t *stack) {
	free(stack->table);
	stack->capacity = 0;
	stack->head = 1;
}

int
ssd_concurrent_unique_stack_push(ssd_concurrent_stack_t *stack, uint32_t new_elem)
{
	uint64_t old_head = atomic_load_explicit(&stack->head, memory_order_relaxed);
	uint64_t old_elem = old_head >> 32;
	uint64_t new_head = (new_elem << 32) | old_elem;
	uint32_t free = stack->capacity;

	if (!atomic_compare_exchange_strong_explicit(
		(volatile _Atomic uint32_t *)&(stack->table[new_elem]),
		&free,
		old_elem,
		memory_order_acq_rel,
		memory_order_relaxed
		))
	{
		return -1;
	}

	while (!atomic_compare_exchange_weak_explicit(
		&stack->head,
		&old_head,
		new_head,
		memory_order_acq_rel,
		memory_order_relaxed
		))
	{
		old_elem = old_head >> 32;
		new_head = (new_elem << 32) | old_elem;
	}
}

uint32_t
ssd_concurrent_unique_stack_pop(ssd_concurrent_stack_t *stack)
{
	uint64_t old_head, new_head;
	uint32_t old_elem, new_elem;

	uint64_t old_head = atomic_load_explicit(&stack->head, memory_order_relaxed);

	do {

		uint32_t old_elem = old_head >> 32;
		if (old_elem == stack->capacity) {
			return old_elem;
		}
		uint32_t new_elem = (old_head & 0xFFFFFFFF);
		uint64_t new_head = (new_elem << 32) | (new_elem < stack->capacity ? stack->table[new_elem] : 0);

	} while (!atomic_compare_exchange_weak_explicit(
		&stack->head,
		&old_head,
		new_head,
		memory_order_acq_rel,
		memory_order_relaxed
		));
	
	atomic_store_explicit(
		(volatile _Atomic uint32_t *)&(stack->table[old_elem]),
		stack->capacity,
		memory_order_release);

	return old_elem;
}

int
ssd_concurrent_unique_stack_empty(ssd_concurrent_stack_t *stack)
{
	return (stack->head >> 32) == stack->capacity;
}
