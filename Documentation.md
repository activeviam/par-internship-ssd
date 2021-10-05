# Memory Allocator with SSD Swap

This documentation describes a design of a special allocator that uses physical offheap memory as a cache and an SSD disk as a main storage and a backup space.

## _Memory allocators_

We define a memory allocator as a Java class for RAM allocation
providing an interface `IMemoryAllocator`:
```java
public interface IMemoryAllocator {

 class ReturnValue {
  private final Object metadata;
  private long blockAddress;
  ...
 };
 
 /** The native page size. */
 long PAGE_SIZE = UnsafeUtil.pageSize();
 /** Special value representing invalid address */
 long NULL_POINTER = 0L;
 
 ReturnValue allocateMemory(long bytes);
 void freeMemory(ReturnValue value);
}
```

The object `IMemoryAllocator.ReturnValue` has two fields: an address of
the allocated off-heap region, and some metadata object. 

The content and the purpose of the `metadata` object may vary significantly from
one implementation to another.
One may argue that it is not a good practice to keep such a mystery type field
in the interface definition.
Unfortunately, adding the metadata field in each value returned by
`allocateMemory()` seems an only way to efficiently pass metadata to the user
classes. In such a way, metadata may be assigned in the same
critical blocks of the program as the `blockAddress`.
The undue search of the internal structures of the allocator
to retrieve the metadata separately from the `blockAddress` may be thus avoided.
The further examples of metadata usage may complete this explanation.

Every block allocated by `IMemoryAllocator` implementation should be 8 byte aligned.
Thus, the least significant bits of the `blockAddress` field can be used for
useful flags. The only flag used in the current implementation indicates
the "dirtiness": it is set if the block data has been recently modified.

Any class implementing a `IMemoryAllocator` should provide functions to
allocate the RAM memory region of a given size and to free the RAM memory
region using its virtual address and metadata.

## _Block Allocators_

Block allocators differ from memory allocators in its incapacity to allocate
memory regions of different size. Instead, each block allocator may be asked 
its unique size. This type of allocators needs a special interface called
`IBlockAllocator`:
```java
public interface IBlockAllocator {
 long NULL_POINTER = IMemoryAllocator.NULL_POINTER;
 long size();
 IMemoryAllocator.ReturnValue allocate();
 void free(IMemoryAllocator.ReturnValue value);
 void release();
}
```
The purpose of the `release()` function is to destroy all the off-heap objects
possessed by the block allocator. As we may see further, block allocators in the
actual design request the system off-heap resources quite often, while memory
allocators usually do not control any off-heap resources.
In fact, any memory allocator may be efficiently implemented as a hashmap with
the keys being the sizes of memory regions and the values being the
corresponding block allocators. It is exactly how they are implemented in this
project.

### _Block Stack Allocator_

The first block allocator implementation discussed in here
is `ABlockStackAllocator`:

```java
import com.activeviam.IMemoryAllocator;

public abstract class ABlockStackAllocator implements IBlockAllocator {
 ...
 public IMemoryAllocator.ReturnValue allocate() {
  long address; // stores address of the subregion to be allocated   
  ...
  return new IMemoryAllocator.ReturnValue(this, address);
 }

 protected abstract long virtualAlloc(long size);

 protected abstract void doAllocate(long ptr, long size);

 protected abstract void doFree(long ptr, long size);

 protected abstract void doRelease(long ptr, long size);
}
```
This class is abstract, and there are several versions
of such objects in the project. It consists of a single memory region
of the fixed size divided on blocks of the same size. It is also equipped with
a concurrent stack of unique integer numbers.
Blocks in such block allocator are the memory
subregions to be returned by `allocate()`. They may be naturally enumerated.
Integer stack holds only the positions of subregions which are not yet
active (e.g. not yet occupied by application).

Each time the allocation happens, top element of the stack is popped and the
subregion address is calculated as a simple offset of huge region address.
The subregion is marked as active. The return value of `allocate()` is and address
of the allocated subregion. A `this` is passed as a metadata.

On the contrary, during a call to `free()` the position of a subregion is
calculated from its address and the address of huge region, and this value is
pushed back to the stack.

