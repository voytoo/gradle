package org.gradle.initialization;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * By Szczepan Faber on 7/5/13
 */
public class BuildPhaseProgress {
    private AtomicInteger remainingTasks;
    private int totalTasks;
    private String shortDescription;

    public BuildPhaseProgress(String shortDescription, int totalTasks) {
        this.totalTasks = totalTasks;
        remainingTasks = new AtomicInteger(0);
        this.shortDescription = shortDescription;
    }

    public String progress() {
        int currentTask = remainingTasks.incrementAndGet();
        if (currentTask > totalTasks) {
            throw new IllegalStateException("All operations have already completed.");
        }
        return shortDescription + " " + (int) (currentTask * 100.0 / totalTasks) + "%";
    }
}
