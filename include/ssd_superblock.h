#ifndef __SSD_SUPERBLOCK_H__
#define __SSD_SUPERBLOCK_H__

#include <ssd_concurrent_stack.h>
#include <pthread.h>
#include <sys/queue.h>

#define SSD_BLOCK_CLEAN 0
#define SSD_BLOCK_DIRTY 1

#define SSD_BLOCK_FREE 0
#define SSD_BLOCK_USED 1

typedef struct ssd_block_header_s	ssd_block_header_t;
typedef ssd_block_header_t* 		ssd_block_header_ptr;
struct ssd_block_header_s {

	ssd_block_header_ptr	next;

	int32_t					fd;
	int16_t					freeness;
	int16_t					cleanliness;
};

typedef struct ssd_superblock_header_s	ssd_superblock_header_t;
typedef ssd_superblock_header_t*		ssd_superblock_header_ptr;
struct ssd_superblock_header_s {
	
	ssd_superblock_header_ptr		next;
	
	pthread_rwlock_t				rwlock;
	concurrent_stack_t				stack;

	ssd_superblock_header_ptr 		link;
	TAILQ_ENTRY(ssd_superblock_s)	tailq;
	
	uint32_t						bsize;
};

void
ssd_superblock_init(ssd_superblock_header_ptr superblock, uint32_t bsize);

void
ssd_superblock_flush(ssd_superblock_header_ptr superblock);

#endif
