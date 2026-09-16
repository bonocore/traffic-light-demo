package com.trafficlight.service;

import com.trafficlight.model.LightState;
import com.trafficlight.model.OperationMode;
import com.trafficlight.model.TrafficLightStatus;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class TrafficLightService {

    private static final int RED_DURATION = 10;
    private static final int AMBER_DURATION = 3;
    private static final int GREEN_DURATION = 10;

    private final AtomicReference<TrafficLightStatus> statusRef = new AtomicReference<>();
    private final BroadcastProcessor<TrafficLightStatus> eventProcessor = BroadcastProcessor.create();
    private ScheduledExecutorService scheduler;

    @PostConstruct
    void init() {
        TrafficLightStatus initial = new TrafficLightStatus(
            LightState.RED,
            OperationMode.AUTO,
            RED_DURATION,
            RED_DURATION,
            Instant.now(),
            "System initialized in AUTO mode (RED)"
        );
        statusRef.set(initial);

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "traffic-light-timer");
            t.setDaemon(true);
            return t;
        });

        // Tick every 1 second
        scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    @PreDestroy
    void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        eventProcessor.onComplete();
    }

    private synchronized void tick() {
        TrafficLightStatus current = statusRef.get();
        if (current.mode() != OperationMode.AUTO) {
            // In manual or emergency mode, tick still emits countdown if needed or keeps alive
            eventProcessor.onNext(current);
            return;
        }

        int remaining = current.remainingSeconds() - 1;
        if (remaining > 0) {
            TrafficLightStatus updated = new TrafficLightStatus(
                current.state(),
                current.mode(),
                remaining,
                current.cycleSeconds(),
                current.lastChanged(),
                current.message()
            );
            statusRef.set(updated);
            eventProcessor.onNext(updated);
            return;
        }

        // Timer elapsed, transition to next phase
        transitionNextAutoPhase(current);
    }

    private void transitionNextAutoPhase(TrafficLightStatus current) {
        LightState nextState;
        int nextDuration;

        switch (current.state()) {
            case GREEN -> {
                nextState = LightState.AMBER;
                nextDuration = AMBER_DURATION;
            }
            case AMBER -> {
                nextState = LightState.RED;
                nextDuration = RED_DURATION;
            }
            case RED, FLASHING_AMBER, OFF -> {
                nextState = LightState.GREEN;
                nextDuration = GREEN_DURATION;
            }
            default -> {
                nextState = LightState.RED;
                nextDuration = RED_DURATION;
            }
        }

        TrafficLightStatus nextStatus = new TrafficLightStatus(
            nextState,
            OperationMode.AUTO,
            nextDuration,
            nextDuration,
            Instant.now(),
            "Auto cycle transitioned to " + nextState
        );

        statusRef.set(nextStatus);
        eventProcessor.onNext(nextStatus);
    }

    public TrafficLightStatus getStatus() {
        return statusRef.get();
    }

    public Multi<TrafficLightStatus> getEventStream() {
        return eventProcessor;
    }

    public synchronized TrafficLightStatus setLightState(LightState newState, String operatorName) {
        if (newState == null) {
            throw new IllegalArgumentException("State cannot be null");
        }
        TrafficLightStatus updated = new TrafficLightStatus(
            newState,
            OperationMode.MANUAL,
            0,
            0,
            Instant.now(),
            "Manual override to " + newState + (operatorName != null ? " by " + operatorName : "")
        );
        statusRef.set(updated);
        eventProcessor.onNext(updated);
        return updated;
    }

    public synchronized TrafficLightStatus setMode(OperationMode newMode, String operatorName) {
        if (newMode == null) {
            throw new IllegalArgumentException("Mode cannot be null");
        }

        TrafficLightStatus current = statusRef.get();
        TrafficLightStatus updated;

        switch (newMode) {
            case AUTO -> {
                int duration = (current.state() == LightState.AMBER) ? AMBER_DURATION :
                               (current.state() == LightState.GREEN) ? GREEN_DURATION : RED_DURATION;
                LightState state = (current.state() == LightState.FLASHING_AMBER || current.state() == LightState.OFF)
                                   ? LightState.RED : current.state();
                updated = new TrafficLightStatus(
                    state,
                    OperationMode.AUTO,
                    duration,
                    duration,
                    Instant.now(),
                    "Switched to AUTO mode" + (operatorName != null ? " by " + operatorName : "")
                );
            }
            case EMERGENCY -> {
                updated = new TrafficLightStatus(
                    LightState.RED,
                    OperationMode.EMERGENCY,
                    0,
                    0,
                    Instant.now(),
                    "EMERGENCY OVERRIDE: All Stop (RED)" + (operatorName != null ? " by " + operatorName : "")
                );
            }
            case MANUAL -> {
                updated = new TrafficLightStatus(
                    current.state(),
                    OperationMode.MANUAL,
                    0,
                    0,
                    Instant.now(),
                    "Switched to MANUAL mode" + (operatorName != null ? " by " + operatorName : "")
                );
            }
            default -> throw new IllegalStateException("Unexpected mode: " + newMode);
        }

        statusRef.set(updated);
        eventProcessor.onNext(updated);
        return updated;
    }

    public synchronized TrafficLightStatus advanceNextPhase(String operatorName) {
        TrafficLightStatus current = statusRef.get();
        LightState nextState = switch (current.state()) {
            case GREEN -> LightState.AMBER;
            case AMBER -> LightState.RED;
            case RED, FLASHING_AMBER, OFF -> LightState.GREEN;
        };

        TrafficLightStatus updated = new TrafficLightStatus(
            nextState,
            current.mode(),
            (current.mode() == OperationMode.AUTO) ?
                (nextState == LightState.AMBER ? AMBER_DURATION :
                 nextState == LightState.GREEN ? GREEN_DURATION : RED_DURATION) : 0,
            (current.mode() == OperationMode.AUTO) ?
                (nextState == LightState.AMBER ? AMBER_DURATION :
                 nextState == LightState.GREEN ? GREEN_DURATION : RED_DURATION) : 0,
            Instant.now(),
            "Advanced phase to " + nextState + (operatorName != null ? " by " + operatorName : "")
        );

        statusRef.set(updated);
        eventProcessor.onNext(updated);
        return updated;
    }
}
