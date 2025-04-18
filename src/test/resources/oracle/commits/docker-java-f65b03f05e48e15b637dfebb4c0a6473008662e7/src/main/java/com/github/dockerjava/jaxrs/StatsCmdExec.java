package com.github.dockerjava.jaxrs;

import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.async.JsonStreamProcessor;
import com.github.dockerjava.jaxrs.async.AbstractCallbackNotifier;
import com.github.dockerjava.jaxrs.async.GETCallbackNotifier;

public class StatsCmdExec extends AbstrAsyncDockerCmdExec<StatsCmd, Statistics, Void> implements StatsCmd.Exec {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatsCmdExec.class);

    public StatsCmdExec(WebTarget baseResource) {
        super(baseResource);
    }

    @Override
    protected AbstractCallbackNotifier<Statistics> callbackNotifier(StatsCmd command,
            ResultCallback<Statistics> resultCallback) {

        WebTarget webTarget = getBaseResource().path("/containers/{id}/stats").resolveTemplate("id",
                command.getContainerId());

        LOGGER.trace("GET: {}", webTarget);

        return new GETCallbackNotifier<Statistics>(
                new JsonStreamProcessor<Statistics>(Statistics.class), command.getResultCallback(), webTarget.request());

    }
}
