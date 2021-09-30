/*
 * (C) ActiveViam 2020
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use
 * reproduction or transfer of this material is strictly prohibited
 */

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.activeviam.MemoryAllocator;
import com.activeviam.UnsafeUtil;
import com.activeviam.reference.SuperblockManager;
import com.activeviam.reference.SuperblockMemoryAllocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.activeviam.MemoryAllocator.PAGE_SIZE;
import static com.activeviam.reference.IBlockAllocator.NULL_POINTER;
import static org.assertj.core.api.Assertions.assertThat;

public class TestMemoryAllocatorOnFile {

  @Test
  void testAllocatingBlock(@TempDir Path tempDir) {
    System.out.println("Running test with temp dir " + tempDir);
    final var allocator = new SuperblockMemoryAllocator(tempDir, 1 << 21, 1 << 30, false);
    final var size1 = 2 * PAGE_SIZE;
    final var alloc1 = allocator.allocateMemory(size1);
    final var size2 = PAGE_SIZE;
    final var alloc2 = allocator.allocateMemory(size2);
    allocator.freeMemory(alloc1);
    allocator.freeMemory(alloc2);
  }

  private final SuperblockManager vmem = new SuperblockManager(1 << 21, 1 << 21, false);
  private final ConcurrentLinkedDeque<MemoryAllocator.ReturnValue> deque = new ConcurrentLinkedDeque();
  private final ReadWriteLock rwlock = new ReentrantReadWriteLock();

  @Test
  void testVmemStorage(@TempDir Path tempDir) {

    final int numThreads = 2;
    final List<Thread> threads = new LinkedList<>();

    vmem.init();

    for (int threadId = 0; threadId < numThreads; threadId++) {

      final int id = threadId + 1;

      final Thread thread = new Thread(() -> {

        MemoryAllocator.ReturnValue value = null;

        for (int i = 0; i < 1; i++) {

          boolean stop = false;
          while (!stop) {
            if (rwlock.readLock().tryLock()) {
              if (value != null && value.isActiveBlock()) {
                UnsafeUtil.putInt(value.getBlockAddress() + 4 * i, id);
                rwlock.readLock().unlock();
                stop = true;
                continue;
              }
            }
            value = getPtr();
          }
        }

        for (int i = 0; i < 1; i++) {

          int actual = Integer.MAX_VALUE;

          boolean stop = false;
          while (!stop) {
            if (rwlock.readLock().tryLock()) {
              if (value != null && value.isActiveBlock()) {
                actual = UnsafeUtil.getInt(value.getBlockAddress() + 4 * i);
                rwlock.readLock().unlock();
                stop = true;
                continue;
              }
            }
            value = getPtr();
          }

          assertThat(actual).isEqualTo(id);
        }

      }, "thread " + id);

      threads.add(thread);
      thread.start();
    }

    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    this.vmem.tryRelease();
  }

  private final ReadWriteLock gcRwlock = new ReentrantReadWriteLock();

  private MemoryAllocator.ReturnValue getPtr() {

    MemoryAllocator.ReturnValue value = null;

    do {
      gcRwlock.readLock().lock();
      value = vmem.allocate();
      gcRwlock.readLock().unlock();
      if (value == null) {
       gc();
      }
    } while (value == null);

    this.deque.add(value);
    return value;
  }

  private void gc() {
    if (gcRwlock.writeLock().tryLock()) {
      final var it = deque.iterator();
      while (it.hasNext()) {
        this.rwlock.writeLock().lock();
        final var value = it.next();
        vmem.free(value);
        value.desactivateBlock();
        this.rwlock.writeLock().unlock();
        it.remove();
      }
      gcRwlock.writeLock().unlock();
    }
  }

  @Test
  void dequeRemove() {

    ConcurrentLinkedDeque<Integer> deque = new ConcurrentLinkedDeque<Integer>();

    deque.add(1);
    deque.add(2);
    deque.add(3);

    final var it = deque.iterator();
    while (it.hasNext()) {
      it.next();
      it.remove();
      for (final var elem: deque) {
        System.out.println(elem);
      }
    }



  }

}
