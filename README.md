# Parallel Ant Colony Solver for MEPhI project

This project is aimed to solve the MDVRPTW problem using the original ant algorithm but in parallel way. Each colony is places into a docker-container and all these slaves are controlled by the master-host.

## Parts

1. Parallel_ACO_Solver - Core python solver, based on Docker containers
2. AntColonyWebGUI - WEB-based GUI for task input
3. GoogleMatrixGetter - tools for building distance matrixes based on real adress locations
4. AntColonyOptimization - Core solver, written on Java

## How to

Have a look at each project Readme
