package net.sf.latexdraw.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import net.sf.latexdraw.instrument.StatusBarController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class TestVersionChecker {
	@Test
	public void testCheckVersionNoNewVersion() throws InterruptedException {
		final StatusBarController controller = mock(StatusBarController.class);
		final FutureTask<Void> future = new FutureTask<>(new VersionChecker(controller, new LangService(new SystemService())), null);
		final ExecutorService taskExecutor = Executors.newFixedThreadPool(1);
		taskExecutor.execute(future);
		taskExecutor.shutdown();
		taskExecutor.awaitTermination(5L, TimeUnit.SECONDS);
	}
}
