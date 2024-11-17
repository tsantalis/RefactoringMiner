/**
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.tracking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.bonitasoft.engine.commons.TenantLifecycleService;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;

public class TimeTracker implements TenantLifecycleService {


    private final Set<String> activatedRecords;
    private final FlushThread flushThread;
    private final List<FlushEventListener> flushEventListeners;
    private final TechnicalLoggerService logger;
    private final Queue<Record> records;
    private final Clock clock;

    private long flushIntervalInMS;
    private boolean startTracking = false;

    private boolean serviceStarted;
    private long lastFlushTimestamp = 0L;


    public TimeTracker(
            final TechnicalLoggerService logger,
            final boolean startTracking,
            final List<FlushEventListener> flushEventListeners,
            final int maxSize,
            final int flushIntervalInSeconds,
            final String... activatedRecords) {
        this(logger, new ThreadSleepClockImpl(), startTracking, flushEventListeners, maxSize, flushIntervalInSeconds * 1000, activatedRecords);
    }

    public TimeTracker(
            final TechnicalLoggerService logger,
            final Clock clock,
            final boolean startTracking,
            final List<FlushEventListener> flushEventListeners,
            final int maxSize,
            final int flushIntervalInMS,
            final String... activatedRecords) {
        super();
        this.startTracking = startTracking;
        this.clock = clock;
        this.flushIntervalInMS = flushIntervalInMS;
        records = new CircularFifoQueue<>(maxSize);
        serviceStarted = false;
        this.logger = logger;
        this.flushEventListeners = flushEventListeners;
        if (activatedRecords == null || activatedRecords.length == 0) {
            this.activatedRecords = Collections.emptySet();
        } else {
            this.activatedRecords = new HashSet<>(Arrays.asList(activatedRecords));
        }
        flushThread = createFlushThread();
        if (logger.isLoggable(getClass(), TechnicalLogSeverity.INFO)) {
            this.logger.log(getClass(), TechnicalLogSeverity.INFO,
                    getStatus());
        }
    }

    public void addFlushEventListener(final FlushEventListener flushEventListener) {
        this.flushEventListeners.add(flushEventListener);
    }
    public void removeFlushEventListener(final FlushEventListener flushEventListener) {
        this.flushEventListeners.remove(flushEventListener);
    }
    public void addActivatedRecord(final String activatedRecord) {
        this.activatedRecords.add(activatedRecord);
    }

    public void removeActivatedRecord(final String activatedRecord) {
        this.activatedRecords.remove(activatedRecord);
    }

    public void startTracking() {
        if (!serviceStarted) {
            if (logger.isLoggable(getClass(), TechnicalLogSeverity.WARNING)) {
                this.logger.log(getClass(), TechnicalLogSeverity.WARNING,
                        "Cannot start Time tracker tracking because service is not started.");
            }
            return;
        }
        startTracking = true;
        startFlushThread();
    }

    public void stopTracking() {
        startTracking = false;
        stopFlushThread();
    }

    FlushThread createFlushThread() {
        return new FlushThread(this);
    }


    private void startFlushThread() {
        if (startTracking) {
            if (logger.isLoggable(getClass(), TechnicalLogSeverity.WARNING)) {
                this.logger.log(getClass(), TechnicalLogSeverity.WARNING,
                        "Starting Time tracker tracking...");
            }
            if (!flushThread.isStarted()) {
                flushThread.start();
            }
            if (logger.isLoggable(getClass(), TechnicalLogSeverity.WARNING)) {
                this.logger.log(getClass(), TechnicalLogSeverity.WARNING,
                        "Time tracker tracking is activated. This may not be used in production as performances may be strongly impacted.");
            }
        }
    }

    private void stopFlushThread() {
        if (isTracking()) {
            if (logger.isLoggable(getClass(), TechnicalLogSeverity.WARNING)) {
                this.logger.log(getClass(), TechnicalLogSeverity.WARNING,
                        "Stopping Time tracker tracking...");
            }
            if (flushThread.isStarted()) {
                flushThread.interrupt();
            }
            if (logger.isLoggable(getClass(), TechnicalLogSeverity.WARNING)) {
                this.logger.log(getClass(), TechnicalLogSeverity.WARNING,
                        "Time tracker tracking is deactivated.");
            }
        }
    }

    public boolean isTracking() {
        return flushThread.isStarted();
    }

    public long getFlushIntervalInMS() {
        return flushIntervalInMS;
    }

    public void setFlushIntervalInSeconds(long flushIntervalInSeconds) {
        this.flushIntervalInMS = flushIntervalInSeconds * 1000;
    }

    public void setFlushIntervalInMS(long flushIntervalInMS) {
        this.flushIntervalInMS = flushIntervalInMS;
    }

    public Clock getClock() {
        return clock;
    }

    public String getStatus() {
        final StringBuilder sb = new StringBuilder();
        sb.append("-----");
        sb.append("\n");

        sb.append("Time Tracker '");
        sb.append(this.getClass().getName());
        sb.append("':");
        sb.append("\n");

        sb.append("  - trackingEnabled: ");
        sb.append(isTracking());
        sb.append("\n");

        sb.append("  - flushIntervalInSeconds: ");
        sb.append(flushIntervalInMS);
        sb.append("\n");

        sb.append("  - activatedRecords: ");
        for (String activatedRecord : activatedRecords) {
            sb.append(activatedRecord);
            sb.append(" ");
        }
        sb.append("\n");

        sb.append("  - flushEventListeners: ");
        for (FlushEventListener flushEventListener : flushEventListeners) {
            sb.append(flushEventListener.getStatus());
            sb.append(" ");
        }
        sb.append("\n");

        sb.append("  - records.size: ");
        sb.append(records.size());
        sb.append("\n");

        sb.append("  - last flush occurrence: ");
        sb.append(new Date(lastFlushTimestamp).toString());
        sb.append("\n");

        sb.append("\n");
        sb.append("-----");
        return sb.toString();
    }

    public boolean isTrackable(final String recordName) {
        return isTracking() && activatedRecords.contains(recordName);
    }

    public TechnicalLoggerService getLogger() {
        return logger;
    }

    public void track(final String recordName, final String recordDescription, final long duration) {
        if (!isTrackable(recordName)) {
            return;
        }
        final long timestamp = System.currentTimeMillis();
        final Record record = new Record(timestamp, recordName, recordDescription, duration);
        log(TechnicalLogSeverity.DEBUG, "Tracking record: " + record);
        synchronized (this) {
            records.add(record);
        }
    }

    void log(TechnicalLogSeverity debug, String message) {
        if (logger.isLoggable(getClass(), debug)) {
            logger.log(getClass(), debug, message);
        }
    }

    public List<FlushResult> flush() {
        if (!isTracking()) {
            return Collections.emptyList();
        }
        log(TechnicalLogSeverity.INFO, "Flushing...");
        lastFlushTimestamp = System.currentTimeMillis();
        final List<Record> records;
        synchronized (this) {
            records = getRecordsCopy();
            clearRecords();
        }
        final FlushEvent flushEvent = new FlushEvent(records);
        final List<FlushResult> flushResults = new ArrayList<>();
        flushListeners(flushEvent, flushResults);
        log(TechnicalLogSeverity.INFO, "Flush finished: " + flushEvent);
        return flushResults;
    }

    void flushListeners(FlushEvent flushEvent, List<FlushResult> flushResults) {
        if (flushEventListeners == null) {
            return;
        }
        for (final FlushEventListener listener : flushEventListeners) {
            flushListener(flushEvent, flushResults, listener);
        }
    }

    void flushListener(FlushEvent flushEvent, List<FlushResult> flushResults, FlushEventListener listener) {
        try {
            flushResults.add(listener.flush(flushEvent));
        } catch (final Exception e) {
            log(TechnicalLogSeverity.WARNING, "Exception while flushing: " + flushEvent + " on listener " + listener);
        }
    }

    public List<Record> getRecordsCopy() {
        return Arrays.asList(records.toArray(new Record[records.size()]));
    }

    public void clearRecords() {
        records.clear();
    }

    @Override
    public void start() {
        if (serviceStarted) {
            return;
        }
        log(TechnicalLogSeverity.INFO, "Starting TimeTracker...");
        serviceStarted = true;
        startFlushThread();
        log(TechnicalLogSeverity.INFO, "TimeTracker started.");
    }


    @Override
    public void stop() {
        if (!serviceStarted) {
            return;
        }
        log(TechnicalLogSeverity.INFO, "Stopping TimeTracker...");
        serviceStarted = false;
        stopFlushThread();
        log(TechnicalLogSeverity.INFO, "TimeTracker stopped.");
    }

    @Override
    public void pause() {
        stop();
    }

    @Override
    public void resume() {
        start();
    }

}
