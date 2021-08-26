#include <ssd_heap.h>

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

#define MAX_LOAD_FACTOR 5

static void
hmap_rehash(ssd_superblock_hmap_t *hmap);

static ssd_superblock_header_ptr
hmap_insert(ssd_superblock_hmap_t *hmap, ssd_superblock_header_ptr block)
{
	uint32_t capacity = primes[hmap->order];
	uint32_t index = block->bsize % capacity;

	block->next = hmap->buckets[index];
	hmap->buckets[index] = block;
	
	hmap->size++;
	if (hmap->size > MAX_LOAD_FACTOR * capacity) {
		hmap_rearrange(hmap);
	}

	return hmap->buckets[index];
}

static ssd_superblock_header_ptr*
hmap_find(ssd_superblock_hmap_t *hmap, uint32_t bsize)
{
	uint32_t capacity = primes[hmap->order];
	uint32_t index = bsize % hmap->capacity;

	ssd_superblock_header_ptr *curr = hmap->buckets + index;

	while (*curr) {
		ssd_superblock_header_ptr block = *curr;
		if (block->bsize == bsize) {
			break;
		}
		curr = &block->next;
	}

	return curr;
}

static ssd_superblock_header_ptr
hmap_remove(ssd_superblock_hmap_t *hmap, ssd_superblock_header_ptr *curr)
{
	ssd_superblock_header_ptr block = *curr;
	*curr = block->next;
	block->next = NULL;
	return block; 
}

static void
hmap_rehash(ssd_superblock_hmap_t *hmap)
{	
	uint32_t old_capacity = primes[hmap->order++];
	
	if (hmap->order >= NUM_HASH_PRIMES)
		return;
	
	uint32_t new_capacity = primes[hmap->order];

	hmap->buckets = realloc(hmap->buckets, sizeof(void*) * new_capacity);
	
	uint32_t diff = new_capacity - old_capacity;

	memset(hmap->buckets + old_capacity, 0, sizeof(void*) * diff);

	for (uint32_t i = 0; i < old_capacity; i++) {
		ssd_superblock_header_ptr *curr = hmap->buckets + i;
		while (*curr) {
			hmap_insert(hmap, hmap_remove(hmap, curr));
		}
	}

	return 0;
}
