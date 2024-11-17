package com.commercetools.sunrise.common.ctp;

import io.sphere.sdk.http.HttpMethod;
import io.sphere.sdk.utils.SphereInternalLogger;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import play.mvc.Http;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public final class MetricAction {
    static final String KEY = "io.sphere.sdk.play.metrics.reportRawData";
    private static final SphereInternalLogger LOGGER = SphereInternalLogger.getLogger("metrics.simple");

    public static void logRequestData(final Http.Context ctx) {
        final List<ReportRawData> rawDatas = (List<ReportRawData>) ctx.args.get(KEY);
        final String durations = rawDatas.stream().map(data -> data.getStopTimestamp() - data.getStartTimestamp()).map(l -> Long.toString(l) + " ms").collect(joining(", "));
        if (LOGGER.isTraceEnabled()) {
            final String queriesAsString = rawDatas.stream().map(q -> {
                final String url = q.getHttpRequest().getUrl();
                final String[] split = StringUtils.split(url, "/", 4);
                final String shortUrl = split.length == 4 ? "/" + split[3] : url;
                final HttpMethod httpMethod = q.getHttpRequest().getHttpMethod();
                final long duration = q.getStopTimestamp() - q.getStartTimestamp();
                final Integer bodySize = getBodySize(q);
                return format("  %s %s %dms %dbytes", httpMethod, shortUrl, duration, bodySize);
            }).collect(joining("\n"));
            LOGGER.trace(() -> format("commercetools requests in %s: \n%s", ctx.request(), queriesAsString));
        } else {
            final Pair<List<ReportRawData>, List<ReportRawData>> queryCommandPair = splitByQueriesAndCommands(rawDatas);
            final List<ReportRawData> queries = queryCommandPair.getLeft();
            final List<ReportRawData> commands = queryCommandPair.getRight();
            final int size = calculateTotalSize(rawDatas);
            LOGGER.debug(() -> format("%s used %d requests (%d queries, %d commands, %dbytes fetched, in (%s)).", ctx.request(), rawDatas.size(), queries.size(), commands.size(), size, durations));
        }
    }

    private static Pair<List<ReportRawData>, List<ReportRawData>> splitByQueriesAndCommands(final List<ReportRawData> rawData) {
        return partition(rawData, data -> data.getHttpRequest().getHttpMethod() == HttpMethod.GET || data.getHttpRequest().getUrl().contains("/product-projections/search"));
    }

    private static Integer calculateTotalSize(final List<ReportRawData> rawData) {
        return rawData.stream().map(elem -> getBodySize(elem)).reduce(0, (a, b) -> a + b);
    }

    private static Integer getBodySize(final ReportRawData elem) {
        return Optional.ofNullable(elem.getHttpResponse().getResponseBody())
                .map(b -> b.length).orElse(0);
    }

    /**
     * Partitions <code>list</code> in two lists according to <code>predicate</code>.
     * @param list the list which should be divided
     * @param predicate returns true if the element of <code>list</code> should belong to the first result list
     * @param <T> generic type of the list
     * @return the first list satisfies <code>predicate</code>, the second one not.
     */
    private static <T> Pair<List<T>, List<T>> partition(final List<T> list, final Predicate<T> predicate) {
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