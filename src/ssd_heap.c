#include <ssd_heap.h>
#include <stdio.h>

#define ALIGN(size) ((size & 0xFFFFFFFFFFFFFFF8) + (8 * !(!(size & 0x7))))

#define SSD_SUPERBLOCK_THRESHOLD_USAGE 0.25f

#define SSD_MAX_BLOCK_SIZE (SSD_SUPERBLOCK_CAPACITY - sizeof(ssd_superblock_header_t) - sizeof(ssd_block_header_t))

static void
heap_recalculate_usage(ssd_heap_t *heap, ssd_superblock_header_ptr superblock)
{
	float old_usage = superblock->usage;

	uint16_t size = ssd_concurrent_stack_size(&superblock->stack);

	uint32_t offset = sizeof(ssd_block_header_t) + superblock->bsize;
	uint16_t max_size = SSD_SUPERBLOCK_CAPACITY / offset; 
	
	float new_usage = (1.f * (max_size - size)) / max_size;

	if ((old_usage < SSD_SUPERBLOCK_THRESHOLD_USAGE) &&
		(new_usage >= SSD_SUPERBLOCK_THRESHOLD_USAGE))
		{
			TAILQ_REMOVE(&(heap->cold_tailq.head), superblock, tailq);
			heap->cold_tailq.size--;
			TAILQ_INSERT_HEAD(&(heap->hot_tailq.head), superblock, tailq);
			heap->hot_tailq.size++;
		} 

	if ((old_usage >= SSD_SUPERBLOCK_THRESHOLD_USAGE) &&
		(new_usage < SSD_SUPERBLOCK_THRESHOLD_USAGE))
		{
			TAILQ_REMOVE(&(heap->hot_tailq.head), superblock, tailq);
			heap->hot_tailq.size--;
			TAILQ_INSERT_TAIL(&(heap->cold_tailq.head), superblock, tailq);
			heap->cold_tailq.size++;
		}

	if (new_usage == 0.f) {
		TAILQ_REMOVE(&(heap->cold_tailq.head), superblock, tailq);
		TAILQ_INSERT_HEAD(&(heap->cold_tailq.head), superblock, tailq);
	}

	superblock->usage = new_usage;
}

static int
heap_rebalance(ssd_heap_pool_t *pool, uint8_t trid)
{
	ssd_heap_t *heap = &pool->heaps[trid];

	ssd_superblock_header_ptr curr, next;

	for (curr = TAILQ_FIRST(&heap->cold_tailq.head); curr != 0; curr = next) {
		next = TAILQ_NEXT(curr, tailq);
		heap_recalculate_usage(heap, curr);
	}
	
	for (curr = TAILQ_FIRST(&heap->hot_tailq.head); curr != 0; curr = next) {
		next = TAILQ_NEXT(curr, tailq);
		heap_recalculate_usage(heap, curr);
	}

	ssd_superblock_header_ptr superblock = TAILQ_FIRST(&(heap->cold_tailq.head));

	if (superblock &&
	   ((superblock->usage == 0.f) ||
		(heap->cold_tailq.size + heap->hot_tailq.size > pool->vmem->num_superblocks / ssd_thread_get_num())))
	{
		TAILQ_REMOVE(&(heap->cold_tailq.head), superblock, tailq);
		heap->cold_tailq.size--;

		ssd_virtmem_release(pool->vmem, superblock);
		return 1;
	}

	return 0;
}

int
ssd_heap_init(ssd_heap_pool_t *pool, ssd_virtmem_pool_t *vmem)
{
	if (!pool) {
		fprintf(stderr, "NULL arg of type ssd_heap_pool_t * at ssd_heap_init()");
		return -1;
	}

	if (!vmem) {
		fprintf(stderr, "NULL arg of type ssd_virtmem_pool_t * at ssd_heap_init()");
		return -1;
	}

	pool->vmem = vmem;

	for (uint8_t trid = 0; trid < SSD_MAX_NUM_THREADS; trid++) {
		
		ssd_heap_t *heap = &pool->heaps[trid];

		ssd_superblock_hmap_init(&heap->hmap);

		TAILQ_INIT(&(heap->hot_tailq.head));
		heap->hot_tailq.size = 0;

		TAILQ_INIT(&(heap->cold_tailq.head));
		heap->cold_tailq.size = 0;
	}

	return 0;
}

