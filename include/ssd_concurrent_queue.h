#ifndef __SSD_CONCURRENT_QUEUE_H__
#define __SSD_CONCURRENT_QUEUE_H__

#include <stdatomic.h>
#include <stdint.h>

typedef void* void_ptr;

typedef struct ssd_concurrent_queue_s {
	volatile _Atomic void_ptr head;
	volatile _Atomic void_ptr tail;
} ssd_concurrent_queue_t;

void
ssd_concurrent_queue_push(ssd_concurrent_queue_t *queue, void_ptr new_head);

void_ptr
ssd_concurrent_queue_pop(ssd_concurrent_queue_t *queue);

int
ssd_concurrent_queue_empty(ssd_concurrent_queue_t *queue);

#endif
