#include <ssd_allocator.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>

int
ssd_storage_init(struct ssd_storage *storage, const char *filename, const uint64_t capacity)
{
	if (!storage) {
		fprintf(stderr, "passed null ssd_storage\n");
		return -1;
	}
	
	int32_t fd = open(filename, O_CREAT|O_TRUNC|O_RDWR, S_IRUSR|S_IWUSR|S_IRGRP|S_IWGRP);
	if (fd < 0) {
		fprintf(stderr, "cannot open file %s\n", filename);
		return -1;
	}
	storage->fd = fd;
	
	/* Initialize a chunk */
	
	int rc = fallocate(fd, 0, 0, capacity);
	if (rc < 0) {
		fprintf(stderr, "cannot allocate needed %lu bytes on ssd: %s\n", capacity, strerror(-rc));
		return -1;
	}
	storage->capacity = capacity;
	
	storage->offset = 0;

	return 0;
}

void
ssd_storage_exit(struct ssd_storage *storage)
{
	if (storage) {
		int rc = ftruncate(storage->fd, 0);
		if (rc < 0) {
			perror("ssd_storage_exit");
			return;
		}
		storage->capacity = storage->offset = 0;
	}
}

off_t
ssd_storage_allocate(struct ssd_storage *storage, const uint64_t capacity)
{
	if (!storage) {
		return -1;
	}

	if (storage->capacity < capacity) {
		return -1;
	}

	if (storage->offset >= storage->capacity - capacity) {
		return -1;
	}

	off_t backup = storage->offset;
	storage->offset += capacity;
	return backup;
}

void
ssd_storage_free(struct ssd_storage *storage, const off_t offset, const uint64_t capacity)
{
	/* TODO: smart allocation procedure */
}
