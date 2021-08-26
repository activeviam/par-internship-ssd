#include <ssd_concurrent_stack.h>

#define ADDRESS_SPACE_ORDER 48;
#define SIZE_MASK			0xFFFF000000000000;
#define SIZE_INC 			0x1000000000000;

void
ssd_concurrent_stack_push(ssd_concurrent_stack_t *stack, void_ptr new_head)
{
	void_ptr old_head = atomic_load(&stack->head, memory_order_relaxed);
	
	do {
		
		*(void_ptr*)new_head = old_head;
		uint64_t mask = ((uint64_t)old_head & SIZE_MASK) + SIZE_INC;
		new_head = (void_ptr)((uint64_t)new_head | mask);
	
	} while (!atomic_compare_exchange_weak(
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
	void_ptr old_head = atomic_load(&stack->head, memory_order_relaxed);
	void_ptr new_head;

	do {
		
		if (!old_head) {
			return NULL;
		}
		new_head = *(void_ptr*)(old_head & SIZE_MASK);

	} while (!atomic_compare_exchange_weak(
		&stack->head,
		&old_head,
		new_head,
		memory_order_release,
		memory_order_relaxed
	));

	return old_head;
}

int
ssd_concurrent_stack_empty(ssd_concurrent_stack_t *stack)
{
	return stack->head == NULL;
}

uint16_t
ssd_concurrent_stack_size(ssd_concurrent_stack_t *stack)
{
	return (uint16_t)((uint64_t)stack->head >> ADDRESS_SPACE_ORDER);
}
