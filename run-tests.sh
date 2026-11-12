#!/usr/bin/env bash
# Compile and run the self-test.
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -d out src/cpuscheduler/*.java test/cpuscheduler/*.java
java -cp out cpuscheduler.SchedulerTest
