# CPU Scheduling Studio

A Java-based CPU Scheduling Visualizer that allows users to enter processes, execute common CPU scheduling algorithms, and compare their Gantt charts and performance statistics.

The backend is developed using plain Java (JDK only, without external libraries), and the frontend uses HTML, CSS, and JavaScript.

## Live Website

**Visit the CPU Scheduling Visualizer here:**

https://tuibaa-bhat.github.io/CPU-scheduling-visualization/

## GitHub Repository

https://github.com/Tuibaa-Bhat/CPU-scheduling-visualization

## Features

- Run individual CPU scheduling algorithms.
- Compare all scheduling algorithms using the same process data.
- Generate Gantt charts to visualize process execution.
- Calculate average waiting time, turnaround time, and response time.
- Display CPU utilization and makespan.
- Support idle intervals and Round Robin time slices.
- View process completion times and scheduling statistics.

## Scheduling Algorithms

1. **FCFS (First Come First Serve):** Executes processes in order of arrival time.
2. **SJF (Shortest Job First):** Selects the available process with the shortest burst time.
3. **Priority Scheduling:** Executes the available process with the highest priority (lowest positive priority number).
4. **Round Robin:** Uses a configurable time quantum to execute processes in turns.

## Technologies Used

- Java (JDK 17 or newer)
- HTML5
- CSS3
- JavaScript
- Java HTTP Server
- JSON

## Run Locally

### Requirements

Install JDK 17 or newer.

Check Java installation:

```bash
java -version
javac -version
```

### Windows

Open the project folder in the terminal and execute:

```cmd
run.bat
```

### macOS / Linux

Execute:

```bash
./run.sh
```

The script compiles the Java source files into the `out/` directory, starts the local server, and opens the application in your browser.

The local application runs at:

```text
http://127.0.0.1:5000
```

Stop the server using `Ctrl + C`.

### Additional Options

- `--no-browser`: Starts the server without automatically opening a browser.
- `NO_BROWSER=1`: Prevents automatic browser opening.
- `PORT=8080`: Changes the server port.

Run the self-test using:

```cmd
run-tests.bat
```

On macOS/Linux:

```bash
./run-tests.sh
```

## Project Structure

```text
CPU-Scheduling-Studio/
│
├── src/
│   └── cpuscheduler/
│       ├── Analysis.java
│       ├── App.java
│       ├── Json.java
│       ├── Scheduler.java
│       └── ScheduleService.java
│
├── static/
│   ├── script.js
│   └── style.css
│
├── templates/
│   └── index.html
│
├── test/
│   └── cpuscheduler/
│       └── SchedulerTest.java
│
├── out/
├── index.html
├── run.bat
├── run.sh
├── run-tests.bat
├── run-tests.sh
├── README.md
└── REPORT_TEMPLATE.md
```

## HTTP Endpoints

| Endpoint | Description |
|---|---|
| `GET /` | Displays the web interface |
| `GET /health` | Returns server health status |
| `GET /static/...` | Serves CSS and JavaScript files |
| `POST /api/schedule` | Executes scheduling simulations |

## Performance Metrics

The application calculates:

- Completion Time
- Turnaround Time
- Waiting Time
- Response Time
- Average Waiting Time
- Average Turnaround Time
- CPU Utilization
- Makespan

## Project Contributors

- **Chandra Prakash** – Developer
- **Tuibaa Altaf Bhat** – Developer

## Team Members

This project was collaboratively developed by:

- [ChandraPrakash-1103](https://github.com/ChandraPrakash-1103)
- [Tuibaa-Bhat](https://github.com/Tuibaa-Bhat)

## Assessment Evidence

The project demonstrates four CPU scheduling algorithms, Gantt chart visualization, process performance metrics, and comparative analysis.

Refer to `REPORT_TEMPLATE.md` for the demonstration procedure, screenshots checklist, hand calculations, analysis prompts, and team contribution details.

Actual experiment results should be recorded after running and testing the application.

## Deployment Information

The live website is published using GitHub Pages.

**Live URL:** https://tuibaa-bhat.github.io/CPU-scheduling-visualization/
