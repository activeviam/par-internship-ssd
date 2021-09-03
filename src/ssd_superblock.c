#include <ssd_env.h>
#include <ssd_superblock.h>
#include <ssd_chunk.h>
#include <unistd.h>
#include <stdio.h>

void
ssd_superblock_init(ssd_superblock_header_ptr superblock, uint32_t bsize)
{
	ssd_rwlock_wrlock(&superblock->rwlock);

	superblock->used = 1;
	superblock->bsize = bsize;

	uint8_t* iter = (uint8_t*)(superblock + 1);
	uint32_t offset = sizeof(ssd_block_header_t) + bsize;
	uint16_t stack_size = SSD_SUPERBLOCK_CAPACITY / offset; 
	uint64_t val = (uint64_t)(iter) + ((uint64_t)stack_size << ADDRESS_SPACE_ORDER);
	superblock->stack.head = (void*)val;

	while (stack_size > 0) {
		
		ssd_block_header_ptr block = (ssd_block_header_ptr)iter;
	
		stack_size--;
		if (stack_size == 0) {
			block->next = 0;
		} else {
			uint64_t val = (uint64_t)((uint8_t*)block + offset);
			val |= ((uint64_t)stack_size << ADDRESS_SPACE_ORDER);
			block->next = (ssd_block_header_ptr)val; 
		}

		block->owner = 0;
	
		iter = iter + offset;
	}

	ssd_rwlock_wrunlock(&superblock->rwlock);
}

void
ssd_superblock_flush(ssd_superblock_header_ptr superblock)
{
	ssd_rwlock_wrlock(&superblock->rwlock);

	uint8_t *iter = (uint8_t*)(superblock + 1);
	uint32_t offset = sizeof(ssd_block_header_t) + superblock->bsize;
	uint32_t max_offset = SSD_SUPERBLOCK_CAPACITY - offset;

	for (uint32_t k = 0; k <= max_offset; k += offset) {
		
		ssd_block_header_ptr block = (ssd_block_header_ptr)(iter + k);
		
		if (!block->owner) {
			continue;
		}

		if ((uint64_t)(block->owner->data) & 0x1) {
			write(block->owner->fd, (void*)SSD_PTR_MASK(block->owner->data), superblock->bsize);
		}

		block->owner->data = 0;
		block->owner = 0;
	}

	superblock->used = 0;
	ssd_rwlock_wrunlock(&superblock->rwlock);
}
