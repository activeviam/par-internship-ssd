#include <ssd_heap.h>
#include <string.h>
#include <stdlib.h>

#define NUM_HASH_PRIMES 30
static const uint64_t primes[NUM_HASH_PRIMES] =
{
  0ul,          3ul,          11ul,         23ul,         53ul,
  97ul,         193ul,        389ul,        769ul,        1543ul,
  3079ul,       6151ul,       12289ul,      24593ul,      49157ul,
  98317ul,      196613ul,     393241ul,     786433ul,     1572869ul,
  3145739ul,    6291469ul,    12582917ul,   25165843ul,   50331653ul,
  100663319ul,  201326611ul,  402653189ul,  805306457ul,  1610612741ul
};

#define MAX_LOAD_FACTOR 10

int
ssd_superblock_hmap_init(ssd_superblock_hmap_t *hmap)
{
	hmap->buckets = NULL;
	hmap->size = 0;
	hmap->order = 0;
	return 0;
}

static void
hmap_rehash(ssd_superblock_hmap_t *hmap);

void
ssd_superblock_hmap_insert(ssd_superblock_hmap_t *hmap, ssd_superblock_header_ptr superblock)
{
	uint32_t capacity = primes[hmap->order];

	if (++hmap->size > MAX_LOAD_FACTOR * capacity) {
		hmap_rehash(hmap);
		capacity = primes[hmap->order];
	}

	uint32_t index = superblock->bsize % capacity;

	superblock->link = hmap->buckets[index];
	hmap->buckets[index] = superblock;
}

ssd_superblock_header_ptr *
ssd_superblock_hmap_find(ssd_superblock_hmap_t *hmap, ssd_superblock_header_ptr superblock)
{
	if (hmap->order == 0)
		return NULL;

	uint32_t capacity = primes[hmap->order];

	uint32_t index = superblock->bsize % capacity;
	
	ssd_superblock_header_ptr *it = hmap->buckets + index;

	while ((*it)->link) {
		if (*it == superblock) {
			break;
		}
		it = &((*it)->link);
	}

	return it;
}

void
ssd_superblock_hmap_suggest(ssd_superblock_hmap_t 		*hmap,
						 	uint32_t					bsize,
						 	ssd_superblock_header_ptr	**it_ptr,
						 	ssd_block_header_ptr		*block_ptr)
{
	if (hmap->order == 0)
		return;

	uint32_t capacity = primes[hmap->order];

	uint32_t index = bsize % capacity;
		
	ssd_superblock_header_ptr *it = hmap->buckets + index;
	
	while (*it) {

		ssd_superblock_header_ptr superblock = *it;
		
		if (superblock->bsize == bsize) {

			*block_ptr = (ssd_block_header_ptr)ssd_concurrent_stack_pop(&superblock->stack);
			if (*block_ptr) {
				*superblock_it = it;
				return;
			}
		}

		it = &(*it)->link;
	}

	*it_ptr = NULL;
}

ssd_superblock_header_ptr
ssd_superblock_hmap_remove(ssd_superblock_hmap_t *hmap, ssd_superblock_header_ptr *it)
{
	ssd_superblock_header_ptr superblock = *it;
	*it = superblock->link;
	superblock->link = NULL;
	return superblock; 
}

static void
hmap_rehash(ssd_superblock_hmap_t *hmap)
{	
	uint32_t old_capacity = primes[hmap->order];
	
	if (++hmap->order >= NUM_HASH_PRIMES)
		return;
	
	uint32_t new_capacity = primes[hmap->order];

	hmap->buckets = realloc(hmap->buckets, sizeof(void*) * new_capacity);
	
	uint32_t diff = new_capacity - old_capacity;

	memset(hmap->buckets + old_capacity, 0, sizeof(void*) * diff);

	for (uint32_t i = 0; i < old_capacity; i++) {
		ssd_superblock_header_ptr *it = hmap->buckets + i;
		while (*it) {
			ssd_superblock_hmap_insert(hmap, ssd_superblock_hmap_remove(hmap, it));
		}
	}
}

void
ssd_superblock_hmap_free(ssd_superblock_hmap_t *hmap)
{
	free(hmap->buckets);
	hmap->size = 0;
	hmap->order = 0;
}
