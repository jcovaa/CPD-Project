# Graphs for Block algorithms

## Introduction

The **Block Matrix Multiplication** algorithm divides large matrixes into smaller sub-blocks and ensures that data stays within the CPU's high-speed cache for multiple operations, drastically reducing the need to access the slower Main Memory.

## Results

The graph below compares three different block sizes (**128, 256, and 512**) to determine its effect on performance.

![Block Analysis](.//block_analysis.png)

### Conclusions

The data reveals a clear relationship between block size and execution efficiency. Generally, larger blocks tend to perform better as they reduce the overhead of the outer loops and maximize the amount of work done once a chunk of data is loaded into the cache. However, this is limited by the physical size of the CPU's cache: if a block is too large, it will spill into the main memory, negating the benefits of the blocking strategy.
