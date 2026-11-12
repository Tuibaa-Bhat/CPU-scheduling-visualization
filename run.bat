@echo off
rem Build and start the CPU Scheduling Visualizer (needs JDK 17+).
cd /d "%~dp0"
if not exist out mkdir out
javac -d out src\cpuscheduler\*.java || exit /b 1
java -cp out cpuscheduler.App %*
