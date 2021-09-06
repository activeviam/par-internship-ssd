HDR_DIR = ../include

_HDRS =	ssd_env.h \
		ssd_thread.h \
		ssd_concurent_stack.h \
		ssd_virtmem.h \
		ssd_superblock.h \
		ssd_superblock_hmap.h \
		ssd_heap.h \
		ssd_chunk.h \

HDRS = $(patsubst %,$(HDR_DIR)/%,$(_HDRS))

SRC_DIR = ../src

OBJS =	main.o \
		ssd_thread.o \
		ssd_concurrent_stack.o \
		ssd_virtmem.o \
		ssd_superblock.o \
		ssd_superblock_hmap.o \
		ssd_heap.o \
		ssd_chunk.o \

TEST_DIR = ../tests

TESTS = test_flush.o \
		test_malloc.o \
		test_arbitration.o \
		test_io_queue.o \
		test_memcpy.o

LIBS = -lpthread

INCLUDE = $(patsubst %,-I%,$(HDR_DIR))
LFS = -D _LARGEFILE64_SOURCE -D _FILE_OFFSET_BITS=64

CFLAGS = $(INCLUDE) $(LFS) -g -D DEBUG -Wall -Wextra -pedantic -fsanitize=address
#CFLAGS = $(INCLUDE) $(LFS) -O3 -fomit-frame-pointer

app: $(OBJS) $(TESTS)
	$(CC) -o $@ $^ $(LIBS) $(CFLAGS)

test_%.o: $(TEST_DIR)/test_%.c
	$(CC) -c -o $@ $< $(LIBS) $(CFLAGS)

%.o: $(SRC_DIR)/%.c
	$(CC) -c -o $@ $< $(LIBS) $(CFLAGS)

.PHONY: info, clean

info:
	@echo CFLAGS:			$(CFLAGS)
	@echo HEADER DIRECTORY: $(HDR_DIR)
	@echo SOURCE DIRECTORY: $(SRC_DIR)
	@echo TEST DIRECTORY:	$(TEST_DIR)

clean:
	rm -f app *.o
