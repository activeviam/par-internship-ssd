#include <ssd_superblock.h>

void
ssd_superblock_init(ssd_superblock_header_ptr superblock, uint32_t bsize)
{
	pthread_rwlock_init(&superblock->rwlock, 0);
	superblock->link = NULL;
	superblock->bsize = bsize;

	uint8_t *iter = (uint8_t*)(superblock + 1);
	uint32_t offset = sizeof(ssd_block_header_t) + bsize;
	for (uint32_t stack_size = SSD_SUPERBLOCK_CAPACITY / offset; stack_size > 0; stack_size--) {
		
		ssd_block_header_ptr block = (ssd_block_header_ptr)iter;
		
		if (stack_size == 1) {
			block->next = NULL;
		} else {
			uint64_t val = (uint64_t)((uint8_t*)block + offset);
			val |= (stack_size << ADDRESS_SPACE_ORDER);
			block->next = (ssd_block_header_ptr)val; 
		}

		block->fd = 0;
		block->freeness = SSD_BLOCK_FREE;
		block->cleanliness = SSD_BLOCK_CLEAN;

		iter = iter + offset;
	}


}

void
ssd_superblock_flush(ssd_superblock_header_ptr superblock);
{
	uint8_t *iter = (uint8_t*)(superblock + 1);
	uint32_t offset = sizeof(ssd_block_header_t) + bsize;
	uint32_t max_offset = SSD_SUPERBLOCK_CAPACITY - offset;

	for (uint32_t k = 0; k <= max_offset; k += offset) {
		
		ssd_block_header_ptr block = (ssd_block_header_ptr)(iter + k);
		
		if (block->freeness == SSD_BLOCK_FREE || block->cleanliness == SSD_BLOCK_CLEAN)
			continue;
		
		void *data = (void*)(block + 1);
		write(block->fd, data, bsize);
	}
}
