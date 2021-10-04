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
useful flags. First flag of the block indicates the "dirtiness": this flag
is set if the block data has been recently modified. Second flag indicates the
"activeness" of the block: if the block isn't active, its data is considered as a
garbage and may be reused in further allocations. The second flag is a bit excessive,
but it facilitates many operations as we will see later.

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
of the `SwapMemoryAllocator`. It never allocates RAM itself: instead, it
calls `allocate()` method of `VirtMemStorage` at the initialization stage
and corresponding `free()` on release.

The important detail in the implementation of the `SwapBlockAllocator`
is a swap operation. It is called whenever the `SwapMemoryAllocator` decides to
release a memory superblock controlled by the `SwapBlockAllocator` and to push it back
to the `VirtMemStorage`. However, in case of the swap external objects may still want to use the released
memory. That's why each external object that wants to use the memory allocated by
`SwapMemoryAllocator`should register itself in the memory of the `SwapBlockAllocator` it used for the
allocation.

In fact, directly after the swap the `SwapBlockAllocator` cannot guarantee anything 
about the validity of the memory it has already released. So it should somehow inform
the owners of its memory blocks that the memory they continue using is in
invalid state from now on. Of course, the memory doesn't just disappear:
each owner should provide its own backup space on disk from where the memory
may be further loaded back to RAM.

In the current implementation an "external object" is simply a chunk.
However, chunks carry a type information, and it is absolutely unnecessary 
to propagate this information to allocators. Instead, an object called
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
stored in an array called `owners`. There are some important memory order issues
with this 

The first step of the `swap()` operation is to iterate over the owners. 


# Dillinger
## _The Last Markdown Editor, Ever_

[![N|Solid](https://cldup.com/dTxpPi9lDf.thumb.png)](https://nodesource.com/products/nsolid)

[![Build Status](https://travis-ci.org/joemccann/dillinger.svg?branch=master)](https://travis-ci.org/joemccann/dillinger)

Dillinger is a cloud-enabled, mobile-ready, offline-storage compatible,
AngularJS-powered HTML5 Markdown editor.

- Type some Markdown on the left
- See HTML in the right
- ✨Magic ✨

## Features

- Import a HTML file and watch it magically convert to Markdown
- Drag and drop images (requires your Dropbox account be linked)
- Import and save files from GitHub, Dropbox, Google Drive and One Drive
- Drag and drop markdown and HTML files into Dillinger
- Export documents as Markdown, HTML and PDF

Markdown is a lightweight markup language based on the formatting conventions
that people naturally use in email.
As [John Gruber] writes on the [Markdown site][df1]

> The overriding design goal for Markdown's
> formatting syntax is to make it as readable
> as possible. The idea is that a
> Markdown-formatted document should be
> publishable as-is, as plain text, without
> looking like it's been marked up with tags
> or formatting instructions.

This text you see here is *actually- written in Markdown! To get a feel
for Markdown's syntax, type some text into the left window and
watch the results in the right.

## Tech

Dillinger uses a number of open source projects to work properly:

- [AngularJS] - HTML enhanced for web apps!
- [Ace Editor] - awesome web-based text editor
- [markdown-it] - Markdown parser done right. Fast and easy to extend.
- [Twitter Bootstrap] - great UI boilerplate for modern web apps
- [node.js] - evented I/O for the backend
- [Express] - fast node.js network app framework [@tjholowaychuk]
- [Gulp] - the streaming build system
- [Breakdance](https://breakdance.github.io/breakdance/) - HTML
to Markdown converter
- [jQuery] - duh

And of course Dillinger itself is open source with a [public repository][dill]
 on GitHub.

## Installation

Dillinger requires [Node.js](https://nodejs.org/) v10+ to run.

Install the dependencies and devDependencies and start the server.

```sh
cd dillinger
npm i
node app
```

For production environments...

```sh
npm install --production
NODE_ENV=production node app
```

## Plugins

Dillinger is currently extended with the following plugins.
Instructions on how to use them in your own application are linked below.

| Plugin | README |
| ------ | ------ |
| Dropbox | [plugins/dropbox/README.md][PlDb] |
| GitHub | [plugins/github/README.md][PlGh] |
| Google Drive | [plugins/googledrive/README.md][PlGd] |
| OneDrive | [plugins/onedrive/README.md][PlOd] |
| Medium | [plugins/medium/README.md][PlMe] |
| Google Analytics | [plugins/googleanalytics/README.md][PlGa] |

## Development

Want to contribute? Great!

Dillinger uses Gulp + Webpack for fast developing.
Make a change in your file and instantaneously see your updates!

Open your favorite Terminal and run these commands.

First Tab:

```sh
node app
```

Second Tab:

```sh
gulp watch
```

(optional) Third:

```sh
karma test
```

#### Building for source

For production release:

```sh
gulp build --prod
```

Generating pre-built zip archives for distribution:

```sh
gulp build dist --prod
```

## Docker

Dillinger is very easy to install and deploy in a Docker container.

By default, the Docker will expose port 8080, so change this within the
Dockerfile if necessary. When ready, simply use the Dockerfile to
build the image.

```sh
cd dillinger
docker build -t <youruser>/dillinger:${package.json.version} .
```

This will create the dillinger image and pull in the necessary dependencies.
Be sure to swap out `${package.json.version}` with the actual
version of Dillinger.

Once done, run the Docker image and map the port to whatever you wish on
your host. In this example, we simply map port 8000 of the host to
port 8080 of the Docker (or whatever port was exposed in the Dockerfile):

```sh
docker run -d -p 8000:8080 --restart=always --cap-add=SYS_ADMIN --name=dillinger <youruser>/dillinger:${package.json.version}
```

> Note: `--capt-add=SYS-ADMIN` is required for PDF rendering.

Verify the deployment by navigating to your server address in
your preferred browser.

```sh
127.0.0.1:8000
```

## License

MIT

**Free Software, Hell Yeah!**

[//]: # (These are reference links used in the body of this note and get stripped out when the markdown processor does its job. There is no need to format nicely because it shouldn't be seen. Thanks SO - http://stackoverflow.com/questions/4823468/store-comments-in-markdown-syntax)

[dill]: <https://github.com/joemccann/dillinger>
[git-repo-url]: <https://github.com/joemccann/dillinger.git>
[john gruber]: <http://daringfireball.net>
[df1]: <http://daringfireball.net/projects/markdown/>
[markdown-it]: <https://github.com/markdown-it/markdown-it>
[Ace Editor]: <http://ace.ajax.org>
[node.js]: <http://nodejs.org>
[Twitter Bootstrap]: <http://twitter.github.com/bootstrap/>
[jQuery]: <http://jquery.com>
[@tjholowaychuk]: <http://twitter.com/tjholowaychuk>
[express]: <http://expressjs.com>
[AngularJS]: <http://angularjs.org>
[Gulp]: <http://gulpjs.com>

[PlDb]: <https://github.com/joemccann/dillinger/tree/master/plugins/dropbox/README.md>
[PlGh]: <https://github.com/joemccann/dillinger/tree/master/plugins/github/README.md>
[PlGd]: <https://github.com/joemccann/dillinger/tree/master/plugins/googledrive/README.md>
[PlOd]: <https://github.com/joemccann/dillinger/tree/master/plugins/onedrive/README.md>
[PlMe]: <https://github.com/joemccann/dillinger/tree/master/plugins/medium/README.md>
[PlGa]: <https://github.com/RahulHP/dillinger/blob/master/plugins/googleanalytics/README.md>
