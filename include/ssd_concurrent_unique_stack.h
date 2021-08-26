#ifndef __SSD_CONCURRENT_UNIQUE_STACK_H__
#define __SSD_CONCURRENT_UNIQUE_STACK_H__

#include <stdint.h>
#include <stdatomic.h>

typedef struct ssd_concurrent_unique_stack_s {
			uint32_t 	*table;
			uint32_t	capacity;
	_Atomic	uint64_t	head;
} ssd_concurrent_stack_t;

ssd_concurrent_stack_t*
ssd_concurrent_unique_stack_init(uint32_t capacity);

void
ssd_concurrent_unique_stack_free(ssd_concurrent_stack_t_t *stack);

void
ssd_concurrent_unique_stack_push(ssd_concurrent_stack_t *stack, uint32_t new_head);

void*
ssd_concurrent_unique_stack_pop(ssd_concurrent_stack_t *stack);

int
ssd_concurrent_unique_stack_empty(ssd_concurrent_stack_t *stack);

#endif
