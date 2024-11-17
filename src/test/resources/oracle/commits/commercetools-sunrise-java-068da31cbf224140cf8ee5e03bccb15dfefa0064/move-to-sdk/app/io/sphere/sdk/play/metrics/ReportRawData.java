package io.sphere.sdk.play.metrics;

import io.sphere.sdk.http.HttpResponse;
import io.sphere.sdk.http.HttpRequest;

public class ReportRawData {
    private final HttpRequest HttpRequest;
    private final HttpResponse httpResponse;
    private final long startTimestamp;
    private final long stopTimestamp;

    public ReportRawData(final HttpRequest HttpRequest, final HttpResponse httpResponse, final long startTimestamp, final long stopTimestamp) {
        this.HttpRequest = HttpRequest;
        this.httpResponse = httpResponse;
        this.startTimestamp = startTimestamp;
        this.stopTimestamp = stopTimestamp;
    }

    public HttpRequest getHttpRequest() {
        return HttpRequest;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getStopTimestamp() {
        return stopTimestamp;
    }
}