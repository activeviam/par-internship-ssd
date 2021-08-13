#ifndef __SSD_ALLOCATOR_H__
#define __SSD_ALLOCATOR_H__

#include <stdint.h>
#include <fcntl.h>

struct ssd_storage {
	off_t	capacity;
	off_t	offset;
	int32_t	fd;
};

int
ssd_storage_init(struct ssd_storage *storage, const char *filename, const uint64_t capacity);

void
ssd_storage_exit(struct ssd_storage *storage);

off_t
ssd_storage_allocate(struct ssd_storage *storage, const uint64_t capacity);

void
ssd_storage_free(struct ssd_storage *storage, const off_t offset, const uint64_t capacity);

#endif
