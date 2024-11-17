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

import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;

public class FlushThread extends Thread {

    private final TimeTracker timeTracker;

    private final TechnicalLoggerService logger;

    public FlushThread(final TimeTracker timeTracker) {
        super("Bonita-TimeTracker-FlushThread");
        this.logger = timeTracker.getLogger();
        this.timeTracker = timeTracker;
    }

    @Override
    public void run() {
        info("Starting " + this.getName() + "...");
        long lastFlushTimestamp = System.currentTimeMillis();
        while (true) {
            try {
                final long flushDuration = System.currentTimeMillis() - lastFlushTimestamp;
                final long sleepTime = this.timeTracker.getFlushIntervalInMS() - flushDuration;
                if (this.logger.isLoggable(getClass(), TechnicalLogSeverity.DEBUG)) {
                    this.logger.log(getClass(), TechnicalLogSeverity.DEBUG, "FlushThread: sleeping for: " + sleepTime + "ms");
                }
                this.timeTracker.getClock().sleep(sleepTime);
            } catch (InterruptedException e) {
                break;
            }
            try {
                final FlushResult flushResult = this.timeTracker.flush();
                lastFlushTimestamp = flushResult.getFlushTime();
            } catch (Exception e) {
                if (this.logger.isLoggable(getClass(), TechnicalLogSeverity.WARNING)) {
                    this.logger.log(getClass(), TechnicalLogSeverity.WARNING, "Exception caught while flushing: " + e.getMessage(), e);
                }
            }
        }
        info(this.getName() + " stopped.");
    }

    void info(String message) {
        if (this.logger.isLoggable(getClass(), TechnicalLogSeverity.INFO)) {
            logger.log(getClass(), TechnicalLogSeverity.INFO, message);
        }
    }

    public boolean isStarted(){
        return isAlive();
    }

}
