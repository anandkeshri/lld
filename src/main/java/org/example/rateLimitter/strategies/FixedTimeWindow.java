package org.example.rateLimitter.strategies;

import org.example.rateLimitter.Config;
import org.example.rateLimitter.TimeUnit;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class FixedTimeWindow implements RateLimitStrategy{
    final private Config config;
    private final Map<Integer, Map<String, Integer>> requestCounter;
    public FixedTimeWindow(Config config){
        this.config = config;
        requestCounter = new HashMap<>();
    }
    @Override
    public synchronized boolean isAllowed(String key) {
        int window = getWindow();
        int count = countRequest(key, window);
        if(count< config.getReqThreshold()){
            if(!requestCounter.containsKey(window)){
                requestCounter.put(window, new HashMap<>());
                // remove old windows
                for(int i=0; i * config.getTimeWindowSize() < requestCounter.size(); i++){
                    if(i * config.getTimeWindowSize() != window) {
                        requestCounter.remove(i * config.getTimeWindowSize());
                    }
                }
            }
            Map<String, Integer> counterMap = requestCounter.getOrDefault(window, new HashMap<>());
            if(!counterMap.containsKey(key)){
                counterMap.put(key, 0);
            }
            counterMap.put(key, 1+counterMap.get(key));
            return true;
        }
        return false;
    }

    private int countRequest(String key, int window){
//        for(int win = 0; win < config.getTimeWindowSize(); win++) {
//            count += requestCounter.getOrDefault(window-win, new HashMap<>()).getOrDefault(key, 0);
//        }
        return requestCounter.getOrDefault(window, new HashMap<>()).getOrDefault(key, 0);
    }

    private int getWindow(){
        LocalTime now = LocalTime.now();
        return switch (config.getTimeUnit()){
            case TimeUnit.MIN -> now.getMinute()/config.getTimeWindowSize();
            case TimeUnit.SEC -> now.getSecond()/config.getTimeWindowSize();
        };
    }
}
