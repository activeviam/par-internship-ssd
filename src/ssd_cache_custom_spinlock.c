#include <ssd_cache.h>
#include <pthread.h>
#include <stdio.h>

struct ssd_cache {
	
	ssd_cache_handle_t	*entries;
	pthread_spinlock_t 	spinlock;

	void				*membuf;
	uint32_t 			nb_blocks;
	uint32_t			block_size;

	uint32_t			head;
};

void*
ssd_cache_get_page(struct ssd_cache *stack, ssd_cache_handle_t handle)
{
	unsigned char *const byte_buf = stack->membuf;
	uint64_t index = stack->block_size;
	index *= handle;
	return (void*)(byte_buf + index);
}

struct ssd_cache*
ssd_cache_init(uint32_t nb_blocks, uint32_t block_size, void *membuf)
{
	struct ssd_cache *stack = malloc(sizeof(struct ssd_cache));
	
	if (stack) {

		stack->membuf = membuf;
		stack->nb_blocks = nb_blocks;
		stack->block_size = block_size;

		stack->entries = malloc(nb_blocks * sizeof(ssd_cache_handle_t));
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

		for (uint32_t i = 0; i < nb_blocks; i++) {
			stack->entries[i] = i;
		}

		stack->head = nb_blocks;

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

ssd_cache_handle_t
ssd_cache_pop(struct ssd_cache *stack)
{
	ssd_cache_handle_t head;

	if (pthread_spin_lock(&stack->spinlock) == 0) {
		
		if (ssd_cache_empty(stack)) {
			return stack->nb_blocks;
		}

		stack->head--;
		head = stack->entries[stack->head];
	}

	pthread_spin_unlock(&stack->spinlock);

	return head;
}

void
ssd_cache_push(struct ssd_cache *stack, ssd_cache_handle_t new_head)
{
	if (pthread_spin_lock(&stack->spinlock) == 0) {	
		
		if (stack->head >= stack->nb_blocks) {
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

int
ssd_cache_valid_handle(struct ssd_cache *stack, ssd_cache_handle_t handle)
{
	return handle < stack->nb_blocks;
}