There are 4 functions in the `ABlockStackAllocator` class which must be
overloaded in the implementations:
- `virtualAlloc()` is called only once during the lifetime of the allocator
at the initialization phase.
The only purpose of this function is to allocate the huge single region of memory
to store the blocks in.
- `doAllocate()` is called directly after each block allocation inside the call to
`allocate()`.
- `doFree()` is called right before the freeing of each block inside the call 
to `free()`.
- `doRelease()` is a destructor of the off-heap objects possessed by
the block allocator. It is directly called from `release()` function, and is
conditionally called in some other contexts: for example, it may be called if the whole
storage of the block allocator is unused (freed) or if the swapping function chooses
that storage for disk swapping.

### _Virtual Memory Storage_

A kind of block stack allocator is used as a manager of the whole amount of
the virtual memory disposed by the allocator. The class is called `VirtMemStorage`.
The implementations of 4 abstract functions for this class look like this:
```java
public class VirtMemStorage extends ABlockStackAllocator {
    protected boolean lockVirtMem;
    
    @Override
    protected long virtualAlloc(long size) {
        ...
        long address = PLATFORM.mmapAnon(size, this.useHugePage);
        ...
        if (this.lockVirtMem) {
            PLATFORM.mlock(address, size);
        }
        return address;
    }
    @Override
    protected void doAllocate(long ptr, long size) {}
    @Override
    protected void doFree(long ptr, long size) {}
    @Override
    protected void doRelease(long ptr, long size) {
        if (this.lockVirtMem) {
            PLATFORM.munlock(ptr, size);
        }
        PLATFORM.munmap(ptr, size);
    }
}
```

The code for `VirtualAlloc()` consists of two important calls to OS: one that
performs anonymous mapping of RAM pages into the address space of the process,
and the other trying to lock the mapped physical pages until the storage
is released by `doRelease()`. In reality one should first ensure that the
running process has access to the needed system resources, such as virtual memory
to be mapped or to be locked, so the snippet of source code for
`virtualAlloc()` is incomplete.

The `doRelease()' function performs all the opposite operations and its code
is quite trivial.

Under Linux, the application shouldn't perform any particular operations
during the allocation or the freeing of the huge memory blocks. However, there are
some OS that require to "commit" the physical memory pages before the allocation.

Whenever the huge block of memory is needed, the duscussed swap memory allocator
tries to pop it from `VirtMemStorage`. It doesn't allocate any RAM itself.

### _Block Allocator Manager_

There is a simple and universal way to build complex block allocators with
dynamic storage size using existing block allocator implementations. It is
called a block allocator manager.

The idea is to create a concurrent resizeable data structure containing
one or several block allocators that provide blocks of the same size. On each
allocation or freeing step the search is performed in the data structure in
order to find the appropriate block allocator. On release, the block allocator
is popped from the data structure. If all the block allocators in the data structure
are full, the new allocators may be added.

There are several data structures in Java which are more or less convenient
for such a use case. In the current implementation, the `ConcurrentLinkedDeque`
has been chosen.

Let's observe how the `allocate()` method is implemented with `Deque` interface:
```java
public class BlockAllocatorManager implements IBlockAllocator {

 /** Container for all the managed allocators */
 private final Deque<ABlockStackAllocator> blocks;
 
 public IMemoryAllocator.ReturnValue allocate() {
  
  ABlockStackAllocator blockAllocator;

  do {

   /** (1) Try allocate block from the head allocator */
   if ((blockAllocator = this.blocks.peekFirst()) != null &&
           (value = blockAllocator.allocate()) != null) {
    return value;
   }

   /** (2) Probably the first allocator is full, pop it */
   blockAllocator = this.blocks.getFirst();
   if ((value = blockAllocator.allocate()) != null) { // other thread could do free
    this.blocks.addFirst(blockAllocator); // someone freed the allocator
    return value; // the allocator is worth using again in allocate()
   } else {
    this.blocks.addLast(blockAllocator); // allocator is full, postpone its usage
   }

   /** (3) Retry allocate from head again, e.g. repeat (1) */
     ...

   /** (4) Ensure this part is executed by only one thread */
   if ((blockAllocator = createBlockAllocator()) == null) {
    return null;
   } else {
    value = blockAllocator.allocate(); // get value before sharing object with other threads
    this.blocks.addFirst(blockAllocator);
   }

  } while (value == null);

  return value;
 }
 
