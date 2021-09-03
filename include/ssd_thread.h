#ifndef __SSD_THREAD_H__
#define __SSD_THREAD_H__

#include <pthread.h>
#include <stdint.h>

#define SSD_MAX_NUM_THREADS 8

uint8_t
ssd_thread_get_trid();

void
ssd_thread_add_trid();

uint8_t
ssd_thread_get_num();

pthread_t
ssd_thread_pthread_wrapper(uint8_t trid);

void*
ssd_thread_register_workload(void *work);

typedef struct ssd_rwlock_s {
	pthread_spinlock_t	reader;
	pthread_spinlock_t	writer;
	uint32_t			count;
} ssd_rwlock_t;

int
ssd_rwlock_init(ssd_rwlock_t *rwlock);

void
ssd_rwlock_destroy(ssd_rwlock_t *rwlock);

void
ssd_rwlock_rdlock(ssd_rwlock_t *rwlock);

int
ssd_rwlock_tryrdlock(ssd_rwlock_t *rwlock);

void
ssd_rwlock_rdunlock(ssd_rwlock_t *rwlock);

void
ssd_rwlock_wrlock(ssd_rwlock_t *rwlock);

int
ssd_rwlock_trywrlock(ssd_rwlock_t *rwlock);

void
ssd_rwlock_wrunlock(ssd_rwlock_t *rwlock);

#endif
