package de.energiequant.vatsim.compatibility.legacyproxy.fetching;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodically calls {@link #onPeriodicWakeup()} which can decide the delay to
 * next execution. If an {@link Exception} occurs, execution will be retried
 * after {@link #DEFAULT_SLEEP_DURATION}. It is recommended to call
 * {@link #start()} instead of spawning the {@link Thread} directly.
 * {@link #stop()} attempts to shutdown the thread as soon as possible.
 */
public abstract class PeriodicRunnable implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicRunnable.class);

    private final AtomicReference<Instant> earliestWakeupTime = new AtomicReference<Instant>(Instant.MIN);
    private final AtomicBoolean shouldStop = new AtomicBoolean();
    private final AtomicReference<Thread> thread = new AtomicReference<Thread>();
    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    private static final Duration DEFAULT_SLEEP_DURATION = Duration.ofMinutes(1);
    private static final Duration MINIMUM_SLEEP_DURATION = Duration.ofSeconds(30);

    /**
     * Called initially when the {@link Thread} is started and gets repeatedly
     * called after the {@link Duration} returned be previous execution has passed.
     * Will also be called {@link #DEFAULT_SLEEP_DURATION} after last call if any
     * {@link Exception} occurred.
     *
     * @return {@link Duration} to sleep until next call; must be at least {@link #MINIMUM_SLEEP_DURATION}
     */
    protected abstract Duration onPeriodicWakeup();

    @Override
    public void run() {
        isStarted.set(true);
        thread.set(Thread.currentThread());

        while (!shouldStop.get()) {
            long secondsToWakeupTime = earliestWakeupTime.get().getEpochSecond() - Instant.now().getEpochSecond();
            if (secondsToWakeupTime <= 0) {
                Duration nextSleepDuration = DEFAULT_SLEEP_DURATION;
                try {
                    nextSleepDuration = onPeriodicWakeup();
                } catch (Exception ex) {
                    LOGGER.warn("Caught exception during periodic wakeup, retrying in {}", DEFAULT_SLEEP_DURATION, ex);
                }

                if (MINIMUM_SLEEP_DURATION.compareTo(nextSleepDuration) > 0) {
                    LOGGER.warn("Requested sleep duration is too small ({}), limiting to minimum of {}",
                                nextSleepDuration, MINIMUM_SLEEP_DURATION
                    );
                    nextSleepDuration = MINIMUM_SLEEP_DURATION;
                } else {
                    LOGGER.debug("Sleeping for {}", nextSleepDuration);
                }

                earliestWakeupTime.set(Instant.now().plus(nextSleepDuration));
            } else {
                try {
                    Thread.sleep(secondsToWakeupTime);
                } catch (InterruptedException ex) {
                    // ignore, interruptions are expected
                }
            }
        }

        isStarted.set(false);
        shouldStop.set(false);
    }

    /**
     * Spawns a new periodically repeating {@link Thread}.
     */
    public void start() {
        if (shouldStop.get()) {
            LOGGER.warn("Previous thread is shutting down, cannot start again before completed.");
            return;
        }

        if (!isStarted.getAndSet(true)) {
            LOGGER.debug("Starting new thread");
            new Thread(this).start();
        } else {
            LOGGER.warn("Thread has already been started");
        }
    }

    /**
     * Attempts to stop the {@link Thread}.
     */
    public void stop() {
        if (!isStarted.get()) {
            LOGGER.debug("Not started");
            return;
        } else if (shouldStop.get()) {
            LOGGER.debug("Already stopping");
            return;
        }

        LOGGER.debug("Stopping thread");

        shouldStop.set(true);

        Thread runningThread = thread.get();
        if (runningThread != null) {
            runningThread.interrupt();
        }
    }

    /**
     * Checks if the runner is running and not instructed to shut down.
     *
     * @return <code>true</code> if running and not shutting down,
     *     <code>false</code> if already stopped or shutdown is in progress
     */
    public boolean isAlive() {
        Thread currentThread = thread.get();
        return isStarted.get() && !shouldStop.get() && currentThread.isAlive();
    }
}
