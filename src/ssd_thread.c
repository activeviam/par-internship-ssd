#include <ssd_thread.h>

static pthread_t threads[SSD_MAX_NUM_THREADS];
static uint8_t nb_threads = 0;

uint8_t
ssd_thread_get_trid()
{
	pthread_t current_thread = pthread_self();
	uint8_t trid;

	for (trid = 0; trid < SSD_MAX_NUM_THREADS; trid++) {
		if (pthread_equal(threads[trid], current_thread))
			break;
	}

	return trid;
}

void
ssd_thread_add_trid()
{
	pthread_t current_thread = pthread_self();

	if (nb_threads < SSD_MAX_NUM_THREADS) {
		threads[nb_threads] = current_thread;
	}

	nb_threads++;
}

uint8_t
ssd_thread_get_num()
{
	return nb_threads;
}

pthread_t
ssd_thread_pthread_wrapper(uint8_t trid)
{
	return threads[trid];	
}

typedef void* (*ssd_thread_worker_t)(void);

void*
ssd_thread_register_workload(void *arg)
{
	ssd_thread_add_trid();
	ssd_thread_worker_t worker = (ssd_thread_worker_t)arg;
	return worker();
}

int
ssd_rwlock_init(ssd_rwlock_t *rwlock)
{
	pthread_spin_init(&rwlock->reader, PTHREAD_PROCESS_PRIVATE);
	pthread_spin_init(&rwlock->writer, PTHREAD_PROCESS_PRIVATE);
	rwlock->count = 0;
	return 0;
}

void
ssd_rwlock_destroy(ssd_rwlock_t *rwlock)
{
	pthread_spin_destroy(&rwlock->reader);
	pthread_spin_destroy(&rwlock->writer);
	rwlock->count = 0;
}

void
ssd_rwlock_rdlock(ssd_rwlock_t *rwlock)
{
	pthread_spin_lock(&rwlock->reader);
	if (rwlock->count == 0) {
		pthread_spin_lock(&rwlock->writer);
	}
	rwlock->count++;
	pthread_spin_unlock(&rwlock->reader);
}

int
ssd_rwlock_tryrdlock(ssd_rwlock_t *rwlock)
{
	int val;

	pthread_spin_lock(&rwlock->reader);
	if (rwlock->count > 0 || ssd_rwlock_trywrlock(rwlock)) {	
		rwlock->count++;
		val = 1;
	} else {
		val = 0;
	}
	pthread_spin_unlock(&rwlock->reader);
	
	return val;
}

void
ssd_rwlock_rdunlock(ssd_rwlock_t *rwlock)
{
	pthread_spin_lock(&rwlock->reader);
	rwlock->count--;
	if (rwlock->count == 0) {
		pthread_spin_unlock(&rwlock->writer);
	}
	pthread_spin_unlock(&rwlock->reader);
}

void
ssd_rwlock_wrlock(ssd_rwlock_t *rwlock)
{
	pthread_spin_lock(&rwlock->writer);
}

int
ssd_rwlock_trywrlock(ssd_rwlock_t *rwlock)
{
	return pthread_spin_trylock(&rwlock->writer) == 0;
}

void
ssd_rwlock_wrunlock(ssd_rwlock_t *rwlock)
{
	pthread_spin_unlock(&rwlock->writer);
}
