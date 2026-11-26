package com.xebia.incubator.xebium;


import static org.junit.Assert.assertEquals;

import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SeleniumScriptFixtureTest {

	SeleniumScriptFixture fixture;
	
	@BeforeClass
	public void classSetUp() throws Exception {
		BasicConfigurator.configure();
	}
	
	@Before
	public void setUp() throws Exception {
		fixture = new SeleniumScriptFixture();
		fixture.startServer();
	}
	
	@After
	public void tearDown() throws Exception {
		fixture.stopServer();
		fixture = null;
	}

	@Test
	public void testRunScript() throws Exception {
		assertEquals("PASSED", fixture.runSuite("http://files/selenium/testsuite.html"));
	}

	@Test
	public void testRunScriptWithAnchor() throws Exception {
		assertEquals("PASSED", fixture.runSuite("<a href='SomeFancyPancyUrl'>http://files/selenium/testsuite.html</a>"));
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
