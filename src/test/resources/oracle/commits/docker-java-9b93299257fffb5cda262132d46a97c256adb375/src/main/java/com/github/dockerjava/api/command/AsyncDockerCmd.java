/*
 * Created on 17.06.2015
 */
package com.github.dockerjava.api.command;

import com.github.dockerjava.api.async.ResultCallback;

/**
 *
 *
 * @author marcus
 *
 */
public interface AsyncDockerCmd<CMD_T extends AsyncDockerCmd<CMD_T, A_RES_T>, A_RES_T> extends DockerCmd<Void> {


    public <T extends ResultCallback<A_RES_T>> T exec(T resultCallback);

}
