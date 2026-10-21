package com.stride.form;

import java.util.Arrays;

/** No Android SDK required: runs the exact signal-processing class used in production. */
public final class GaitMathChecks {
    private static int checks = 0;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static void near(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < .001, message + ": " + actual);
    }
    public static void main(String[] args) {
        near(GaitMath.angle(0, 0, 0, 1, 1, 1), 90, "Right angle");
        near(GaitMath.angle(0, 0, 0, 1, 0, 2), 180, "Straight knee");
        near(GaitMath.angle(0, 0, 0, 0, 0, 0), 0, "Degenerate joint is finite");
        near(GaitMath.angle(2, 3, 2, 5, 4, 5), 90, "Translation and scale preserve angle");

        check(GaitMath.score(100, 100, 80, 9900, 20) == 80, "Percent uses usable clean frames");
        check(GaitMath.score(100, 50, 40, 9900, 20) == 80, "Untracked frames do not count as bad form");
        check(GaitMath.score(100, 49, 49, 9900, 20) == -1, "Low coverage withholds score");
        check(GaitMath.score(61, 30, 30, 9900, 20) == -1, "Rounded coverage cannot bypass minimum");
        check(GaitMath.score(100, 29, 29, 9900, 20) == -1, "Insufficient usable frames");
        check(GaitMath.score(100, 100, 100, 3999, 20) == -1, "Insufficient observed duration");
        check(GaitMath.score(100, 100, 100, 9900, 0) == -1, "Stationary person receives no form score");
        check(GaitMath.score(0, 0, 0, 0, 0) == -1, "Empty clip is unscorable");
        check(GaitMath.score(100, 100, 0, 9900, 20) == 0, "Zero is a real score, distinct from unavailable");
        check(GaitMath.score(100, 100, 101, 9900, 20) == -1, "Invalid counts rejected");

        boolean[] yes = {true, true, true, true, true, true};
        long[] times = {0, 100, 200, 300, 400, 500};
        check(Arrays.equals(GaitMath.sustained(times, yes, new boolean[]{false, true, true, false, false, false}), new boolean[6]), "Two noisy frames are suppressed");
        check(Arrays.equals(GaitMath.sustained(times, yes, new boolean[]{false, true, true, true, false, false}), new boolean[]{false, true, true, true, false, false}), "Three consecutive flags retained");
        check(Arrays.equals(GaitMath.sustained(new long[]{0,100,900}, new boolean[]{true,true,true}, new boolean[]{true,true,true}), new boolean[3]), "Long gap breaks sustained sequence");
        check(Arrays.equals(GaitMath.sustained(new long[]{0,100,200}, new boolean[]{true,false,true}, new boolean[]{true,true,true}), new boolean[3]), "Low visibility breaks sequence");
        check(Arrays.equals(GaitMath.sustained(new long[]{0,100,200}, new boolean[]{true,true,true}, new boolean[]{true,true,true}), new boolean[]{true,true,true}), "Final segment flushes at end of clip");
        check(GaitMath.sustained(new long[0], new boolean[0], new boolean[0]).length == 0, "Empty sustained signal");

        long[] runTimes = new long[150]; float[] runDiff = new float[150];
        for(int i=0;i<150;i++) { runTimes[i]=i*100L; runDiff[i]=((i/3)%2==0)?.2f:-.2f; }
        long[] events = GaitMath.stepEvents(runTimes, runDiff);
        check(events.length == 49, "Alternating leg events counted");
        check(GaitMath.cadence(events) == 200, "Step rate includes both feet");
        for(int i=0;i<150;i++) runDiff[i] *= -1;
        check(Arrays.equals(events, GaitMath.stepEvents(runTimes, runDiff)), "Left/right phase does not change cadence");
        for(int i=0;i<150;i++) runDiff[i] = (i%2==0)?.03f:-.03f;
        check(GaitMath.stepEvents(runTimes, runDiff).length == 0, "Small tracking jitter does not count as running");
        check(GaitMath.stepEvents(new long[]{0,100,2000,2100}, new float[]{.2f,.2f,-.2f,-.2f}).length == 0, "Tracking gap does not invent a step");
        check(GaitMath.stepEvents(new long[]{0,100,200,300}, new float[]{.2f,Float.NaN,-.2f,-.2f}).length == 0, "Missing signal resets phase");
        check(GaitMath.cadence(new long[]{0,300,600}) == -1, "Too few steps withholds cadence");
        check(GaitMath.cadence(new long[]{0,200,1200,1400,2400,2600,3600}) == -1, "Irregular intervals withhold cadence");
        check(GaitMath.cadence(new long[]{0,200,400,600,800,1000}) == -1, "Implausible step rate withheld");
        boolean rejected = false;
        try { GaitMath.stepEvents(new long[]{0,0}, new float[]{.2f,-.2f}); } catch(IllegalArgumentException e) { rejected=true; }
        check(rejected, "Non-monotonic samples rejected");
        System.out.println("PASS: " + checks + " production gait-math checks");
    }
}
