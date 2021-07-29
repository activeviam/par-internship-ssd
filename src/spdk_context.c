#include <spdk_context.h>

static bool
probe_cb(void *cb_ctx, const struct spdk_nvme_transport_id *trid,
	struct spdk_nvme_ctrlr_opts *opts)
{
	printf("attaching to %s\n", trid->traddr);
	return true;
}

static void
attach_cb(void *cb_ctx, const struct spdk_nvme_transport_id *trid,
	struct spdk_nvme_ctrlr *ctrlr, const struct spdk_nvme_ctrlr_opts *opts)
{
	struct spdk_nvme_ctrlr **target = (struct spdk_nvme_ctrlr**)cb_ctx;
	*target = ctrlr;
	printf("attached to %s\n", trid->traddr);
}

static int
register_controller(struct spdk_nvme_ctrlr** ctrlr)
{
	int rc = spdk_nvme_probe(NULL, ctrlr, probe_cb, attach_cb, NULL);
	
	if (rc != 0 || *ctrlr == NULL) {
		fprintf(stderr, "spdk_nvme_probe() failed\n");
		return -1;
	}

	return 0;
}

static int
register_namespaces(struct ctrlr_entry *ctrlr_entry)
{
	int ns_id, num_ns;
	struct spdk_nvme_ns *ns;
	struct ns_entry *ns_entry;

	/*
	 * Each controller has one or more namespaces.  An NVMe namespace is basically
	 *  equivalent to a SCSI LUN.  The controller's IDENTIFY data tells us how
	 *  many namespaces exist on the controller.  For Intel(R) P3X00 controllers,
	 *  it will just be one namespace.
	 *
	 * Note that in NVMe, namespace IDs start at 1, not 0.
	 */

	struct spdk_nvme_ctrlr *ctrlr = ctrlr_entry->ctrlr;

	if (!ctrlr) {
		fprintf(stderr, "trying to register namespaces of invalid controller\n");
		return -1;
	}

	num_ns = spdk_nvme_ctrlr_get_num_ns(ctrlr);
	
	TAILQ_INIT(&ctrlr_entry->ns);

	for (ns_id = 1; ns_id <= num_ns; ns_id++) {
		
		ns = spdk_nvme_ctrlr_get_ns(ctrlr, ns_id);
			
		if (ns == NULL || !spdk_nvme_ns_is_active(ns)) {
			continue;
		}

		ns_entry = (struct ns_entry*)malloc(sizeof(struct ns_entry));
	
		if (ns_entry == NULL) {
			fprintf(stderr, "malloc() failed to allocate new namespace entry\n");
			return -1;
		}

		ns_entry->ns = ns;

		size_t ns_size = spdk_nvme_ns_get_size(ns);
		size_t lb_size = spdk_nvme_ns_get_sector_size(ns);

		for (ns_entry->lb_order = 0; lb_size > 1; lb_size = lb_size >> 1) {
			ns_entry->lb_order++;
		}

		ns_entry->lb_capacity = ns_size >> ns_entry->lb_order;
		ns_entry->lbs_occupied = 0;

		TAILQ_INSERT_TAIL(&ctrlr_entry->ns, ns_entry, link);
		printf("  namespace id: %d size: %u blocks\n", spdk_nvme_ns_get_id(ns_entry->ns), ns_entry->lb_capacity);
	}

	if (TAILQ_EMPTY(&ctrlr_entry->ns)) {
		fprintf(stderr, "namespaces are not found\n");
		return -1;
	}

	return 0;
}

void
ctrlr_entry_free(struct ctrlr_entry *ctrlr_entry)
{
	struct ns_entry *ns_entry, *tmp_ns_entry;
	TAILQ_FOREACH_SAFE(ns_entry, &ctrlr_entry->ns, link, tmp_ns_entry) {
		TAILQ_REMOVE(&ctrlr_entry->ns, ns_entry, link);
		free(ns_entry);
	}

	if (ctrlr_entry->ctrlr) {
		spdk_nvme_detach(ctrlr_entry->ctrlr);
	}

	free(ctrlr_entry);
}

struct ctrlr_entry*
ctrlr_entry_init(struct spdk_env_opts *opts) {
	
	int rc;
	
	struct ctrlr_entry *ctrlr_entry = (struct ctrlr_entry*)malloc(sizeof(struct ctrlr_entry));

	if (!ctrlr_entry) {
		fprintf(stderr, "malloc() failed when trying to create ssd ctrlr\n");
		return NULL;
	}

	ctrlr_entry->opts = opts;

	rc = register_controller(&ctrlr_entry->ctrlr);
	if (rc != 0) {
		free(ctrlr_entry);
		return NULL;
	}

	rc = register_namespaces(ctrlr_entry);
	if (rc != 0) {
		ctrlr_entry_free(ctrlr_entry);
		return NULL;
	}

	return ctrlr_entry;
}
