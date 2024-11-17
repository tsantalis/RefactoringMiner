package org.bonitasoft.engine.tracking;

import java.util.List;

/**
 * @author Charles Souillard
 */
public class FlushResult {

    private final long flushTime;

    private final List<FlushEventListenerResult> flushEventListenerResults;


    public FlushResult(long flushTime, List<FlushEventListenerResult> flushEventListenerResults) {
        this.flushTime = flushTime;
        this.flushEventListenerResults = flushEventListenerResults;
    }

    public List<FlushEventListenerResult> getFlushEventListenerResults() {
        return flushEventListenerResults;
    }

    public long getFlushTime() {
        return flushTime;
    }
}
