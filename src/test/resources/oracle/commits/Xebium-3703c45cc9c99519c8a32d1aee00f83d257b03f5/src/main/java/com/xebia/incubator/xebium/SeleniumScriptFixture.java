package com.xebia.incubator.xebium;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.openqa.selenium.server.RemoteControlConfiguration;
import org.openqa.selenium.server.SeleniumServer;
import org.openqa.selenium.server.htmlrunner.HTMLLauncher;

public class SeleniumScriptFixture {

	private static Logger LOG = Logger.getLogger(SeleniumScriptFixture.class);
	
	private String browser = "*firefox";
	private String browserURL = "http://google.nl";
	private File outputFile = new File("SeleniumScriptFixture.log");
	private int timeoutInSeconds = 30;
	private boolean multiWindow = false;

	static {
		BasicConfigurator.configure();
	}
	
	public String runScript(String scriptName) throws Exception {
		File suiteFile = asFile(scriptName);
		
		RemoteControlConfiguration configuration = new RemoteControlConfiguration();
		configuration.setProxyInjectionModeArg(true);
		configuration.setPort(4444);
		
		SeleniumServer remoteControl = new SeleniumServer(configuration);
		remoteControl.start();

		String result = null;
		try {
			LOG.info("Server started, launching test suite");
			HTMLLauncher launcher = new HTMLLauncher(remoteControl);
			//try {
			result = launcher.runHTMLSuite(browser, browserURL, suiteFile, outputFile, timeoutInSeconds, multiWindow);
			//} catch (SeleniumCommandTimedOutException e) {
			//	LOG.error("HTML suite failed with timeout", e);
			//}
			LOG.info("Finished execution of test suite, result = " + result);
		} catch (Exception e) {
			LOG.error("Failed to run test suite", e);
			throw e;
		} finally {
			// TODO: do something with result
			remoteControl.stop();
		}
		
		LOG.debug("End of RunScript");
		return result;
	}

	private File asFile(final String scriptName) {
		String fileName = scriptName.replaceAll("http:/", "FitNesseRoot");
		
		return new File(fileName);
	}
	
}
