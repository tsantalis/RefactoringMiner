package com.github.dockerjava.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.api.model.VolumeBind;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.TestDockerCmdExecFactory;
import com.github.dockerjava.core.async.ResultCallbackTemplate;
import com.google.common.base.Joiner;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDockerClientTest extends Assert {

    public static final Logger LOG = LoggerFactory.getLogger(AbstractDockerClientTest.class);

    protected DockerClient dockerClient;

    protected TestDockerCmdExecFactory dockerCmdExecFactory = new TestDockerCmdExecFactory(
            DockerClientBuilder.getDefaultDockerCmdExecFactory());

    public void beforeTest() throws Exception {

        LOG.info("======================= BEFORETEST =======================");
        LOG.info("Connecting to Docker server");
        dockerClient = DockerClientBuilder.getInstance(config()).withDockerCmdExecFactory(dockerCmdExecFactory).build();

        LOG.info("Pulling image 'busybox'");

        PullResponseCallback callback = new PullResponseCallback();
        // need to block until image is pulled completely
        dockerClient.pullImageCmd("busybox", callback).withTag("latest").exec();
        callback.awaitFinish();

        assertNotNull(dockerClient);
        LOG.info("======================= END OF BEFORETEST =======================\n\n");
    }

    private DockerClientConfig config() {
        return config(null);
    }

    protected DockerClientConfig config(String password) {
        DockerClientConfig.DockerClientConfigBuilder builder = DockerClientConfig.createDefaultConfigBuilder()
                .withServerAddress("https://index.docker.io/v1/");
        if (password != null) {
            builder = builder.withPassword(password);
        }
        return builder.build();
    }

    public void afterTest() {
        LOG.info("======================= END OF AFTERTEST =======================");
    }

    public void beforeMethod(Method method) {
        LOG.info(String.format("################################## STARTING %s ##################################",
                method.getName()));
    }

    public void afterMethod(ITestResult result) {

        for (String container : dockerCmdExecFactory.getContainerNames()) {
            LOG.info("Cleaning up temporary container {}", container);

            try {
                dockerClient.removeContainerCmd(container).withForce().exec();
            } catch (DockerException ignore) {
                // ignore.printStackTrace();
            }
        }

        for (String image : dockerCmdExecFactory.getImageNames()) {
            LOG.info("Cleaning up temporary image with {}", image);
            try {
                dockerClient.removeImageCmd(image).withForce().exec();
            } catch (DockerException ignore) {
                // ignore.printStackTrace();
            }
        }

        LOG.info("################################## END OF {} ##################################\n", result.getName());
    }

    protected String asString(InputStream response) {
        return consumeAsString(response);
    }

    public static String consumeAsString(InputStream response) {

        StringWriter logwriter = new StringWriter();

        try {
            LineIterator itr = IOUtils.lineIterator(response, "UTF-8");

            while (itr.hasNext()) {
                String line = itr.next();
                logwriter.write(line + (itr.hasNext() ? "\n" : ""));
                LOG.info("line: " + line);
            }
            response.close();

            return logwriter.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(response);
        }
    }

    // UTIL

    /**
     * Checks to see if a specific port is available.
     *
     * @param port
     *            the port to check for availability
     */
    public static boolean available(int port) {
        if (port < 1100 || port > 60000) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException ignored) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }

    /**
     * Asserts that {@link InspectContainerResponse#getVolumes()} (<code>.Volumes</code>) has {@link VolumeBind}s for
     * the given {@link Volume}s
     */
    public static void assertContainerHasVolumes(InspectContainerResponse inspectContainerResponse,
            Volume... expectedVolumes) {
        VolumeBind[] volumeBinds = inspectContainerResponse.getVolumes();
        LOG.info("Inspect .Volumes = [{}]", Joiner.on(", ").join(volumeBinds));

        List<Volume> volumes = new ArrayList<Volume>();
        for (VolumeBind bind : volumeBinds) {
            volumes.add(new Volume(bind.getContainerPath()));
        }
        assertThat(volumes, contains(expectedVolumes));
    }

    public static class CollectFramesCallback extends CollectStreamItemCallback<Frame> {

        @Override
        public void onNext(Frame frame) {
            items.add(frame);
            log.append(new String(frame.getPayload()));
        }

    }

    protected String containerLog(String containerId) throws Exception {

        CollectFramesCallback collectFramesCallback = new CollectFramesCallback();

        dockerClient.logContainerCmd(containerId, collectFramesCallback).withStdOut().exec();

        collectFramesCallback.awaitFinish();

        return collectFramesCallback.toString();
    }


    public static class CollectStreamItemCallback<T> extends ResultCallbackTemplate<T> {
        public final List<T> items = new ArrayList<T>();

        protected final StringBuffer log = new StringBuffer();

        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
            super.onError(throwable);
        }

        @Override
        public void onNext(T item) {
            items.add(item);
            log.append("" + item);
            LOG.info(item.toString());
        }

        @Override
        public String toString() {
            return log.toString();
        }
    }

    public static class BuildLogCallback extends CollectStreamItemCallback<BuildResponseItem> {
        public String awaitImageId() throws Exception {
            awaitFinish();
            BuildResponseItem item = items.get(items.size() - 1);
            assertThat(item.toString(), containsString("Successfully built"));
            return item.getStream().replaceFirst("Successfully built", "").trim();
        }
    }

    public static class PullResponseCallback extends CollectStreamItemCallback<PullResponseItem> {

    }

    protected String buildImage(File baseDir) throws Exception {

        BuildLogCallback callback = new BuildLogCallback();

        dockerClient.buildImageCmd(baseDir, callback).withNoCache().exec();

        return callback.awaitImageId();
    }
}
