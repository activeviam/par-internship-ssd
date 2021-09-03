#ifndef __SSD_CONCURRENT_STACK_H__
#define __SSD_CONCURRENT_STACK_H__

#include <stdatomic.h>
#include <stdint.h>

typedef void* void_ptr;

typedef struct ssd_concurrent_stack_s {
	volatile _Atomic void_ptr head;
} ssd_concurrent_stack_t;

void
ssd_concurrent_stack_push(ssd_concurrent_stack_t *stack, void_ptr new_head);

void_ptr
ssd_concurrent_stack_pop(ssd_concurrent_stack_t *stack);

int
ssd_concurrent_stack_empty(ssd_concurrent_stack_t *stack);

uint16_t
ssd_concurrent_stack_size(ssd_concurrent_stack_t *stack);

#endif
