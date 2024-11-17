package io.sphere.sdk.play.metrics;

import io.sphere.sdk.http.HttpClient;
import io.sphere.sdk.http.HttpRequest;
import io.sphere.sdk.http.HttpResponse;
import play.mvc.Http;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class MetricHttpClient implements HttpClient {
    private final HttpClient underlying;

    private MetricHttpClient(final HttpClient underlying) {
        this.underlying = underlying;
    }

    @Override
    public CompletionStage<HttpResponse> execute(final HttpRequest httpRequest) {
        final Optional<Http.Context> contextOptional = Optional.ofNullable(Http.Context.current.get());
        final CompletionStage<HttpResponse> result = underlying.execute(httpRequest);
        final long startTimestamp = System.currentTimeMillis();
        contextOptional.ifPresent(context ->
                result.thenAccept(response -> {
                    final long stopTimestamp = System.currentTimeMillis();
                    report(context, httpRequest, response, startTimestamp, stopTimestamp);
                }));
        return result;
    }

    @SuppressWarnings("unchecked")
    private void report(final Http.Context context, final HttpRequest httpRequest, final HttpResponse response, long startTimestamp, long stopTimestamp) {
        final Optional<List<ReportRawData>> dataOptional = Optional.ofNullable((List<ReportRawData>) context.args.get(MetricAction.KEY));
        dataOptional.ifPresent(data -> data.add(new ReportRawData(httpRequest, response, startTimestamp, stopTimestamp)));
    }

    @Override
    public void close() {
        underlying.close();
    }

    public static MetricHttpClient of(final HttpClient underlying) {
        return new MetricHttpClient(underlying);
    }
}