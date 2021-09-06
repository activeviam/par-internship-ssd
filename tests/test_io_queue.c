#include <sys/queue.h>
#include <pthread.h>
#include <stdlib.h>
#include <stdio.h>
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

	if (!entry) {
		pthread_mutex_unlock(&queue->mutex);
		return 0;
	} else {
		STAILQ_REMOVE_HEAD(&queue->head, link);
		pthread_mutex_unlock(&queue->mutex);
	}

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

uint32_t
atomic_increment()
{
	static _Atomic uint32_t count = 0;

	uint32_t old = count;
	uint32_t new;

	do {
		new = old + 1;
	} while (!atomic_compare_exchange_weak_explicit(
		&count,
		&old,
		new,
		memory_order_release,
		memory_order_relaxed
	));

	return old;
}

ssd_heap_pool_t heaps;

ssd_chunk_t *chunks;

struct threadsafe_queue alloc_queue, free_queue;

#define NUM_CHUNKS 4

static void*
test_io_queue_creator()
{
	uint8_t trid = ssd_thread_get_trid();

	chunks = malloc(NUM_CHUNKS * sizeof(ssd_chunk_t));

	for (uint16_t i = 0; i < NUM_CHUNKS; i++) {
		
		ssd_chunk_t *chunk = &chunks[i];
		uint32_t size = 512 << (rand() % 11);

		ssd_chunk_init(&heaps, chunk, size);
		printf("create: thread %u: chunk %d: superblock = %p, block = %lu: size = %u\n", trid, chunk->fd, chunk->superblock, (uint64_t)chunk->block, chunk->size);
		threadsafe_queue_push(&alloc_queue, chunk);
	}

	return 0;
}

static void*
test_io_queue_deleter()
{
	uint8_t trid = ssd_thread_get_trid();

	for (uint16_t i = 0; i < NUM_CHUNKS; i++) {

		ssd_chunk_t *chunk = 0;
		while (!chunk) {
			chunk = threadsafe_queue_pop(&free_queue);
		}
	
		printf("delete: thread %u: chunk %d: superblock = %p, block = %lu, data[42] = %f\n", trid, chunk->fd, chunk->superblock, (uint64_t)chunk->block, ssd_chunk_read_double(chunk, 42));
		ssd_chunk_free(chunk);
	}	

	free(chunks);

	return 0;
}

static void*
test_io_queue_worker()
{
	uint8_t trid = ssd_thread_get_trid();
	
	uint32_t inc;

	while ((inc = atomic_increment()) < NUM_CHUNKS) {
		
		ssd_chunk_t *chunk = 0;
		while (!chunk) {
			chunk = threadsafe_queue_pop(&alloc_queue);
		}

		uint32_t max_j = chunk->size >> 3;

		for (uint32_t j = 0; j < max_j; j++) {
			ssd_chunk_write_double(chunk, j, 0.1 * j);
		}

		printf("io ops: thread %u: chunk %d: superblock = %p, block = %lu\n", trid, chunk->fd, chunk->superblock, (uint64_t)chunk->block);
		
		threadsafe_queue_push(&free_queue, chunk);
	}	

	return 0;
}

void
test_io_queue()
{
	ssd_thread_add_trid();

	ssd_virtmem_pool_t virtmem_pool;
	ssd_virtmem_init(&virtmem_pool, 24 * (sizeof(ssd_superblock_header_t) + SSD_SUPERBLOCK_CAPACITY));

	ssd_heap_init(&heaps, &virtmem_pool);

	printf("mempool = %p\n", virtmem_pool.mapping);
	
	threadsafe_queue_init(&alloc_queue);
	threadsafe_queue_init(&free_queue);
	
	pthread_t thread;
	pthread_create(&thread, 0, ssd_thread_register_workload, (void*)test_io_queue_deleter);

	for (uint8_t trid = 2; trid < SSD_MAX_NUM_THREADS; trid++) {
		pthread_t thread;
		pthread_create(&thread, 0, ssd_thread_register_workload, (void*)test_io_queue_worker);
	}

	test_io_queue_creator();	
	
	void* res;
	for (uint8_t trid = 1; trid < SSD_MAX_NUM_THREADS; trid++) {
		pthread_join(ssd_thread_pthread_wrapper(trid), &res);
	}

	threadsafe_queue_free(&free_queue);
	threadsafe_queue_free(&alloc_queue);

	ssd_heap_free(&heaps);
	ssd_virtmem_free(&virtmem_pool);
}

