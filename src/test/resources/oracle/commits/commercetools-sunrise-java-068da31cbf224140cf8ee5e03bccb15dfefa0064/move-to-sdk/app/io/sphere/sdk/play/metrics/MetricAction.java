package io.sphere.sdk.play.metrics;

import io.sphere.sdk.http.HttpMethod;
import io.sphere.sdk.utils.SphereInternalLogger;
import org.apache.commons.lang3.tuple.Pair;
import play.Configuration;
import play.libs.concurrent.HttpExecution;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public class MetricAction extends play.mvc.Action.Simple {
    public static final String CONFIG_METRICS_ENABLED = "application.metrics.enabled";
    public static final String KEY = "io.sphere.sdk.play.metrics.reportRawData";
    private static final SphereInternalLogger LOGGER = SphereInternalLogger.getLogger("metrics.simple");

    private final boolean metricsEnabled;

    @Inject
    public MetricAction(final Configuration configuration) {
        this.metricsEnabled = configuration.getBoolean(CONFIG_METRICS_ENABLED);
    }

    @Override
    public CompletionStage<Result> call(final Http.Context ctx) {
        if (metricsEnabled) {
            final List<ReportRawData> rawData = Collections.synchronizedList(new LinkedList<>());
            ctx.args.put(KEY, rawData);
            return delegate.call(ctx)
                    .whenCompleteAsync((r, t) -> logRequestData(ctx, rawData), HttpExecution.defaultContext());
        } else {
            return delegate.call(ctx);
        }
    }

    private void logRequestData(final Http.Context ctx, final List<ReportRawData> rawDatas) {
        final Pair<List<ReportRawData>, List<ReportRawData>> queryCommandPair = splitByQueriesAndCommands(rawDatas);
        final List<ReportRawData> queries = queryCommandPair.getLeft();
        final List<ReportRawData> commands = queryCommandPair.getRight();
        final int size = calculateTotalSize(rawDatas);
        final String durations = rawDatas.stream().map(data -> data.getStopTimestamp() - data.getStartTimestamp()).map(l -> Long.toString(l) + " ms").collect(joining(", "));
        LOGGER.debug(() -> format("%s used %d requests (%d queries, %d commands, %d bytes fetched, in (%s)).", ctx.request(), rawDatas.size(), queries.size(), commands.size(), size, durations));
    }

    private Pair<List<ReportRawData>, List<ReportRawData>> splitByQueriesAndCommands(final List<ReportRawData> rawData) {
        return partition(rawData, data -> data.getHttpRequest().getHttpMethod() == HttpMethod.GET);
    }

    private Integer calculateTotalSize(final List<ReportRawData> rawData) {
        return rawData.stream().map(elem -> Optional.ofNullable(elem.getHttpResponse().getResponseBody())
                .map(b -> b.length).orElse(0)).reduce(0, (a, b) -> a + b);
    }

    /**
     * Partitions <code>list</code> in two lists according to <code>predicate</code>.
     * @param list the list which should be divided
     * @param predicate returns true if the element of <code>list</code> should belong to the first result list
     * @param <T> generic type of the list
     * @return the first list satisfies <code>predicate</code>, the second one not.
     */
    public static <T> Pair<List<T>, List<T>> partition(final List<T> list, final Predicate<T> predicate) {
        final List<T> matchingPredicate = new ArrayList<>();
        final List<T> notMatchingPredicate = new ArrayList<>();
        for (final T element : list) {
            if (predicate.test(element)) {
                matchingPredicate.add(element);
            } else {
                notMatchingPredicate.add(element);
            }
        }
        return Pair.of(matchingPredicate, notMatchingPredicate);
    }
}