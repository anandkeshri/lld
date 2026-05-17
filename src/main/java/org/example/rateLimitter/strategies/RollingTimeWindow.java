package org.example.rateLimitter.strategies;

import org.example.rateLimitter.Config;
import org.example.rateLimitter.TimeUnit;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class RollingTimeWindow implements RateLimitStrategy{
    final private Config config;
    private Map<Integer, Map<String, Integer>> requestCounter;
    public RollingTimeWindow(Config config){
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
                int bound = window - config.getTimeWindowSize();
                if(bound>=0) {
                    for (int i = 0; i  < bound; i++) {
                        requestCounter.remove(i);
                    }
                }
                else {
                    bound = 60+bound;
                    for (int i = window+1; i  < bound; i++) {
                        requestCounter.remove(i);
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
        int count=0;
        for(int win = 0; win < config.getTimeWindowSize(); win++) {
            count += requestCounter.getOrDefault((60+window-win)%60, new HashMap<>()).getOrDefault(key, 0);
        }
        return count;
    }

    private int getWindow(){
        LocalTime now = LocalTime.now();
        return switch (config.getTimeUnit()){
            case TimeUnit.MIN -> now.getMinute();
            case TimeUnit.SEC -> now.getSecond();
        };
    }
}
