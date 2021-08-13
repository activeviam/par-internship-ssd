#include <ssd_cache.h>
#include <pthread.h>
#include <stdio.h>

struct ssd_cache {
	struct ssd_cache_info	info;
	ssd_cache_handle_t		*entries;
	pthread_spinlock_t 		spinlock;
	uint32_t				head;
};

const struct ssd_cache_info*
ssd_cache_get_info(ssd_cache *cache)
{
	return &cache->info;
}

struct ssd_cache*
ssd_cache_init(uint32_t block_number, uint32_t block_order, void *membuf)
{
	struct ssd_cache *stack = malloc(sizeof(struct ssd_cache));
	
	if (stack) {

		stack->info.membuf = membuf;
		stack->info.block_number = block_number;
		stack->info.block_order = block_order;

		stack->entries = malloc(block_number * sizeof(ssd_cache_handle_t));
		if (!stack->entries) {
			ssd_cache_free(stack);
			return NULL;
		}

		int rc;
		rc = pthread_spin_init(&stack->spinlock, PTHREAD_PROCESS_PRIVATE);
		if (rc != 0) {
			fprintf(stderr, "pthread_spin_init() failed\n");
			ssd_cache_free(stack);
			return NULL;
		}

		char *bytebuf = (char*)membuf;
		uint32_t block_size = 1 << block_order;

		for (uint32_t i = 0; i < block_order; i++) {
			stack->entries[i] = (void*)(bytebuf + i * block_size);
		}

		stack->head = block_number;

	}

	return stack;
}

void
ssd_cache_free(struct ssd_cache *stack)
{
	if (stack->entries) {
		free(stack->entries);
	}

	pthread_spin_destroy(&stack->spinlock);

	free(stack);
}

void*
ssd_cache_pop(struct ssd_cache *stack)
{
	ssd_cache_handle_t head;

	if (pthread_spin_lock(&stack->spinlock) == 0) {
		
		if (ssd_cache_empty(stack)) {
			return NULL;
		}

		stack->head--;
		head = stack->entries[stack->head];
	}

	pthread_spin_unlock(&stack->spinlock);

	return head;
}

void
ssd_cache_push(struct ssd_cache *stack, void *new_head)
{
	if (pthread_spin_lock(&stack->spinlock) == 0) {	
		
		if (stack->head >= stack->info.block_number) {
			return;
		}

		stack->entries[stack->head] = new_head;
		stack->head++;
	}

	pthread_spin_unlock(&stack->spinlock);
}

int
ssd_cache_empty(struct ssd_cache *stack)
{
	return stack->head == 0;
}
