package cpuscheduler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CPU Scheduling Visualizer - scheduling algorithms.
 *
 * <ol>
 *   <li>FCFS     - First Come First Served</li>
 *   <li>SJF      - Shortest Job First (non-preemptive)</li>
 *   <li>Priority - Priority scheduling (non-preemptive, lower number = higher priority)</li>
 *   <li>RR       - Round Robin (preemptive)</li>
 * </ol>
 *
 * Features: arrival times, CPU idle detection, Gantt chart generation,
 * completion / turnaround / waiting / response times, input validation and
 * a stable (input-order) tie-break.
 */
public final class Scheduler {

    private Scheduler() {
    }

    public static final List<String> ALGORITHMS = List.of("FCFS", "SJF", "Priority", "RR");

    // ============================================================
    // Data types
    // ============================================================

    /** Raw (unvalidated) process as entered by the user. */
    public record ProcessInput(String id, long arrival, long burst, long priority) {
    }

    /** Validated process; {@code order} is the position in the user's input. */
    public record Job(String id, long arrival, long burst, long priority, int order) {
    }

    /** One finished process with all of its timing metrics. */
    public record Row(String id, long arrival, long burst, long priority,
                      long start, long completion, long turnaround,
                      long waiting, long response) {

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("arrival", arrival);
            m.put("burst", burst);
            m.put("priority", priority);
            m.put("start", start);
            m.put("completion", completion);
            m.put("turnaround", turnaround);
            m.put("waiting", waiting);
            m.put("response", response);
            return m;
        }
    }

    /** One block of the Gantt chart ("Idle" for CPU idle time). */
    public record Segment(String id, long start, long end) {

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("start", start);
            m.put("end", end);
            return m;
        }
    }

    /** Result of {@link #schedule}. */
    public record Output(List<Row> results, List<Segment> chart,
                         double avgWaiting, double avgTurnaround) {
    }

    /** Invalid user input (the Java counterpart of Python's ValueError). */
    public static class ValidationException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ValidationException(String message) {
            super(message);
        }
    }

    // ============================================================
    // 1. Input validation
    // ============================================================

    public static List<Job> validateProcesses(List<ProcessInput> processes) {

        if (processes == null || processes.isEmpty()) {
            throw new ValidationException("Please enter at least one process.");
        }

        List<Job> jobs = new ArrayList<>();

        for (int index = 0; index < processes.size(); index++) {

            ProcessInput p = processes.get(index);

            if (p == null || p.id() == null) {
                throw new ValidationException("Please provide valid process information.");
            }

            String pid = p.id().strip();

            if (pid.isEmpty()) {
                throw new ValidationException("Process ID cannot be empty.");
            }
            if (p.arrival() < 0) {
                throw new ValidationException("Arrival time of " + pid + " cannot be negative.");
            }
            if (p.burst() <= 0) {
                throw new ValidationException("Burst time of " + pid + " must be greater than zero.");
            }
            if (p.priority() <= 0) {
                throw new ValidationException("Priority of " + pid + " must be greater than zero.");
            }

            jobs.add(new Job(pid, p.arrival(), p.burst(), p.priority(), index));
        }

        Set<String> unique = new HashSet<>();
        for (Job job : jobs) {
            unique.add(job.id());
        }
        if (unique.size() != jobs.size()) {
            throw new ValidationException("Each process must have a unique Process ID.");
        }

        return jobs;
    }

    // ============================================================
    // 2. Gantt chart helper
    // ============================================================

    private static void addGanttEntry(List<Segment> chart, String pid, long start, long end) {
        if (end <= start) {
            return;
        }
        chart.add(new Segment(pid, start, end));
    }

    private static Row finished(Job job, long start, long completion) {
        long turnaround = completion - job.arrival();
        long waiting = turnaround - job.burst();
        long response = start - job.arrival();
        return new Row(job.id(), job.arrival(), job.burst(), job.priority(),
                start, completion, turnaround, waiting, response);
    }

    // ============================================================
    // 3-5. Non-preemptive algorithms (FCFS, SJF, Priority)
    //
    // They only differ in which available process is picked next:
    //   FCFS     -> (arrival, order)
    //   SJF      -> (burst, arrival, order)
    //   Priority -> (priority, arrival, order)   lower number wins
    // ============================================================

    private static final class Partial {
        final List<Row> results = new ArrayList<>();
        final List<Segment> chart = new ArrayList<>();
    }

    private static Partial nonPreemptive(List<Job> jobs, Comparator<Job> pick) {

        List<Job> remaining = new ArrayList<>(jobs);
        Partial out = new Partial();
        long currentTime = 0;

        while (!remaining.isEmpty()) {

            Job selected = null;

            for (Job job : remaining) {
                if (job.arrival() <= currentTime
                        && (selected == null || pick.compare(job, selected) < 0)) {
                    selected = job;
                }
            }

            // CPU idle: jump to the next arrival
            if (selected == null) {

                long nextArrival = Long.MAX_VALUE;
                for (Job job : remaining) {
                    nextArrival = Math.min(nextArrival, job.arrival());
                }

                addGanttEntry(out.chart, "Idle", currentTime, nextArrival);
                currentTime = nextArrival;

                for (Job job : remaining) {
                    if (job.arrival() <= currentTime
                            && (selected == null || pick.compare(job, selected) < 0)) {
                        selected = job;
                    }
                }
            }

            long startTime = currentTime;
            long completionTime = startTime + selected.burst();

            addGanttEntry(out.chart, selected.id(), startTime, completionTime);
            out.results.add(finished(selected, startTime, completionTime));

            currentTime = completionTime;
            remaining.remove(selected);
        }

        return out;
    }

    private static final Comparator<Job> BY_FCFS =
            Comparator.comparingLong(Job::arrival).thenComparingInt(Job::order);

    private static final Comparator<Job> BY_SJF =
            Comparator.comparingLong(Job::burst)
                    .thenComparingLong(Job::arrival)
                    .thenComparingInt(Job::order);

    private static final Comparator<Job> BY_PRIORITY =
            Comparator.comparingLong(Job::priority)
                    .thenComparingLong(Job::arrival)
                    .thenComparingInt(Job::order);

    // ============================================================
    // 6. Round Robin (preemptive)
    // ============================================================

    private static Partial roundRobin(List<Job> jobs, long quantum) {

        if (quantum <= 0) {
            throw new ValidationException("Time quantum must be greater than zero.");
        }

        List<Job> sorted = new ArrayList<>(jobs);
        sorted.sort(BY_FCFS);

        Map<String, Long> remainingTime = new HashMap<>();
        for (Job job : sorted) {
            remainingTime.put(job.id(), job.burst());
        }

        Map<String, Long> firstStart = new HashMap<>();
        Map<String, Long> completionTime = new HashMap<>();
        Deque<Job> readyQueue = new ArrayDeque<>();
        Partial out = new Partial();

        long currentTime = 0;
        int nextProcess = 0;
        int total = sorted.size();

        while (nextProcess < total || !readyQueue.isEmpty()) {

            // Ready queue empty: jump ahead to the next arrival
            if (readyQueue.isEmpty()) {

                long nextArrival = sorted.get(nextProcess).arrival();

                if (currentTime < nextArrival) {
                    addGanttEntry(out.chart, "Idle", currentTime, nextArrival);
                }

                currentTime = Math.max(currentTime, nextArrival);

                while (nextProcess < total && sorted.get(nextProcess).arrival() <= currentTime) {
                    readyQueue.addLast(sorted.get(nextProcess));
                    nextProcess++;
                }
            }

            Job process = readyQueue.pollFirst();
            String pid = process.id();

            firstStart.putIfAbsent(pid, currentTime);

            long executionTime = Math.min(quantum, remainingTime.get(pid));
            long startTime = currentTime;
            long endTime = currentTime + executionTime;

            addGanttEntry(out.chart, pid, startTime, endTime);

            currentTime = endTime;
            remainingTime.put(pid, remainingTime.get(pid) - executionTime);

            // Processes that arrived during this slice queue up BEFORE the
            // preempted process is put back.
            while (nextProcess < total && sorted.get(nextProcess).arrival() <= currentTime) {
                readyQueue.addLast(sorted.get(nextProcess));
                nextProcess++;
            }

            if (remainingTime.get(pid) > 0) {
                readyQueue.addLast(process);
            } else {
                completionTime.put(pid, currentTime);
            }
        }

        for (Job job : sorted) {
            out.results.add(finished(job, firstStart.get(job.id()), completionTime.get(job.id())));
        }

        return out;
    }

    // ============================================================
    // 7. Main scheduling entry point
    // ============================================================

    /**
     * Runs one scheduling algorithm.
     *
     * @param processes processes in input order
     * @param algorithm FCFS, SJF, Priority or RR
     * @param quantum   time quantum (only used by RR)
     */
    public static Output schedule(List<ProcessInput> processes, String algorithm, long quantum) {

        if (algorithm == null || !ALGORITHMS.contains(algorithm)) {
            throw new ValidationException(
                    "Unknown scheduling algorithm. Choose FCFS, SJF, Priority, or RR.");
        }

        List<Job> jobs = validateProcesses(processes);

        if (algorithm.equals("RR") && quantum <= 0) {
            throw new ValidationException("Time quantum must be greater than zero.");
        }

        Partial run;
        switch (algorithm) {
            case "FCFS" -> run = nonPreemptive(jobs, BY_FCFS);
            case "SJF" -> run = nonPreemptive(jobs, BY_SJF);
            case "Priority" -> run = nonPreemptive(jobs, BY_PRIORITY);
            default -> run = roundRobin(jobs, quantum);
        }

        // Result rows go back to the user's original input order
        Map<String, Integer> originalOrder = new HashMap<>();
        for (Job job : jobs) {
            originalOrder.put(job.id(), job.order());
        }

        List<Row> results = new ArrayList<>(run.results);
        results.sort(Comparator.comparingInt(r -> originalOrder.get(r.id())));

        double sumWaiting = 0;
        double sumTurnaround = 0;
        for (Row r : results) {
            sumWaiting += r.waiting();
            sumTurnaround += r.turnaround();
        }

        return new Output(results, run.chart,
                sumWaiting / results.size(), sumTurnaround / results.size());
    }
}
