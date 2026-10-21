package com.stride.form;

import java.util.ArrayList;
import java.util.List;

/** Platform-independent signal processing shared by the Android analyzer and offline tests. */
public final class GaitMath {
    private GaitMath() {}

    public static float angle(float ax, float ay, float bx, float by, float cx, float cy) {
        double ux = ax - bx, uy = ay - by, vx = cx - bx, vy = cy - by;
        double norm = Math.hypot(ux, uy) * Math.hypot(vx, vy);
        if (norm < 1e-7) return 0f;
        double cosine = Math.max(-1, Math.min(1, (ux * vx + uy * vy) / norm));
        return (float) Math.toDegrees(Math.acos(cosine));
    }

    /** Hysteretic alternating ankle events. Missing/long gaps reset the phase. */
    public static long[] stepEvents(long[] times, float[] differences) {
        if (times.length != differences.length) throw new IllegalArgumentException("Signal lengths differ");
        List<Long> events = new ArrayList<>();
        int previous = 0;
        long previousSwitchTime = -1000, previousSampleTime = -1000;
        for (int i = 0; i < times.length; i++) {
            if (i > 0 && times[i] <= times[i - 1]) throw new IllegalArgumentException("Timestamps must increase");
            float d = differences[i];
            if (!Float.isFinite(d)) { previous = 0; continue; }
            if (times[i] - previousSampleTime > 300) previous = 0;
            previousSampleTime = times[i];
            if (times[i] - previousSwitchTime > 1200) previous = 0;
            int state = d > .07f ? 1 : d < -.07f ? -1 : 0;
            if (state != 0 && state != previous) {
                long interval = times[i] - previousSwitchTime;
                if (previous == 0) {
                    previous = state;
                    previousSwitchTime = times[i];
                } else if (interval >= 180) {
                    events.add(times[i]);
                    previous = state;
                    previousSwitchTime = times[i];
                }
                // A transition faster than 180ms is noise. Keep the previous accepted phase.
            }
        }
        long[] result = new long[events.size()];
        for (int i = 0; i < result.length; i++) result[i] = events.get(i);
        return result;
    }

    /** Three consecutive valid samples, each no more than 300ms apart, form a sustained flag. */
    public static boolean[] sustained(long[] times, boolean[] valid, boolean[] flagged) {
        if (times.length != valid.length || times.length != flagged.length) throw new IllegalArgumentException("Signal lengths differ");
        boolean[] result = new boolean[times.length];
        int start = -1;
        for (int i = 0; i <= times.length; i++) {
            boolean present = i < times.length && valid[i] && flagged[i];
            boolean gap = i > 0 && i < times.length && times[i] - times[i - 1] > 300;
            if ((!present || gap) && start >= 0) {
                if (i - start >= 3) for (int j = start; j < i; j++) result[j] = true;
                start = -1;
            }
            if (present && start < 0) start = i;
        }
        return result;
    }

    /** -1 means unscorable, rather than zero. */
    public static int score(int total, int usable, int clean, long observedMs, int alternatingEvents) {
        if (total <= 0 || usable < 30 || clean < 0 || clean > usable || usable > total || observedMs < 4000 ||
                usable / (double) total < .5 || alternatingEvents < 6) return -1;
        return (int) Math.round(100.0 * clean / usable);
    }

    /** Median-like mean of plausible step intervals; -1 if insufficient or highly irregular. */
    public static int cadence(long[] events) {
        if (events.length < 6) return -1;
        List<Double> intervals = new ArrayList<>();
        for (int i = 1; i < events.length; i++) {
            double value = events[i] - events[i - 1];
            if (value >= 180 && value <= 1000) intervals.add(value);
        }
        if (intervals.size() < 5) return -1;
        double sum = 0;
        for (double value : intervals) sum += value;
        double mean = sum / intervals.size(), squared = 0;
        for (double value : intervals) squared += (value - mean) * (value - mean);
        if (Math.sqrt(squared / intervals.size()) / mean >= .4) return -1;
        int cadence = (int) Math.round(60000 / mean);
        return cadence >= 90 && cadence <= 240 ? cadence : -1;
    }
}