int
ssd_heap_allocate(ssd_heap_pool_t			*pool,
				  ssd_superblock_header_ptr *superblock_ptr,
				  ssd_block_header_ptr 		*block_ptr,
				  uint32_t 					capacity)
{
	uint8_t trid = ssd_thread_get_trid();
	
	ssd_heap_t *heap = &pool->heaps[trid];
	
	uint32_t bsize = ALIGN(capacity);

	if ((bsize < 24) || (bsize > SSD_MAX_BLOCK_SIZE)) {
		fprintf(stderr, "block of capacity %u cannot be allocated in heap, ", capacity);
		fprintf(stderr, "the limits are (16, %lu] bytes\n", SSD_MAX_BLOCK_SIZE);
		return -1;
	}
	
	ssd_superblock_header_ptr *it;

	ssd_superblock_hmap_suggest(&heap->hmap, bsize, &it, block_ptr);
	
	if (it) {
	
		*superblock_ptr = *it;
		return heap_rebalance(pool, trid);
	}

	ssd_virtmem_acquire(pool->vmem, bsize, superblock_ptr, block_ptr);
	
	if (*superblock_ptr) {

		if (!(*block_ptr)) {
			ssd_superblock_reinit(*superblock_ptr, bsize);
			*block_ptr = (ssd_block_header_ptr)ssd_concurrent_stack_pop(&(*superblock_ptr)->stack);
		}
		
		ssd_superblock_hmap_insert(&heap->hmap, *superblock_ptr);
		
		TAILQ_INSERT_TAIL(&heap->cold_tailq.head, *superblock_ptr, tailq);
		heap->cold_tailq.size++;

		return heap_rebalance(pool, trid);
	}

	if (heap->cold_tailq.size > 0) {	

		*superblock_ptr = TAILQ_FIRST(&heap->cold_tailq.head);

		ssd_superblock_header_ptr *it = ssd_superblock_hmap_find(&heap->hmap, *superblock_ptr);
		ssd_superblock_hmap_remove(&heap->hmap, it);
		
		ssd_superblock_reinit(*superblock_ptr, bsize);
		ssd_superblock_hmap_insert(&heap->hmap, *superblock_ptr);
		
		*block_ptr = (ssd_block_header_ptr)ssd_concurrent_stack_pop(&(*superblock_ptr)->stack);
		return heap_rebalance(pool, trid);
	}

	uint64_t offset = SSD_SUPERBLOCK_CAPACITY + sizeof(ssd_superblock_header_t);

	*superblock_ptr = (ssd_superblock_header_ptr)((uint8_t*)pool->vmem->mapping + offset * trid);
	
	ssd_superblock_reinit(*superblock_ptr, bsize);
	ssd_superblock_hmap_insert(&heap->hmap, *superblock_ptr);
		
	TAILQ_INSERT_TAIL(&heap->cold_tailq.head, *superblock_ptr, tailq);
	heap->cold_tailq.size++;

	*block_ptr = (ssd_block_header_ptr)ssd_concurrent_stack_pop(&(*superblock_ptr)->stack);
	
	return heap_rebalance(pool, trid);
}

void
ssd_heap_deallocate(ssd_superblock_header_ptr superblock,
					ssd_block_header_ptr block)
{
	block->owner = 0;
	ssd_concurrent_stack_push(&superblock->stack, (void*)block);
}

void
ssd_heap_free(ssd_heap_pool_t *pool)
{
	for (uint8_t trid = 0; trid < SSD_MAX_NUM_THREADS; trid++) {
		ssd_heap_t *heap = &pool->heaps[trid];
		ssd_superblock_hmap_free(&heap->hmap);
	}

	ssd_superblock_hmap_free(&pool->vmem->hmap);
}
