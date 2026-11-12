<<<<<<< HEAD
@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -d out src\cpuscheduler\*.java test\cpuscheduler\*.java || exit /b 1
java -cp out cpuscheduler.SchedulerTest
=======
@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -d out src\cpuscheduler\*.java test\cpuscheduler\*.java || exit /b 1
java -cp out cpuscheduler.SchedulerTest
>>>>>>> 7addbcb3072f767c6c36db92f4a0d6fee47c93e8
