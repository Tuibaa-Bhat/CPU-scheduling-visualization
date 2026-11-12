<<<<<<< HEAD
@echo off
rem Build and start the CPU Scheduling Visualizer (needs JDK 17+).
cd /d "%~dp0"
if not exist out mkdir out
javac -d out src\cpuscheduler\*.java || exit /b 1
java -cp out cpuscheduler.App %*
=======
@echo off
rem Build and start the CPU Scheduling Visualizer (needs JDK 17+).
cd /d "%~dp0"
if not exist out mkdir out
javac -d out src\cpuscheduler\*.java || exit /b 1
java -cp out cpuscheduler.App %*
>>>>>>> 7addbcb3072f767c6c36db92f4a0d6fee47c93e8
