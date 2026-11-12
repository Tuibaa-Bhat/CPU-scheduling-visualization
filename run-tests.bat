@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -d out src\cpuscheduler\*.java test\cpuscheduler\*.java || exit /b 1
java -cp out cpuscheduler.SchedulerTest
