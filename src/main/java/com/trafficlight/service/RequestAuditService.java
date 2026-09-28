package com.trafficlight.service;

import com.trafficlight.model.HttpRequestLog;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@ApplicationScoped
public class RequestAuditService {

    private static final int MAX_LOGS = 30;
    private final ConcurrentLinkedDeque<HttpRequestLog> logs = new ConcurrentLinkedDeque<>();

    public void record(HttpRequestLog log) {
        logs.addFirst(log);
        while (logs.size() > MAX_LOGS) {
            logs.pollLast();
        }
    }

    public List<HttpRequestLog> getRecent(int limit) {
        int max = (limit <= 0) ? 10 : Math.min(limit, MAX_LOGS);
        List<HttpRequestLog> result = new ArrayList<>(max);
        int count = 0;
        for (HttpRequestLog entry : logs) {
            result.add(entry);
            count++;
            if (count >= max) {
                break;
            }
        }
        return result;
    }

    public void clear() {
        logs.clear();
    }
}
