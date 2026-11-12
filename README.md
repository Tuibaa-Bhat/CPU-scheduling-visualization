<<<<<<< HEAD
# CPU Scheduling Studio

A small Java app for entering processes, running common CPU scheduling policies, and comparing their Gantt charts and timing statistics. The backend is plain Java (JDK only, no libraries); the web interface in `templates/index.html` is served unchanged.

Use **Run schedule** to inspect one policy or **Compare all policies** to run FCFS, SJF, Priority, and Round Robin on the same process data. The comparison includes average waiting, turnaround and response times, CPU utilization, and makespan. The process table includes first start and response time, and the Gantt chart shows idle intervals and Round Robin slices.

## Run locally

Requires a JDK, version 17 or newer (`java -version` and `javac -version` should both work).

```powershell
.\run.bat
```

On macOS/Linux use `./run.sh`. The script compiles `src/` into `out/`, starts the server and opens <https://tuibaa-bhat.github.io/CPU-scheduling-visualization/> (in Microsoft Edge if it is installed, otherwise your default browser). Stop the server with `Ctrl+C`.

Options: `--no-browser` (or env `NO_BROWSER=1`) skips opening the browser; env `PORT=8080` changes the port. Run the self-test with `run-tests.bat` / `./run-tests.sh`.

Without the scripts:

```
javac -d out src/cpuscheduler/*.java
java -cp out cpuscheduler.App
```

Always start it from this folder (the one containing `templates/` and `static/`), or pass `-Dapp.dir=<this folder>`.

## Policies

- **FCFS**: non-preemptive, ordered by arrival time; ties keep the input order.
- **SJF**: non-preemptive; chooses the shortest burst among processes that have arrived.
- **Priority**: non-preemptive; the lowest positive priority number wins. Ties use arrival time, then input order.
- **Round Robin**: preemptive, with a configurable positive integer quantum. Processes that arrive during a time slice join the ready queue before the running process is requeued.

The app reports completion time, turnaround time (`completion − arrival`), waiting time (`turnaround − burst`), average waiting and turnaround times, and CPU utilization. Idle gaps are included in the Gantt chart. All times use abstract integer units, and the model has one CPU.

## HTTP endpoints

- `GET /` — the web interface (`POST /` returns the same page).
- `GET /static/...` — files from `static/`.
- `GET /health` — JSON status.
- `POST /api/schedule` — runs a simulation and returns JSON. Form-encoded fields: `algorithm` (FCFS, SJF, Priority, RR), `action` (`calculate` or `compare`), `process_count` (3, 5, 8, 10 or 15), `quantum`, and one `pid`, `arrival`, `burst`, `priority` per process.

## Project files

