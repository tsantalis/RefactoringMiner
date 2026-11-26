package com.xebia.incubator.xebium;


import static org.junit.Assert.assertEquals;

import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SeleniumScriptFixtureTest {

	@Before
	public void setUp() throws Exception {
		BasicConfigurator.configure();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRunScript() throws Exception {
		SeleniumScriptFixture fixture = new SeleniumScriptFixture();
		
		assertEquals("PASSED", fixture.runScript("http://files/selenium/testsuite.html"));
	}
	
	/*
	@Test
	@Ignore
	public void simpleSeleniumTest() throws Exception {
		Selenium selenium = new DefaultSelenium("localhost", 4444, "*safari", "http://google.nl");
		
		selenium.start();
		
		selenium.open("http://google.nl");
		selenium.type("q", "xebium is the new test solution");
		
		selenium.click("btnG");
		selenium.waitForPageToLoad("30000");
		for (int second = 0;; second++) {
			if (second >= 60) fail("timeout");
			try { if (selenium.isElementPresent("link=Project Xebium! - The Xebia Fitnesse Selenium integration solution ...")) break; } catch (Exception e) {}
			Thread.sleep(1000);
		}

		selenium.click("link=Project Xebium! - The Xebia Fitnesse Selenium integration solution ...");
		selenium.waitForPageToLoad("30000");
		for (int second = 0;; second++) {
			if (second >= 60) fail("timeout");
			try { if (selenium.isTextPresent("Project Xebium! - The Xebia Fitnesse Selenium integration solution.")) break; } catch (Exception e) {}
			Thread.sleep(1000);
		}
		
		selenium.stop();
	}
	*/
}
