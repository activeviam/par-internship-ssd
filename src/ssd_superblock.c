#include <ssd_env.h>
#include <ssd_superblock.h>
#include <ssd_chunk.h>
#include <unistd.h>
#include <stdio.h>
#include <errno.h>
#include <string.h>

int
ssd_superblock_reinit(ssd_superblock_header_ptr superblock, uint32_t bsize)
{
	if (bsize & 0x7) {
		fprintf(stderr, "bsize not aligned\n");
		return -1;
	}
	
	uint8_t *iter = (uint8_t*)(superblock + 1);

	uint32_t offset, max_offset;
	
	if (superblock->bsize != 0) {

		offset = sizeof(ssd_block_header_t) + superblock->bsize;
		max_offset = SSD_SUPERBLOCK_CAPACITY - offset;

		ssd_rwlock_wrlock(&superblock->rwlock);

		for (uint32_t k = 0; k <= max_offset; k += offset) {

			ssd_block_header_ptr block = (ssd_block_header_ptr)(iter + k);	

			if (block->owner) {

				if ((uint64_t)(block->owner->data) & 0x1) {
					
					ssize_t bytes_written = write(block->owner->fd, (void*)SSD_PTR_MASK(block->owner->data), superblock->bsize);
					
					if (bytes_written != superblock->bsize) {
						fprintf(stderr, "error in file write: %s\n", strerror(errno));
						ssd_rwlock_wrunlock(&superblock->rwlock);
						return -1;
					}

				}

				block->owner->data = 0;
				block->owner = 0;
			}

		}

		superblock->bsize |= 0x1U;

		ssd_rwlock_wrunlock(&superblock->rwlock);
	}
	
	ssd_rwlock_wrlock(&superblock->rwlock);

	superblock->bsize = bsize;
	
	offset = sizeof(ssd_block_header_t) + superblock->bsize;
	max_offset = SSD_SUPERBLOCK_CAPACITY - offset;

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

	return 0;
}