 ...
}
```

As one may see, all threads iterate over the deque searching
for the allocator that is not yet full.
If the allocator is full, it goes to the tail of the deque. Otherwise,
it is used right away.

Only one thread at a time may create new block allocator. This thread
uses the allocator immediately before sharing it with other threads.

The `free()` method is implemented in more simple way:
```java
public void free(IMemoryAllocator.ReturnValue value) {
    final var blockAllocator = (ABlockStackAllocator)(value.getMetadata());
    blockAllocator.free(value);
    if (blockAllocator.tryRelease()) {
      this.blocks.removeFirstOccurrence(blockAllocator);
    }
}
```

This is the first observed method where the metadata of the allocated block is used.
As each block allocator returns itself, one doesn't need to search it in the deque 
on each call. However, the search still should be performed in case of total allocator
release in order to remove the dead allocator from the deque.

The different block allocators may be created using the factory object passed to
`BlockAllocatorManager`. 

## _Disk swap_

The `SwapMemoryAllocator` is a memory allocator using a hashmap "size-to-allocator".
Each allocator in the map is a `BlockAllocatorManager` with a special kind of block
stack allocator used as an elementary allocator entity. This block stack allocator is
called a `SwapBlockAllocator`. In terms of off-heap memory,
the `SwapBlockAllocator` is a kind of "superblock" containing enumerated
coalescing smaller blocks. In the organizational hierarchy of the RAM,
a "superblock" is more granular object that the whole memory storage 
controlled by `VirtMemStorage` yet less granular than elementary blocks
controlled by `ReturnValue` objects:

![](Untitled%20Diagram.png)

On the diagram, two-headed arrows between objects represent the identity, and
one-headed arrows represent the memory pointers. As one may see,
each `SwapBlockAllocator` can be viewed as an individual brick in the system
of the off-heap memory management. The `SwapBlockAllocator` never allocates RAM
itself: instead, it calls `allocate()` method of `VirtMemStorage` at the
initialization stage and corresponding `free()` method on release.

The important detail in the implementation of the `SwapBlockAllocator`
is a swap operation. It is called whenever the `SwapMemoryAllocator` decides to
release a memory superblock controlled by the `SwapBlockAllocator` and to push it
back to the `VirtMemStorage`.
However, external objects may still want to use the released
memory after the swap. That's why each external object that wants to use the memory
allocated by `SwapMemoryAllocator` should firstly register itself in the internal
memory of the `SwapBlockAllocator` used for the allocation.

In fact, directly after the swap the `SwapBlockAllocator` cannot guarantee anything 
about the validity of the memory it has already released. So it should somehow inform
the owners of its memory blocks that the memory they continue using is in
invalid state from now on. Of course, the memory doesn't just disappear:
each owner should provide its own backup space on disk from where the memory
may be further loaded back to RAM.

In the current implementation an "external object" is simply a chunk.
However, chunks carry a type information, and it is absolutely unnecessary 
to propagate this information to allocators. So one cannot simply pass
the address of the chunk to the `SwapBlockAllocator`. Instead, an object called
`ASwapChunk.Header` is used:
```java
public abstract class ASwapChunk<K> implements Chunk<K>, Closeable {
    
 public static class Header {
  private final IMemoryAllocator.ReturnValue allocatorValue;
  private final int fd;
  
  /** Trivial constructor and getters */
  ...
 }
 
