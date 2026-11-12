#!/usr/bin/env bash
# Build and start the CPU Scheduling Visualizer (needs JDK 17+).
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -d out src/cpuscheduler/*.java
exec java -cp out cpuscheduler.App "$@"
