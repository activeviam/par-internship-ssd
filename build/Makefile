HDR_DIR = ../include

DEP_DIR = 	$(HDR_DIR) \
			$(SPDK_DIR)/include \
			$(BOOST_DIR)

SRC_DIR =	../src

LIB_DIR = 	$(SPDK_DIR)/build/lib \
			$(SPDK_DIR)/dpdk/build/lib \
			$(SPDK_DIR)/isa-l/.libs

WHOLE_ARCHIVE = -lspdk_nvme \
		-lspdk_sock \
		-lspdk_rpc \
		-lspdk_jsonrpc \
		-lspdk_json \
		-lspdk_sock_posix \
		-lspdk_util \
		-lspdk_env_dpdk \
		-lspdk_log \
		-lrte_eal \
		-lrte_mempool \
		-lrte_ring \
		-lrte_mbuf \
		-lrte_bus_pci \
		-lrte_pci \
		-lrte_mempool_ring \
		-lrte_power \
		-lrte_ethdev \
		-lrte_net \
		-lrte_telemetry \
		-lrte_kvargs \
		-lrte_vhost \
		-lrte_cryptodev \
		-lrte_hash \
		-lrte_rcu \
		-lisal

NOWHOLE_ARCHIVE = -pthread \
		  -lrt \
		  -luuid \
		  -lcrypto \
		  -lm \
		  -laio \
		  -lnuma \
		  -ldl

_HDRS =	ssd_chunk.h \
		ssd_cache.h \
		spdk_context.h

HDRS = $(patsubst %,$(HDR_DIR)/%,$(_HDRS))

OBJS =	main.o \
		ssd_chunk.o \
		spdk_context.o

BUILD_CMD = $(strip $(CFLAGS))

ifeq ($(BUILD_CMD),-DCUSTOM_LOCKFREE)
	OBJS += ssd_cache_custom_lockfree.o
else ifeq ($(BUILD_CMD),-DCUSTOM_SPINLOCK)
	OBJS += ssd_cache_custom_spinlock.o
else ifeq ($(BUILD_CMD),-DBOOST)
	OBJS += ssd_cache_boost.o
endif

DEP_CMD = $(patsubst %,-I%,$(DEP_DIR))
LIB_CMD = 	-Wl,--whole-archive $(WHOLE_ARCHIVE) \
			-Wl,--no-whole-archive $(NOWHOLE_ARCHIVE) \
			$(patsubst %,-L%,$(LIB_DIR))

override CFLAGS += -DDEBUG -Wall -Wextra -pedantic -fsanitize=address
#override CFLAGS += -O3 -fomit-frame-pointer

COMPILER = $(CXX)

app: $(OBJS) 
	$(COMPILER) -o $@ $^ $(LIB_CMD) $(CFLAGS)

%.o: $(SRC_DIR)/%.c $(HDRS)
	$(COMPILER) -c -o $@ $< $(DEP_CMD) $(LIB_CMD) $(CFLAGS)

.PHONY: info, clean

info:
	@echo CFLAGS:  $(CFLAGS)
	@echo SPDK_DIR: $(SPDK_DIR)
	@echo BOOST_DIR: $(BOOST_DIR)

clean:
	rm -f app *.o
