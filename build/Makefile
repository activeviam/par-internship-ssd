HDR_DIR = ../include

DEP_DIR = 	$(HDR_DIR) \
			$(BOOST_DIR)

SRC_DIR =	../src

LIBS = -luring

_HDRS =	ssd_chunk.h \
		ssd_cache.h \
		ssd_allocator.h

HDRS = $(patsubst %,$(HDR_DIR)/%,$(_HDRS))

OBJS =	main.o \
		ssd_chunk.o \
		ssd_allocator.o

BUILD_CMD = $(strip $(CFLAGS))

ifeq ($(BUILD_CMD),-DCUSTOM_LOCKFREE)
	OBJS += ssd_cache_custom_lockfree.o
else ifeq ($(BUILD_CMD),-DCUSTOM_SPINLOCK)
	OBJS += ssd_cache_custom_spinlock.o
else ifeq ($(BUILD_CMD),-DBOOST)
	OBJS += ssd_cache_boost.o
endif

DEP_CMD = $(patsubst %,-I%,$(DEP_DIR))

LFS = -D _LARGEFILE64_SOURCE -D _FILE_OFFSET_BITS=64

#override CFLAGS += $(LFS) -g -D DEBUG -Wall -Wextra -pedantic -fsanitize=address
override CFLAGS += $(LFS) -O3 -fomit-frame-pointer

COMPILER = $(CXX)

app: $(OBJS) 
	$(COMPILER) -o $@ $^ $(LIBS) $(CFLAGS)

%.o: $(SRC_DIR)/%.c $(HDRS)
	$(COMPILER) -c -o $@ $< $(DEP_CMD) $(LIBS) $(CFLAGS)

.PHONY: info, clean

info:
	@echo CFLAGS:  $(CFLAGS)
	@echo BOOST_DIR: $(BOOST_DIR)

clean:
	rm -f app *.o