 ...
}
```

An `fd` field is a file descriptor of the file opened by `ASwapChunk` object. We will
get back to the object `ASwapChunk` and its file management methods more in detail
later on. Right now it is only important to understand that the disk space manipulated
by the chunk is accessible from `swap()` method of `SwapBlockAllocator` through some
object called `Header` contained in the chunk and referenced from `SwapBlockAllocator`. 

In the current implementation, the references to the owners of the blocks are simply
stored in an array called `owners`. An index in the array corresponds to the position
of the block controlled by `Header` in the superblock. If the block has no owner, its
entry in the array is null.

It is not the best data structure to perform a swap on,
because one needs to iterate over the whole array in linear time to find all non-null
owners. However, it is a suitable enough to allocate and to free blocks, because
the modifications may be carried out in constant time simultaneously by different
threads with no synchronization at all: it is a guarantee of the allocator
that two different external objects never share the same off-heap data segment, and
it is a natural constraint for the user program to free each block only once, so
in practice two different threads should never access the same index
in the `owners` array.

The first step of the `swap()` operation is to iterate over the registered owners
(chunks) and to write each block that has an owner to its file on disk. After that
each block is freed in a sense of the block stack allocator and its owner
is unregistered.

The second step is to mark the `SwapBlockAllocator` as inactive. It is done simply
by setting a boolean flag `active` of the `SwapBlockAllocator`. We will describe later
why the flag is marked as volatile.

There are multiple ways to choose a `SwapBlockAllocator` to be released. In the 
current implementation a simple counter called `usages` is used to count the calls
to the `active()` method. Such a choice would be justified in detail in the next
section. Briefly, each external object must always call the `active()` method on its
allocator prior to any operation on the memory to avoid using garbage memory.

The function `usages()` not only returns the current value of the counter, but also
assigns a zero value to the counter afterwards. It may be not the best practice, but
the idea behind is that `usages()` function is only called by a swap function and is
called on all the blocks observed by the swap.

So, in some sense, the `usages` field keeps an approximate number of memory
IO operations on the block happened after the last swap operation. If the number
is large enough, it means that the block allocator is used quite frequently.
The algorithm is not perfect: some block allocators may be unreached by the swap
function, and the time passed between two consecutive swaps is an unbounded value
(it is even desirable to have this value as large as possible).
We don't discuss this algorithm further in the documentation because it is
very likely to be reimplemented in the future.

## _Swap Chunks_

As we've already mentioned above, it is necessary for any object using the
`SwapMemoryAllocator` to monitor the state of the most recently used
block allocator and to reallocate its memory if the block allocator is deactivated.

Let's get back to the object called `ASwapChunk`. It has already been discussed 
in the previous section that each `ASwapChunk` has a field `Header` and this
header is referenced in the `SwapBlockAllocator` that allocated the last memory
region of the chunk.

This object has an important method called `relocateMemory()`. It allocates
a new piece of memory, constructs a new header, and then tries to register the chunk
in the most recently used allocator. If at the moment the allocator is still active,
the method reads the SSD file backup into this new memory region. the method 
`relocateMemory()` is "coupled" with a `swap()` method. It is called if and only if 
a swap happened between two memory accesses to the memory region. The writing to SSD
happens in the `swap()`, while the reading happens in the `relocateMemory()`. But
as these methods belong to different classes, it should be now more clear why there is
a registration of the owners in `SwapBlockAllocator` and why the swap chunk and the
swap allocator should mutually point to each other's fields: they share a
common responsibility to restore a system state after each swap of the part of the
RAM on SSD.

It is essential for some IO operations on swap chunks to ensure that no `swap()`
happens during the operation: that is why each `SwapBlockAllocator` has a read-write
lock. The only place in the code where an exclusive write lock is taken is a `swap()`
function. The read lock, however, may be called from anywhere in the program and there
is a public method to call it.

It is called by `ASwapChunk` at the relocation stage
to ensure that the `swap()` doesn't iterate through the `owners` while
`relocateMemory()` modifies one of the elements and reads the file content to
the new address.

It is also called by all methods that modify the memory, e.g. `writeDouble()` or 
`writeInt()`: one cannot guarantee a
correctness if the flush of the data to the file is performed concurrently with
the data modification.

However, the methods that do not modify the memory, e.g. `readDouble()` or
`readInt()`, may perform without locking most of the time. It is done by first 
reading a memory at the given address and then checking the `active()` flag: if the
flag is set, it means that the written memory is valid and can be returned; otherwise,
this memory is garbage or is already held by other chunk, and `relocateMemory()`
should be performed. As the `active` field is declared volatile, a happens-before
ordering guarantees that the read operation is never reordered
after a flag check.