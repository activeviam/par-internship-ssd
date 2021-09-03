#include <ssd_concurrent_stack.h>
#include <ssd_env.h>

#define SIZE_MASK			0xFFFF000000000000LU
#define SIZE_INC 			0x1000000000000LU

void
ssd_concurrent_stack_push(ssd_concurrent_stack_t *stack, void_ptr new_head)
{
	void_ptr old_head = atomic_load_explicit(&stack->head, memory_order_relaxed);
	
	do {
		
		*(void_ptr*)new_head = old_head;
		uint64_t mask = ((uint64_t)old_head & SIZE_MASK) + SIZE_INC;
		new_head = (void_ptr)((uint64_t)new_head | mask);
	
	} while (!atomic_compare_exchange_weak_explicit(
		&stack->head,
		&old_head,
		new_head,
		memory_order_release,
		memory_order_relaxed
	));
}

void_ptr
ssd_concurrent_stack_pop(ssd_concurrent_stack_t *stack)
{
	void_ptr old_head = atomic_load_explicit(&stack->head, memory_order_relaxed);
	void_ptr new_head;
	uint64_t mask;

	do {
		
		if (!old_head) {
			return 0;
		}
		mask = (uint64_t)old_head & ~SIZE_MASK;
		new_head = *(void_ptr*)mask;

	} while (!atomic_compare_exchange_weak_explicit(
		&stack->head,
		&old_head,
		new_head,
		memory_order_release,
		memory_order_relaxed
	));

	return (void_ptr)mask;
}

int
ssd_concurrent_stack_empty(ssd_concurrent_stack_t *stack)
{
	return stack->head == 0;
}

uint16_t
ssd_concurrent_stack_size(ssd_concurrent_stack_t *stack)
{
	return (uint16_t)((uint64_t)stack->head >> ADDRESS_SPACE_ORDER);
}
