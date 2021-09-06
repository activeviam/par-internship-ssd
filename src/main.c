#include <ssd_virtmem.h>
#include <ssd_superblock.h>
#include <ssd_superblock_hmap.h>
#include <ssd_heap.h>
#include <ssd_chunk.h>
#include <ssd_thread.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <sys/resource.h>
#include <threadsafe_queue.h>

#include <../tests/tests.h>

void
configure_num_files()
{
	struct rlimit rlimit;
	getrlimit(RLIMIT_NOFILE, &rlimit);
	rlimit.rlim_cur = rlimit.rlim_max;
	setrlimit(RLIMIT_NOFILE, &rlimit);		
}

int
main()
{
	configure_num_files();

	printf("sizeof(ssd_superblock_header_t) = %lu\n", sizeof(ssd_superblock_header_t));
	printf("sizeof(ssd_block_header_t) = %lu\n", sizeof(ssd_block_header_t));
	printf("sizeof(ssd_chunk_t) = %lu\n", sizeof(ssd_chunk_t));
	
	test_memcpy();

	return 0;
}
