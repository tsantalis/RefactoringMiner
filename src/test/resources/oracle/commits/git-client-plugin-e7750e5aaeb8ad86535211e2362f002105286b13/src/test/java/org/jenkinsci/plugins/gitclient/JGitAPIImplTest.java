package org.jenkinsci.plugins.gitclient;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class JGitAPIImplTest extends GitAPITestCase {
    @Override
    protected GitClient setupGitAPI(File ws) throws Exception {
        return Git.with(listener, env).in(ws).using("jgit").getClient();
    }

    /**
     * timeout is not implemented in JGitAPIImpl.
     */
    @Override
    protected boolean getTimeoutVisibleInCurrentTest() {
        return false;
    }

    /**
     * Override to run the test and assert its state.
     *
     * @throws Throwable if any exception is thrown
     */
    protected void runTest() throws Throwable {
        Method m = getClass().getMethod(getName());

        if (m.getAnnotation(NotImplementedInJGit.class)!=null)
            return; // skip this test case

        try {
            m.invoke(this);
        } catch (InvocationTargetException e) {
            e.fillInStackTrace();
            throw e.getTargetException();
        } catch (IllegalAccessException e) {
            e.fillInStackTrace();
            throw e;
        }
    }
}
