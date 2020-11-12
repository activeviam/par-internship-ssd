Data storage on files powered by NVMe
===================

Project architecture
-----------

Currently, the project features a reference allocator storing data on files: `MemoryAllocatorOnFile`.
Though implemented in a straight-forward way, it still has several underlying chunks depending
on the size of inner blocks it allocates.<br>
The interface `MemoryAllocator` is the specification for any allocator tested in this project.

This allocator is used to implement `Chunk`s, that represent an array of raw data. ActiveViam having
goals to get the best of the machine performance, Chunks cannot have generic methods, not to pay
the cost of boxing/unboxing. That's the main reason for the existence of `Chunk`. It provides
read and write methods adapted for primitive types. In this scholar project, we only define methods
for `int` and `double`.

The project already ships two sets of implementations:

  - the series of `HeapXyzChunk`, acting as a reference, using an underlying Java array
  - the series of `FileXyzChunk`, that makes use of the `MemoryAllocatorOnFile`.

All Chunk implementations are succinctly tested. Those tests can be used as references for testing
new implementations of Chunks or Allocators.

Development environment
-----------

This project is written in Java 15, though it does not use many of the latest features so far.

It uses Maven to manage its dependencies.

### Compiling the project

```bash
mvn compile
```

Build print compilation warnings due to the usage of `sun.misc.Unsafe`. This is indeed an internal
API but it has been accepted by many vendors. [Project Panama](https://github.com/openjdk/panama-foreign/)
aims at providing safer alternatives, but there are not ready yet.

### Running unit tests

```bash
mvn test
```
