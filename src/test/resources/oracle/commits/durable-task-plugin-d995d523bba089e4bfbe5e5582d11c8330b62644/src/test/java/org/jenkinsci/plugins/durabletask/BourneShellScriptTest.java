/*
 * The MIT License
 *
 * Copyright 2015 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.durabletask;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.Slave;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.remoting.VirtualChannel;
import hudson.slaves.DumbSlave;
import hudson.slaves.OfflineCause;
import hudson.tasks.Shell;
import hudson.util.StreamTaskListener;
import hudson.util.VersionNumber;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import static org.hamcrest.Matchers.*;
import org.jenkinsci.test.acceptance.docker.Docker;
import org.jenkinsci.test.acceptance.docker.DockerContainer;
import org.jenkinsci.test.acceptance.docker.DockerRule;
import org.jenkinsci.test.acceptance.docker.fixtures.JavaContainer;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;
import org.jvnet.hudson.test.SimpleCommandLauncher;

public class BourneShellScriptTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    @Rule public DockerRule<JavaContainer> dockerUbuntu = new DockerRule<>(JavaContainer.class);

    @Rule public DockerRule<CentOSFixture> dockerCentOS = new DockerRule<>(CentOSFixture.class);

    @Rule public DockerRule<AlpineFixture> dockerAlpine = new DockerRule<>(AlpineFixture.class);

    @Rule public DockerRule<SlimFixture> dockerSlim = new DockerRule<>(SlimFixture.class);

    @BeforeClass public static void unixAndDocker() throws Exception {
        assumeTrue("This test is only for Unix", File.pathSeparatorChar==':');
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        assumeThat("`docker version` could be run", new Launcher.LocalLauncher(StreamTaskListener.fromStderr()).launch().cmds("docker", "version", "--format", "{{.Client.Version}}").stdout(new TeeOutputStream(baos, System.err)).stderr(System.err).join(), is(0));
        assumeThat("Docker must be at least 1.13.0 for this test (uses --init)", new VersionNumber(baos.toString().trim()), greaterThanOrEqualTo(new VersionNumber("1.13.0")));
    }

    @Rule public LoggerRule logging = new LoggerRule().recordPackage(BourneShellScript.class, Level.FINE);

    private StreamTaskListener listener;
    private FilePath ws;
    private Launcher launcher;

    @Before public void vars() {
        listener = StreamTaskListener.fromStdout();
        ws = j.jenkins.getRootPath().child("ws");
        launcher = j.jenkins.createLauncher(listener);
    }

    @Test
    public void smokeTest() throws Exception {
        Controller c = new BourneShellScript("echo hello world").launch(new EnvVars(), ws, launcher, listener);
        awaitCompletion(c);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        c.writeLog(ws,baos);
        assertEquals(0, c.exitStatus(ws, launcher, listener).intValue());
        assertTrue(baos.toString().contains("hello world"));
        c.cleanup(ws);
    }

    @Test public void stop() throws Exception {
        // Have observed both SIGTERM and SIGCHLD, perhaps depending on which process (the written sh, or sleep) gets the signal first.
        // TODO without the `trap … EXIT` the other handlers do not seem to get run, and we get exit code 143 (~ uncaught SIGTERM). Why?
        // Also on jenkins.ci neither signal trap is encountered, only EXIT.
        Controller c = new BourneShellScript("trap 'echo got SIGCHLD' CHLD; trap 'echo got SIGTERM' TERM; trap 'echo exiting; exit 99' EXIT; sleep 999").launch(new EnvVars(), ws, launcher, listener);
        Thread.sleep(1000);
        c.stop(ws, launcher);
        awaitCompletion(c);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        c.writeLog(ws, baos);
        String log = baos.toString();
        System.out.println(log);
        assertEquals(99, c.exitStatus(ws, launcher, listener).intValue());
        assertTrue(log.contains("sleep 999"));
        assertTrue(log.contains("got SIG"));
        c.cleanup(ws);
    }

    @Test public void reboot() throws Exception {
        int orig = BourneShellScript.HEARTBEAT_CHECK_INTERVAL;
        BourneShellScript.HEARTBEAT_CHECK_INTERVAL = 15;
        try {
        FileMonitoringTask.FileMonitoringController c = (FileMonitoringTask.FileMonitoringController) new BourneShellScript("sleep 999").launch(new EnvVars("killemall", "true"), ws, launcher, listener);
        Thread.sleep(1000);
        launcher.kill(Collections.singletonMap("killemall", "true"));
        c.getResultFile(ws).delete();
        awaitCompletion(c);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        c.writeLog(ws, baos);
        String log = baos.toString();
        System.out.println(log);
        assertEquals(Integer.valueOf(-1), c.exitStatus(ws, launcher, listener));
        assertTrue(log.contains("sleep 999"));
        c.cleanup(ws);
        } finally {
            BourneShellScript.HEARTBEAT_CHECK_INTERVAL = orig;
        }
    }

    @Test public void justSlow() throws Exception {
        Controller c = new BourneShellScript("sleep 60").launch(new EnvVars(), ws, launcher, listener);
        awaitCompletion(c);
        c.writeLog(ws, System.out);
        assertEquals(0, c.exitStatus(ws, launcher, listener).intValue());
        c.cleanup(ws);
    }

    @Issue("JENKINS-27152")
    @Test public void cleanWorkspace() throws Exception {
        Controller c = new BourneShellScript("touch stuff && echo ---`ls -1a`---").launch(new EnvVars(), ws, launcher, listener);
        awaitCompletion(c);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        c.writeLog(ws, baos);
        assertEquals(0, c.exitStatus(ws, launcher, listener).intValue());
        assertThat(baos.toString(), containsString("---. .. stuff---"));
        c.cleanup(ws);
    }

    @Issue("JENKINS-26133")
    @Test public void output() throws Exception {
        DurableTask task = new BourneShellScript("echo 42");
        task.captureOutput();
        Controller c = task.launch(new EnvVars(), ws, launcher, listener);
        awaitCompletion(c);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        c.writeLog(ws, baos);
        assertEquals(0, c.exitStatus(ws, launcher, listener).intValue());
        assertThat(baos.toString(), containsString("+ echo 42"));
        assertEquals("42\n", new String(c.getOutput(ws, launcher)));
        c.cleanup(ws);
    }

    @Issue("JENKINS-38381")
    @Test public void watch() throws Exception {
        Slave s = j.createOnlineSlave();
        ws = s.getWorkspaceRoot();
        launcher = s.createLauncher(listener);
        DurableTask task = new BourneShellScript("set +x; for x in 1 2 3 4 5; do echo $x; sleep 1; done");
        Controller c = task.launch(new EnvVars(), ws, launcher, listener);
        BlockingQueue<Integer> status = new LinkedBlockingQueue<>();
        BlockingQueue<String> output = new LinkedBlockingQueue<>();
        BlockingQueue<String> lines = new LinkedBlockingQueue<>();
        c.watch(ws, new MockHandler(s.getChannel(), status, output, lines), listener);
        assertEquals("+ set +x", lines.take());
        assertEquals(0, status.take().intValue());
        assertEquals("<no output>", output.take());
        assertEquals("[1, 2, 3, 4, 5]", lines.toString());
        task = new BourneShellScript("echo result");
        task.captureOutput();
        c = task.launch(new EnvVars(), ws, launcher, listener);
        status = new LinkedBlockingQueue<>();
        output = new LinkedBlockingQueue<>();
        lines = new LinkedBlockingQueue<>();
        c.watch(ws, new MockHandler(s.getChannel(), status, output, lines), listener);
        assertEquals(0, status.take().intValue());
        assertEquals("result\n", output.take());
        assertEquals("[+ echo result]", lines.toString());
    }
    static class MockHandler extends Handler {
        final BlockingQueue<Integer> status;
        final BlockingQueue<String> output;
        final BlockingQueue<String> lines;
        @SuppressWarnings("unchecked")
        MockHandler(VirtualChannel channel, BlockingQueue<Integer> status, BlockingQueue<String> output, BlockingQueue<String> lines) {
            this.status = channel.export(BlockingQueue.class, status);
            this.output = channel.export(BlockingQueue.class, output);
            this.lines = channel.export(BlockingQueue.class, lines);
        }
        @Override public void output(InputStream stream) throws Exception {
            lines.addAll(IOUtils.readLines(stream, StandardCharsets.UTF_8));
        }
        @Override public void exited(int code, byte[] data) throws Exception {
            status.add(code);
            output.add(data != null ? new String(data, StandardCharsets.UTF_8) : "<no output>");
        }
    }

    @Issue("JENKINS-40734")
    @Test public void envWithShellChar() throws Exception {
        Controller c = new BourneShellScript("echo \"value=$MYNEWVAR\"").launch(new EnvVars("MYNEWVAR", "foo$$bar"), ws, launcher, listener);
        awaitCompletion(c);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        c.writeLog(ws,baos);
        assertEquals(0, c.exitStatus(ws, launcher, listener).intValue());
        assertThat(baos.toString(), containsString("value=foo$$bar"));
        c.cleanup(ws);
    }

    @Test public void shebang() throws Exception {
        setGlobalInterpreter("/bin/false"); // Should be overridden
        Controller c = new BourneShellScript("#!/bin/cat\nHello, world!").launch(new EnvVars(), ws, launcher, listener);
        awaitCompletion(c);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        c.writeLog(ws, new TeeOutputStream(baos, System.out));
        assertEquals(0, c.exitStatus(ws, launcher, listener).intValue());
        assertThat(baos.toString(), containsString("Hello, world!"));
        c.cleanup(ws);
    }

    @Issue("JENKINS-50902")
    @Test public void configuredInterpreter() throws Exception {
        // Run script inside the container as this is system dependent
        DumbSlave node = createDockerSlave(dockerSlim.get(), SlimFixture.SLIM_JAVA_LOCATION);
        j.jenkins.addNode(node);
        j.waitOnline(node);
        launcher = node.createLauncher(listener);
        ws = node.getWorkspaceRoot().child("configuredInterpreter");
        ws.mkdirs();

        setGlobalInterpreter("/bin/bash");
        String script = "if [ ! -z \"$BASH_VERSION\" ]; then echo 'this is bash'; else echo 'this is not'; fi";
        Controller c = new BourneShellScript(script).launch(new EnvVars(), ws, launcher, listener);
        awaitCompletion(c);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        c.writeLog(ws, new TeeOutputStream(baos, System.out));
        assertEquals(0, c.exitStatus(ws, launcher, listener).intValue());
        assertThat(baos.toString(), containsString("this is bash"));
        c.cleanup(ws);

        // Find it in the PATH
        setGlobalInterpreter("bash");
        c = new BourneShellScript(script).launch(new EnvVars(), ws, launcher, listener);
        awaitCompletion(c);
        baos = new ByteArrayOutputStream();
        c.writeLog(ws, new TeeOutputStream(baos, System.out));
        assertEquals(0, c.exitStatus(ws, launcher, listener).intValue());
        assertThat(baos.toString(), containsString("this is bash"));
        c.cleanup(ws);

        setGlobalInterpreter("no_such_shell");
        c = new BourneShellScript(script).launch(new EnvVars(), ws, launcher, listener);
        awaitCompletion(c);
        baos = new ByteArrayOutputStream();
        c.writeLog(ws, new TeeOutputStream(baos, System.out));
        assertNotEquals(0, c.exitStatus(ws, launcher, listener).intValue());
        assertThat(baos.toString(), containsString("no_such_shell"));
        c.cleanup(ws);
    }

    private void setGlobalInterpreter(String interpreter) {
        j.jenkins.getInjector().getInstance(Shell.DescriptorImpl.class).setShell(interpreter);
    }

    private void awaitCompletion(Controller c) throws IOException, InterruptedException {
        while (c.exitStatus(ws, launcher, listener) == null) {
            Thread.sleep(100);
        }
    }

    @Test public void runOnUbuntuDocker() throws Exception {
        JavaContainer container = dockerUbuntu.get();
        runOnDocker(createDockerSlave(container, null));
    }

    @Test public void runOnCentOSDocker() throws Exception {
        CentOSFixture container = dockerCentOS.get();
        runOnDocker(createDockerSlave(container, null));
    }

    @Issue("JENKINS-52847")
    @Test public void runOnAlpineDocker() throws Exception {
        AlpineFixture container = dockerAlpine.get();
        runOnDocker(createDockerSlave(container, null), 45);
    }

    @Issue("JENKINS-52881")
    @Test public void runOnSlimDocker() throws Exception {
        SlimFixture container = dockerSlim.get();
        runOnDocker(createDockerSlave(container, SlimFixture.SLIM_JAVA_LOCATION), 45);
    }

    private DumbSlave createDockerSlave(DockerContainer container, String javaPath) throws hudson.model.Descriptor.FormException, IOException {
        return new DumbSlave("docker", "/home/test", new SSHLauncher(container.ipBound(22), container.port(22), "test", "test", "", "", javaPath, null, null));
    }

    private void runOnDocker(DumbSlave s) throws Exception {
        runOnDocker(s, 10);
    }

    private void runOnDocker(DumbSlave s, int sleepSeconds) throws Exception {
        j.jenkins.addNode(s);
        j.waitOnline(s);
        FilePath dockerWS = s.getWorkspaceRoot();
        Launcher dockerLauncher = s.createLauncher(listener);
        final AtomicReference<Proc> proc = new AtomicReference<>();
        Launcher decorated = new Launcher.DecoratedLauncher(dockerLauncher) {
            @Override public Proc launch(Launcher.ProcStarter starter) throws IOException {
                Proc delegate = super.launch(starter);
                assertTrue(proc.compareAndSet(null, delegate));
                return delegate;
            }
        };
        String script = String.format("echo hello world; sleep 5; echo long since started; sleep %s", sleepSeconds - 5);
        ByteArrayOutputStream baos;
        Controller c = new BourneShellScript(script).launch(new EnvVars(), dockerWS, decorated, listener);
        baos = new ByteArrayOutputStream();
        while (c.exitStatus(dockerWS, dockerLauncher, listener) == null) {
            c.writeLog(dockerWS, baos);
            if (baos.toString().contains("long since started")) {
                assertNotNull(proc.get());
                assertFalse("JENKINS-58290: wrapper process still running:\n" + baos, proc.get().isAlive());
            }
            Thread.sleep(100);
        }
        assertEquals(0, c.exitStatus(dockerWS, dockerLauncher, listener).intValue());
        assertTrue(baos.toString().contains("hello world"));
        c.cleanup(dockerWS);
        do {
            Thread.sleep(1000);
            baos = new ByteArrayOutputStream();
            try {
                assertEquals(0, dockerLauncher.launch().cmds("ps", "-e", "-o", "pid,stat,comm").stdout(new TeeOutputStream(baos, System.out)).join());
            } catch (IOException x) { // no ps? forget this check
                System.err.println(x);
                break;
            }
        } while (baos.toString().contains(" sleep "));
        assertThat("no zombies running", baos.toString(), not(containsString(" Z ")));
        s.toComputer().disconnect(new OfflineCause.UserCause(null, null));
    }

    @Test public void runWithCommandLauncher() throws Exception {
        assumeTrue("Docker required for this test", new Docker().isAvailable());
        runOnDocker(new DumbSlave("docker", "/home/jenkins/agent", new SimpleCommandLauncher("docker run -i --rm --name agent jenkinsci/slave:3.7-1 java -jar /usr/share/jenkins/slave.jar")));
    }

    @Test public void runWithTiniCommandLauncher() throws Exception {
        assumeTrue("Docker required for this test", new Docker().isAvailable());
        runOnDocker(new DumbSlave("docker", "/home/jenkins/agent", new SimpleCommandLauncher("docker run -i --rm --name agent --init jenkinsci/slave:3.7-1 java -jar /usr/share/jenkins/slave.jar")));
    }

}
