package org.example.rateLimitter;

public class Config {
    private final TimeUnit timeUnit;
    private final int timeWindowSize;

    private final int reqThreshold;

    public Config(int reqThreshold, int timeWindowSize, TimeUnit unit){
        this.timeWindowSize = timeWindowSize;
        this.timeUnit = unit;
        this.reqThreshold = reqThreshold;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public int getTimeWindowSize() {
        return timeWindowSize;
    }

    public int getReqThreshold() {
        return reqThreshold;
    }
}
