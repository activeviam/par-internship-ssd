#ifndef __SPDK_CONTEXT_H__

#include <spdk/stdinc.h>
#include <spdk/nvme.h>
#include <spdk/env.h>

struct ns_entry {
	TAILQ_ENTRY(ns_entry) 	link;
	struct spdk_nvme_ns		*ns;
	uint32_t				lb_capacity;
	uint32_t				lb_order;
	uint32_t				lb_sectors;
	uint32_t				lbs_occupied;
};

struct ctrlr_entry {
	struct spdk_nvme_ctrlr 	*ctrlr;
	struct spdk_env_opts 	*opts;
	TAILQ_HEAD(, ns_entry)	ns;
};

struct ctrlr_entry*
ctrlr_entry_init(struct spdk_env_opts *opts, uint32_t lb_order);

void
ctrlr_entry_free(struct ctrlr_entry *entry);

#endif
