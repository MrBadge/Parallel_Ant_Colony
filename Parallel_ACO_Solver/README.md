# Parallel Ant Colony Solver for MEPhI project

This project is aimed to solve the MDVRPTW problem using the original ant algorithm but in parallel way. Each colony is places into a docker-container and all these slaves are controlled by the master-host.

## How to

1. Building and spinning up the master: `docker-compose up --build master`
2. Building the slave: `docker-compose up --build slave -d`
3. Growing the network: `docker-compose scale slave=10`

## Tasks input

Have a look at the test problem files...