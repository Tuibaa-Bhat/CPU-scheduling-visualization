package cpuscheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cpuscheduler.Analysis.Summary;
import cpuscheduler.Scheduler.ProcessInput;
import cpuscheduler.Scheduler.ValidationException;

/**
 * Form validation + simulation workflow (formerly the POST branch of the
 * Flask {@code home()} view).
 *
 * Form fields: algorithm, action (calculate | compare), process_count,
 * quantum, and the repeated fields pid / arrival / burst / priority.
 */
public final class ScheduleService {

    private ScheduleService() {
    }

    public static final List<Integer> ALLOWED_COUNTS = List.of(3, 5, 8, 10, 15);
    public static final long DEFAULT_QUANTUM = 2;

    // ============================================================
    // Default process generation
    // ============================================================

    public static List<ProcessInput> defaultProcesses(int count) {
        List<ProcessInput> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new ProcessInput("P" + (i + 1), 0, i + 1, i + 1));
        }
        return list;
    }

    /** Tops {@code processValues} up to {@code count} entries using unused P-numbers. */
    public static List<ProcessInput> fillWithDefaults(List<ProcessInput> processValues, int count) {
        Set<String> used = new HashSet<>();
        for (ProcessInput p : processValues) {
            used.add(p.id());
        }
        List<ProcessInput> defaults = defaultProcesses(count);
        int nextNum = 1;
        for (int i = processValues.size(); i < defaults.size(); i++) {
            while (used.contains("P" + nextNum)) {
                nextNum++;
            }
            String newId = "P" + nextNum;
            used.add(newId);
            ProcessInput t = defaults.get(i);
            processValues.add(new ProcessInput(newId, t.arrival(), t.burst(), t.priority()));
        }
        return processValues;
    }

    // ============================================================
    // Input validation
    // ============================================================

    static long parseInt(String raw, String label, Long minimum) {
        long value;
        try {
            value = Long.parseLong(String.valueOf(raw).strip());
        } catch (NumberFormatException e) {
            throw new ValidationException(label + " must be a valid whole number.");
        }
        if (minimum != null && value < minimum) {
            throw new ValidationException(label + " must be at least " + minimum + ".");
        }
        return value;
    }

    private static String first(Map<String, List<String>> form, String key, String fallback) {
        List<String> values = form.get(key);
        return (values == null || values.isEmpty()) ? fallback : values.get(0);
    }

    private static List<String> all(Map<String, List<String>> form, String key) {
        List<String> values = form.get(key);
        return values == null ? List.of() : values;
    }

    private static List<Map<String, Object>> processMaps(List<ProcessInput> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ProcessInput p : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.id());
            m.put("arrival", p.arrival());
            m.put("burst", p.burst());
            m.put("priority", p.priority());
            out.add(m);
        }
        return out;
    }

    // ============================================================
    // Main workflow
    // ============================================================

    /** Validates a submitted form, runs the simulation and returns every output value. */
    public static Map<String, Object> run(Map<String, List<String>> form) {

        List<Map<String, Object>> results = List.of();
        List<Map<String, Object>> ganttChart = List.of();
        List<Map<String, Object>> comparisonResults = List.of();
        List<Map<String, Object>> observations = List.of();

        double avgWaiting = 0, avgTurnaround = 0, avgResponse = 0, utilization = 0, throughput = 0;
        long idleTime = 0, makespan = 0;

        String error = null;
        boolean calculated = false;
        boolean compared = false;

        String algorithm = first(form, "algorithm", "FCFS");
        String action = first(form, "action", "calculate");
        long quantum = DEFAULT_QUANTUM;
        long processCount = 3;
        List<ProcessInput> processValues = defaultProcesses(3);

        try {

            if (!Scheduler.ALGORITHMS.contains(algorithm)) {
                throw new ValidationException("Please select a valid scheduling algorithm.");
            }

            if (!action.equals("calculate") && !action.equals("compare")) {
                throw new ValidationException("Invalid operation selected.");
            }

            processCount = parseInt(first(form, "process_count", "3"), "Number of processes", 1L);

            if (processCount > Integer.MAX_VALUE || !ALLOWED_COUNTS.contains((int) processCount)) {
                StringBuilder sb = new StringBuilder("Please select a valid process count: ");
                for (int i = 0; i < ALLOWED_COUNTS.size(); i++) {
                    sb.append(i == 0 ? "" : ", ").append(ALLOWED_COUNTS.get(i));
                }
                throw new ValidationException(sb.toString());
            }

            String rawQuantum = first(form, "quantum", String.valueOf(DEFAULT_QUANTUM));

            if (algorithm.equals("RR") || action.equals("compare")) {
                quantum = parseInt(rawQuantum, "Time quantum", 1L);
            } else {
                try {
                    quantum = parseInt(rawQuantum, "Time quantum", 1L);
                } catch (ValidationException e) {
                    quantum = DEFAULT_QUANTUM;
                }
            }

            List<String> ids = all(form, "pid");
            List<String> arrivals = all(form, "arrival");
            List<String> bursts = all(form, "burst");
            List<String> priorities = all(form, "priority");

            if (!(ids.size() == arrivals.size() && arrivals.size() == bursts.size())) {
                throw new ValidationException(
                        "Process data is incomplete. Please check all process fields.");
            }

            if (ids.size() < processCount) {
                throw new ValidationException(
                        "Please provide information for all selected processes.");
            }

            List<ProcessInput> submitted = new ArrayList<>();

            for (int i = 0; i < processCount; i++) {

                String pid = ids.get(i).strip();

                if (pid.isEmpty()) {
                    throw new ValidationException(
                            "Process ID in row " + (i + 1) + " cannot be empty.");
                }

                long arrival = parseInt(arrivals.get(i), "Arrival time of " + pid, 0L);
                long burst = parseInt(bursts.get(i), "Burst time of " + pid, 1L);

                long priority = 1;
                if (i < priorities.size() && !priorities.get(i).strip().isEmpty()) {
                    priority = parseInt(priorities.get(i), "Priority of " + pid, 1L);
                }

                submitted.add(new ProcessInput(pid, arrival, burst, priority));
            }

            Set<String> uniqueIds = new HashSet<>();
            for (ProcessInput p : submitted) {
                uniqueIds.add(p.id());
            }
            if (uniqueIds.size() != submitted.size()) {
                throw new ValidationException("Duplicate Process IDs are not allowed.");
            }

            processValues = submitted;

            // Selected algorithm
            Summary selected = Analysis.summarize(processValues, algorithm, quantum);

            results = Analysis.rowMaps(selected.results());
            ganttChart = Analysis.segmentMaps(selected.ganttChart());
            avgWaiting = selected.avgWaiting();
            avgTurnaround = selected.avgTurnaround();
            avgResponse = selected.avgResponse();
            utilization = selected.utilization();
            throughput = selected.throughput();
            idleTime = selected.idleTime();
            makespan = selected.makespan();
            calculated = true;

            // Compare all algorithms
            if (action.equals("compare")) {
                List<Summary> comparison = Analysis.compareAlgorithms(processValues, quantum);
                List<Map<String, Object>> maps = new ArrayList<>();
                for (Summary s : comparison) {
                    maps.add(s.toMap());
                }
                comparisonResults = maps;
                observations = Analysis.generateObservations(comparison);
                compared = true;
            }

        } catch (ValidationException exc) {

            error = exc.getMessage();
            calculated = false;
            compared = false;
            results = List.of();
            ganttChart = List.of();
            comparisonResults = List.of();
            observations = List.of();

        } catch (RuntimeException exc) {

            System.err.println("Unexpected scheduling application error.");
            exc.printStackTrace();

            error = "An unexpected error occurred during the scheduling simulation. "
                    + "Please check the terminal.";
            calculated = false;
            compared = false;
            results = List.of();
            ganttChart = List.of();
            comparisonResults = List.of();
            observations = List.of();
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("allowed_counts", ALLOWED_COUNTS);
        out.put("process_count", processCount);
        out.put("process_values", processMaps(processValues));
        out.put("results", results);
        out.put("gantt_chart", ganttChart);
        out.put("avg_waiting", avgWaiting);
        out.put("avg_turnaround", avgTurnaround);
        out.put("avg_response", avgResponse);
        out.put("utilization", utilization);
        out.put("throughput", throughput);
        out.put("idle_time", idleTime);
        out.put("makespan", makespan);
        out.put("comparison_results", comparisonResults);
        out.put("observations", observations);
        out.put("algorithm", algorithm);
        out.put("quantum", quantum);
        out.put("error", error);
        out.put("calculated", calculated);
        out.put("compared", compared);
        return out;
    }
}
