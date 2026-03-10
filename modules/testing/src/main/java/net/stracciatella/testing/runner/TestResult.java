package net.stracciatella.testing.runner;

public record TestResult(String suiteName, String testName, Status status, String message, long durationMs) {
    public enum Status {
        PASSED, FAILED, TIMED_OUT, ERROR
    }
}
