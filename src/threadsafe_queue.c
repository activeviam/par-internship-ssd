#include <threadsafe_queue.h>
#include <stdlib.h>

int
threadsafe_queue_init(struct threadsafe_queue *queue)
{
	STAILQ_INIT(&queue->head);
	pthread_mutex_init(&queue->mutex, 0);
	return 0;
}

ssd_chunk_t*
threadsafe_queue_pop(struct threadsafe_queue *queue)
{
	struct threadsafe_queue_entry *entry;

	pthread_mutex_lock(&queue->mutex);
	entry = STAILQ_FIRST(&queue->head);
	STAILQ_REMOVE_HEAD(&queue->head, link);
	pthread_mutex_unlock(&queue->mutex);

	if (!entry)
		return NULL;

	ssd_chunk_t *chunk = entry->chunk;
	free(entry);

	return chunk;
}

void
threadsafe_queue_push(struct threadsafe_queue *queue, ssd_chunk_t *chunk)
{
	struct threadsafe_queue_entry *entry;

	entry = malloc(sizeof(struct threadsafe_queue_entry));
	entry->chunk = chunk;

	pthread_mutex_lock(&queue->mutex);
	STAILQ_INSERT_TAIL(&queue->head, entry, link);
	pthread_mutex_unlock(&queue->mutex);
}

void
threadsafe_queue_free(struct threadsafe_queue *queue)
{
	pthread_mutex_destroy(&queue->mutex);
}

