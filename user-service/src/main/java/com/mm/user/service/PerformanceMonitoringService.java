package com.mm.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class PerformanceMonitoringService {
    
    private final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> timers = new ConcurrentHashMap<>();
    
    public void incrementCounter(String name) {
        counters.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    public long getCounter(String name) {
        return counters.getOrDefault(name, new AtomicLong(0)).get();
    }
    
    public void startTimer(String name) {
        timers.put(name, System.currentTimeMillis());
    }
    
    public long stopTimer(String name) {
        Long startTime = timers.remove(name);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            log.info("Timer {}: {}ms", name, duration);
            return duration;
        }
        return 0;
    }
    
    public void logPerformance(String operation, int count, long timeMs) {
        double rate = (double) count / (timeMs / 1000.0);
        log.info("Performance - {}: {} items in {}ms ({} items/sec)", 
                operation, count, timeMs, String.format("%.2f", rate));
    }
    
    public void resetCounters() {
        counters.clear();
        timers.clear();
    }
} 