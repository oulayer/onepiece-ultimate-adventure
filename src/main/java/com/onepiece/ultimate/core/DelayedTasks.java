package com.onepiece.ultimate.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiny per-owner scheduler measured in level game time. Used to land hits on the exact
 * "impact" frame of an animation (wind-up), and for multi-stage moves.
 * Owned by a player's data object, so tasks die with the player – nothing is kept globally.
 */
public final class DelayedTasks {
    private record Task(long at, Runnable action) {}

    private final List<Task> tasks = new ArrayList<>();

    public void schedule(long now, int delay, Runnable action) {
        if (delay <= 0) action.run();
        else tasks.add(new Task(now + delay, action));
    }

    public void tick(long now) {
        if (tasks.isEmpty()) return;
        // copy: tasks can schedule new tasks
        List<Task> due = null;
        for (int i = tasks.size() - 1; i >= 0; i--) {
            Task t = tasks.get(i);
            if (t.at <= now) {
                if (due == null) due = new ArrayList<>();
                due.add(t);
                tasks.remove(i);
            }
        }
        if (due != null) {
            for (int i = due.size() - 1; i >= 0; i--) due.get(i).action.run();
        }
    }

    public void clear() {
        tasks.clear();
    }

    public int size() {
        return tasks.size();
    }
}
