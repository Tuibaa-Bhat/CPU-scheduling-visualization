package cpuscheduler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import cpuscheduler.Scheduler.Output;
import cpuscheduler.Scheduler.ProcessInput;
import cpuscheduler.Scheduler.Row;
import cpuscheduler.Scheduler.Segment;

/**
 * Performance metrics, same-input comparison of all algorithms and the
 * automatic observations shown after a comparison.
 */
public final class Analysis {

    private Analysis() {
    }

    /** All metrics of one scheduling run. */
    public record Summary(String algorithm,
                          double avgWaiting, double avgTurnaround, double avgResponse,
                          double utilization, long idleTime, double throughput,
                          long totalBurst, long makespan,
                          List<Row> results, List<Segment> ganttChart) {

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("algorithm", algorithm);
            m.put("avg_waiting", avgWaiting);
            m.put("avg_turnaround", avgTurnaround);
            m.put("avg_response", avgResponse);
            m.put("utilization", utilization);
            m.put("idle_time", idleTime);
            m.put("throughput", throughput);
            m.put("total_burst", totalBurst);
            m.put("makespan", makespan);
            m.put("results", rowMaps(results));
            m.put("gantt_chart", segmentMaps(ganttChart));
            return m;
        }
    }

    public static List<Map<String, Object>> rowMaps(List<Row> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Row r : rows) {
            out.add(r.toMap());
        }
        return out;
    }

    public static List<Map<String, Object>> segmentMaps(List<Segment> chart) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Segment s : chart) {
            out.add(s.toMap());
        }
        return out;
    }

    // ============================================================
    // Rounding / formatting that matches Python's round() and
    // format(x, ".2f"): exact binary value, ties to even.
    // ============================================================

    static double round(double value, int digits) {
        return new BigDecimal(value).setScale(digits, RoundingMode.HALF_EVEN).doubleValue();
    }

    static String fixed(double value, int digits) {
        return new BigDecimal(value).setScale(digits, RoundingMode.HALF_EVEN).toPlainString();
    }

    // ============================================================
    // 5. Performance metric calculation
    // ============================================================

    /** Runs one algorithm and calculates every performance metric. */
    public static Summary summarize(List<ProcessInput> processes, String algorithm, long quantum) {

        Output run = Scheduler.schedule(processes, algorithm, quantum);
        List<Row> rows = run.results();
        List<Segment> chart = run.chart();

        long totalBurst = 0;
        long responseSum = 0;
        for (Row row : rows) {
            totalBurst += row.burst();
            responseSum += row.response();
        }

        long makespan = 0;
        long idleTime = 0;
        for (Segment s : chart) {
            makespan = Math.max(makespan, s.end());
            if (s.id().equals("Idle")) {
                idleTime += s.end() - s.start();
            }
        }

        double avgResponse = rows.isEmpty() ? 0 : (double) responseSum / rows.size();
        double utilization = makespan > 0 ? ((double) totalBurst / makespan) * 100 : 0;
        double throughput = makespan > 0 ? (double) rows.size() / makespan : 0;

        return new Summary(algorithm, run.avgWaiting(), run.avgTurnaround(), avgResponse,
                utilization, idleTime, throughput, totalBurst, makespan, rows, chart);
    }

    // ============================================================
    // 6. Comparative analysis
    // ============================================================

    /** Runs all four algorithms on identical input. */
    public static List<Summary> compareAlgorithms(List<ProcessInput> processes, long quantum) {
        List<Summary> out = new ArrayList<>();
        for (String algorithm : Scheduler.ALGORITHMS) {
            out.add(summarize(processes, algorithm, quantum));
        }
        return out;
    }

    // ============================================================
    // 7. Automatic observations
    // ============================================================

    private static Summary best(List<Summary> all, ToDoubleFunction<Summary> key, boolean lowest) {
        Summary winner = all.get(0);
        for (Summary s : all) {
            // strict comparison: the first algorithm wins a tie (same as Python min/max)
            if (lowest ? key.applyAsDouble(s) < key.applyAsDouble(winner)
                       : key.applyAsDouble(s) > key.applyAsDouble(winner)) {
                winner = s;
            }
        }
        return winner;
    }

    private static Map<String, Object> observation(String title, Summary s, double value,
                                                   int digits, String description) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("algorithm", s.algorithm());
        m.put("value", round(value, digits));
        m.put("description", description);
        return m;
    }

    /** Observations generated from the actual simulation results. */
    public static List<Map<String, Object>> generateObservations(List<Summary> all) {

        List<Map<String, Object>> out = new ArrayList<>();

        if (all == null || all.isEmpty()) {
            return out;
        }

        Summary w = best(all, Summary::avgWaiting, true);
        out.add(observation("Lowest Average Waiting Time", w, w.avgWaiting(), 2,
                w.algorithm() + " achieved the lowest average waiting time of "
                        + fixed(w.avgWaiting(), 2) + " time units."));

        Summary t = best(all, Summary::avgTurnaround, true);
        out.add(observation("Lowest Average Turnaround Time", t, t.avgTurnaround(), 2,
                t.algorithm() + " achieved the lowest average turnaround time of "
                        + fixed(t.avgTurnaround(), 2) + " time units."));

        Summary r = best(all, Summary::avgResponse, true);
        out.add(observation("Lowest Average Response Time", r, r.avgResponse(), 2,
                r.algorithm() + " achieved the lowest average response time of "
                        + fixed(r.avgResponse(), 2) + " time units."));

        Summary u = best(all, Summary::utilization, false);
        out.add(observation("Highest CPU Utilization", u, u.utilization(), 2,
                u.algorithm() + " achieved the highest CPU utilization of "
                        + fixed(u.utilization(), 2) + "%."));

        Summary th = best(all, Summary::throughput, false);
        out.add(observation("Highest Throughput", th, th.throughput(), 4,
                th.algorithm() + " achieved the highest throughput of "
                        + fixed(th.throughput(), 4) + " processes per time unit."));

        return out;
    }
}