- `src/cpuscheduler/Scheduler.java` — scheduling algorithms, validation and timing calculations.
- `src/cpuscheduler/Analysis.java` — performance metrics, same-input comparison and automatic observations.
- `src/cpuscheduler/ScheduleService.java` — form validation and the simulation workflow.
- `src/cpuscheduler/App.java` — HTTP server and routes (built on the JDK's `com.sun.net.httpserver`).
- `src/cpuscheduler/Json.java` — small JSON writer.
- `test/cpuscheduler/SchedulerTest.java` — hand-calculated self-test.
- `templates/index.html` — responsive interface, Gantt chart, and results table.
- `static/` — additional stylesheet and script.
- `REPORT_TEMPLATE.md` — reproducible demonstration procedure, evidence checklist, analysis prompts, and team contribution record for the course rubric.

## Assessment evidence

The project supports simulation execution through configurable inputs and four policies; observation through a Gantt chart and per-process/aggregate metrics; and comparative analysis through the same-input policy comparison. Use `REPORT_TEMPLATE.md` to record your actual runs, screenshots, hand calculations, workload-based conclusions, environment, and individual contributions. The report prompts are not pre-filled with fabricated experiment results: complete them from your demonstration and be ready to explain your own work.

## Project Contributors

- Chandra Prakash – Developer
- Tuibaa ALtaf Bhat – Developer

## Team Members

This project was collaboratively developed by
[ChandraPrakash-1103](https://github.com/ChandraPrakash-1103)
and [Tuibaa-Bhat](https://github.com/Tuibaa-Bhat).
=======
# CPU Scheduling Studio

A small Java app for entering processes, running common CPU scheduling policies, and comparing their Gantt charts and timing statistics. The backend is plain Java (JDK only, no libraries); the web interface in `templates/index.html` is served unchanged.

Use **Run schedule** to inspect one policy or **Compare all policies** to run FCFS, SJF, Priority, and Round Robin on the same process data. The comparison includes average waiting, turnaround and response times, CPU utilization, and makespan. The process table includes first start and response time, and the Gantt chart shows idle intervals and Round Robin slices.

## Run locally

Requires a JDK, version 17 or newer (`java -version` and `javac -version` should both work).

```powershell
.\run.bat
```

On macOS/Linux use `./run.sh`. The script compiles `src/` into `out/`, starts the server and opens <http://127.0.0.1:5000> (in Microsoft Edge if it is installed, otherwise your default browser). Stop the server with `Ctrl+C`.

Options: `--no-browser` (or env `NO_BROWSER=1`) skips opening the browser; env `PORT=8080` changes the port. Run the self-test with `run-tests.bat` / `./run-tests.sh`.

Without the scripts:

```
javac -d out src/cpuscheduler/*.java
java -cp out cpuscheduler.App
```

Always start it from this folder (the one containing `templates/` and `static/`), or pass `-Dapp.dir=<this folder>`.

## Policies

- **FCFS**: non-preemptive, ordered by arrival time; ties keep the input order.
- **SJF**: non-preemptive; chooses the shortest burst among processes that have arrived.
- **Priority**: non-preemptive; the lowest positive priority number wins. Ties use arrival time, then input order.
- **Round Robin**: preemptive, with a configurable positive integer quantum. Processes that arrive during a time slice join the ready queue before the running process is requeued.

The app reports completion time, turnaround time (`completion − arrival`), waiting time (`turnaround − burst`), average waiting and turnaround times, and CPU utilization. Idle gaps are included in the Gantt chart. All times use abstract integer units, and the model has one CPU.

## HTTP endpoints

- `GET /` — the web interface (`POST /` returns the same page).
- `GET /static/...` — files from `static/`.
- `GET /health` — JSON status.
- `POST /api/schedule` — runs a simulation and returns JSON. Form-encoded fields: `algorithm` (FCFS, SJF, Priority, RR), `action` (`calculate` or `compare`), `process_count` (3, 5, 8, 10 or 15), `quantum`, and one `pid`, `arrival`, `burst`, `priority` per process.

## Project files

- `src/cpuscheduler/Scheduler.java` — scheduling algorithms, validation and timing calculations.
- `src/cpuscheduler/Analysis.java` — performance metrics, same-input comparison and automatic observations.
- `src/cpuscheduler/ScheduleService.java` — form validation and the simulation workflow.
- `src/cpuscheduler/App.java` — HTTP server and routes (built on the JDK's `com.sun.net.httpserver`).
- `src/cpuscheduler/Json.java` — small JSON writer.
- `test/cpuscheduler/SchedulerTest.java` — hand-calculated self-test.
- `templates/index.html` — responsive interface, Gantt chart, and results table.
- `static/` — additional stylesheet and script.
- `REPORT_TEMPLATE.md` — reproducible demonstration procedure, evidence checklist, analysis prompts, and team contribution record for the course rubric.

## Assessment evidence

The project supports simulation execution through configurable inputs and four policies; observation through a Gantt chart and per-process/aggregate metrics; and comparative analysis through the same-input policy comparison. Use `REPORT_TEMPLATE.md` to record your actual runs, screenshots, hand calculations, workload-based conclusions, environment, and individual contributions. The report prompts are not pre-filled with fabricated experiment results: complete them from your demonstration and be ready to explain your own work.

## Project Contributors

- Chandra Prakash – Developer
- Tuibaa ALtaf Bhat – Developer

## Team Members

This project was collaboratively developed by
[ChandraPrakash-1103](https://github.com/ChandraPrakash-1103)
and [Tuibaa-Bhat](https://github.com/Tuibaa-Bhat).
>>>>>>> 7addbcb3072f767c6c36db92f4a0d6fee47c93e8
