package pl.edu.agh.facelivenessdetection.processing;

import android.app.ActivityManager;
import android.os.SystemClock;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ProcessingMonitor {

    private static final long INTERVAL_MS = 1000;

    private final Timer fpsTimer;

    private final ActivityManager activityManager;

    private final AtomicInteger fpsRate;

    private final AtomicInteger framesProcessedInSingleInterval;

    private final AtomicLong startMs;

    private final AtomicLong numRuns;

    private final AtomicLong totalRunMs;

    public ProcessingMonitor(ActivityManager activityManager) {
        this.fpsTimer = new Timer();
        this.activityManager = activityManager;

        this.fpsRate = new AtomicInteger(0);
        this.framesProcessedInSingleInterval = new AtomicInteger(0);
        this.startMs = new AtomicLong(0);
        this.numRuns = new AtomicLong(0);
        this.totalRunMs = new AtomicLong(0);
    }

    public void startTimer() {
        fpsTimer.scheduleAtFixedRate(
                new TimerTask() {
                    @Override
                    public void run() {
                        fpsRate.set(framesProcessedInSingleInterval.intValue());
                        framesProcessedInSingleInterval.set(0);
                    }
                }, 0, INTERVAL_MS);
    }

    public void notifyDetectionStart() {
        startMs.set(SystemClock.elapsedRealtime());
    }

    public void notifyDetectionCompleted() {
        final long currentLatencyMs = SystemClock.elapsedRealtime() - startMs.get();
        numRuns.incrementAndGet();
        framesProcessedInSingleInterval.incrementAndGet();
        totalRunMs.addAndGet(currentLatencyMs);
    }

    public int getFPSRate() {
        return fpsRate.get();
    }

    public long getAverageLatency() {
        final long runs = numRuns.get();
        if (runs == 0) {
            return 0;
        }
        return totalRunMs.get() / numRuns.get();
    }

    public long getAvailableMegs() {
        final ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(mi);
        return mi.availMem / 0x100000L;
    }

    public void stopTimer() {
        fpsTimer.cancel();
        fpsRate.set(0);
        framesProcessedInSingleInterval.set(0);
        totalRunMs.set(0);
        numRuns.set(0);
    }
}
