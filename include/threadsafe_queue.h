#ifndef __THREADSAFE_QUEUE_H__
#define __THREADSAFE_QUEUE_H__

#include <sys/queue.h>
#include <pthread.h>
#include <ssd_chunk.h>

struct threadsafe_queue_entry {
	ssd_chunk_t								*chunk;
	STAILQ_ENTRY(threadsafe_queue_entry)	link;
};

struct threadsafe_queue {
	STAILQ_HEAD(threadsafe_queue_head, threadsafe_queue_entry)	head;
	pthread_mutex_t												mutex;
};

int
threadsafe_queue_init(struct threadsafe_queue *queue);

ssd_chunk_t*
threadsafe_queue_pop(struct threadsafe_queue *queue);

void
threadsafe_queue_push(struct threadsafe_queue *queue, ssd_chunk_t *chunk);

void
threadsafe_queue_free(struct threadsafe_queue *queue);

#endif
