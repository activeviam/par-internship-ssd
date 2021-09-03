#include <ssd_concurrent_queue.h>

void
ssd_concurrent_queue_push(ssd_concurrent_queue_t *queue, void_ptr new_head)
{
	void_ptr old_head = atomic_load_explicit(&queue->head, memory_order_relaxed);
	
	do {
		
		*(void_ptr*)new_head = old_head;
	
	} while (!atomic_compare_exchange_weak_explicit(
		&queue->head,
		&old_head,
		new_head,
		memory_order_release,
		memory_order_relaxed
	));
}

void_ptr
ssd_concurrent_queue_pop(ssd_concurrent_queue_t *queue)
{
	void_ptr old_tail = atomic_load_explicit(&queue->tail, memory_order_relaxed);
	uint64_t mask;

	do {
		
		if (!old_head) {
			return 0;
		}
		mask = (uint64_t)old_head & ~SIZE_MASK;
		new_head = *(void_ptr*)mask;

	} while (!atomic_compare_exchange_weak_explicit(
		&queue->head,
		&old_head,
		new_head,
		memory_order_release,
		memory_order_relaxed
	));

	return (void_ptr)mask;
}

int
ssd_concurrent_queue_empty(ssd_concurrent_queue_t *queue)
{
	return queue->head == 0;
}
