package cpuscheduler;

import java.util.List;

import cpuscheduler.Scheduler.Output;
import cpuscheduler.Scheduler.ProcessInput;
import cpuscheduler.Scheduler.ValidationException;

/** Dependency-free self-test with hand-calculated expectations. Run with run-tests.bat / run-tests.sh. */
public final class SchedulerTest {

    private static int checks = 0;

    private static void eq(Object expected, Object actual, String what) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void near(double expected, double actual, String what) {
        checks++;
        if (Math.abs(expected - actual) > 1e-9) {
            throw new AssertionError(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void fails(String message, Runnable r) {
        checks++;
        try {
            r.run();
        } catch (ValidationException e) {
            eq(message, e.getMessage(), "error message");
            return;
        }
        throw new AssertionError("expected ValidationException: " + message);
    }

    private static String gantt(Output o) {
        StringBuilder sb = new StringBuilder();
        o.chart().forEach(s -> sb.append(s.id()).append('[').append(s.start()).append('-').append(s.end()).append("] "));
        return sb.toString().trim();
    }

    public static void main(String[] args) {

        // A: arrives 0 burst 5 | B: arrives 1 burst 3 | C: arrives 2 burst 1   (priorities 1,2,3)
        List<ProcessInput> abc = List.of(
                new ProcessInput("A", 0, 5, 1),
                new ProcessInput("B", 1, 3, 2),
                new ProcessInput("C", 2, 1, 3));

        Output fcfs = Scheduler.schedule(abc, "FCFS", 2);
        eq("A[0-5] B[5-8] C[8-9]", gantt(fcfs), "FCFS gantt");
        near((0 + 4 + 6) / 3.0, fcfs.avgWaiting(), "FCFS avg waiting");
        near((5 + 7 + 7) / 3.0, fcfs.avgTurnaround(), "FCFS avg turnaround");

        Output sjf = Scheduler.schedule(abc, "SJF", 2);
        eq("A[0-5] C[5-6] B[6-9]", gantt(sjf), "SJF gantt");
        near((0 + 5 + 3) / 3.0, sjf.avgWaiting(), "SJF avg waiting");
        eq("A,B,C", String.join(",", sjf.results().stream().map(r -> r.id()).toList()), "rows keep input order");

        Output prio = Scheduler.schedule(List.of(
                new ProcessInput("A", 0, 4, 3),
                new ProcessInput("B", 0, 2, 1),
                new ProcessInput("C", 0, 3, 2)), "Priority", 2);
        eq("B[0-2] C[2-5] A[5-9]", gantt(prio), "Priority gantt (lower number first)");

        Output rr = Scheduler.schedule(abc, "RR", 2);
        eq("A[0-2] B[2-4] C[4-5] A[5-7] B[7-8] A[8-9]", gantt(rr), "RR gantt");
        eq(9L, rr.results().get(0).completion(), "RR completion A");
        eq(8L, rr.results().get(1).completion(), "RR completion B");
        eq(1L, rr.results().get(1).response(), "RR response B");

        Output idle = Scheduler.schedule(List.of(new ProcessInput("A", 3, 2, 1)), "FCFS", 2);
        eq("Idle[0-3] A[3-5]", gantt(idle), "idle gap");
        eq(0L, idle.results().get(0).waiting(), "no waiting after idle");

        Output rrIdle = Scheduler.schedule(List.of(
                new ProcessInput("A", 0, 1, 1), new ProcessInput("B", 5, 1, 1)), "RR", 2);
        eq("A[0-1] Idle[1-5] B[5-6]", gantt(rrIdle), "RR idle gap");

        // Analysis
        Analysis.Summary s = Analysis.summarize(abc, "FCFS", 2);
        eq(9L, s.makespan(), "makespan");
        near(100.0, s.utilization(), "utilization");
        near(3.0 / 9.0, s.throughput(), "throughput");
        eq("0.12", Analysis.fixed(0.125, 2), "Python-style half-even formatting");

        // Validation
        fails("Please enter at least one process.", () -> Scheduler.schedule(List.of(), "FCFS", 2));
        fails("Unknown scheduling algorithm. Choose FCFS, SJF, Priority, or RR.",
                () -> Scheduler.schedule(abc, "LIFO", 2));
        fails("Time quantum must be greater than zero.", () -> Scheduler.schedule(abc, "RR", 0));
        fails("Burst time of X must be greater than zero.",
                () -> Scheduler.schedule(List.of(new ProcessInput("X", 0, 0, 1)), "FCFS", 2));
        fails("Arrival time of X cannot be negative.",
                () -> Scheduler.schedule(List.of(new ProcessInput("X", -1, 1, 1)), "FCFS", 2));
        fails("Each process must have a unique Process ID.", () -> Scheduler.schedule(List.of(
                new ProcessInput("X", 0, 1, 1), new ProcessInput("X", 0, 1, 1)), "FCFS", 2));

        // Form workflow
        var form = App.parseForm("algorithm=SJF&action=compare&process_count=3&quantum=2"
                + "&pid=A&arrival=0&burst=5&priority=1&pid=B&arrival=1&burst=3&priority=2"
                + "&pid=C&arrival=2&burst=1&priority=3");
        var out = ScheduleService.run(form);
        eq(Boolean.TRUE, out.get("compared"), "compare flag");
        eq(5, ((List<?>) out.get("observations")).size(), "five observations");
        var bad = ScheduleService.run(App.parseForm("algorithm=FCFS&process_count=4"));
        eq("Please select a valid process count: 3, 5, 8, 10, 15", bad.get("error"), "bad count message");

        System.out.println("All " + checks + " checks passed.");
    }
}
