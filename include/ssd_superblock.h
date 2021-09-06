#ifndef __SSD_SUPERBLOCK_H__
#define __SSD_SUPERBLOCK_H__

#include <ssd_thread.h>
#include <ssd_concurrent_stack.h>
#include <pthread.h>
#include <sys/queue.h>

#define SSD_PTR_MASK(ptr) ((uint64_t)(ptr) & 0xFFFFFFFFFFFFFFF8)
#define SSD_CHUNK_DATA_DIRTY(ptr) ((void*)((uint64_t)(ptr) | 0x1))

struct ssd_chunk_s;

typedef struct ssd_block_header_s	ssd_block_header_t;
typedef ssd_block_header_t* 		ssd_block_header_ptr;

struct ssd_block_header_s {
	ssd_block_header_ptr	next;
	struct ssd_chunk_s		*owner;
};

#define SSD_SUPERBLOCK_CAPACITY (1U << 21)

#define SSD_SUPERBLOCK_STATUS_EMPTY		0U
#define SSD_SUPERBLOCK_STATUS_BUSY		1U
#define SSD_SUPERBLOCK STATUS_FLUSHED	2U

typedef struct ssd_superblock_header_s	ssd_superblock_header_t;
typedef ssd_superblock_header_t*		ssd_superblock_header_ptr;

struct ssd_superblock_header_s {
	ssd_superblock_header_ptr				next;
	ssd_concurrent_stack_t					stack;
	ssd_rwlock_t							rwlock;
	uint32_t								bsize;
	float									usage;
	ssd_superblock_header_ptr 				link;
	TAILQ_ENTRY(ssd_superblock_header_s)	tailq;
};

int
ssd_superblock_reinit(ssd_superblock_header_ptr superblock, uint32_t bsize);

#endif
