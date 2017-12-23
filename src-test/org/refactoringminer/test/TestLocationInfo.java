package org.refactoringminer.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import gr.uom.java.xmi.LocationInfo;

public class TestLocationInfo {
	
	@Test
	public void testLocationInfoNormalLineEndings() {
		
		String string = "public void test() {\n"
				+ "\n"
				+ "\tmethod();\n\n"
				+ "}\n";
		
		LocationInfo locationInfo = new LocationInfo(string, "", 0, 3);
		assertEquals(0, locationInfo.getStartLine());
		assertEquals(0, locationInfo.getEndLine());
		assertEquals(4, locationInfo.getLength());
		assertEquals(0, locationInfo.getStartColumn());
		assertEquals(3, locationInfo.getEndColumn());
	
		locationInfo = new LocationInfo(string, "", 1, 25);
		assertEquals(0, locationInfo.getStartLine());
		assertEquals(2, locationInfo.getEndLine());
		assertEquals(25, locationInfo.getLength());
		assertEquals(1, locationInfo.getStartColumn());
		assertEquals(3, locationInfo.getEndColumn());
		
		locationInfo = new LocationInfo(string, "", 0, 35);
		assertEquals(0, locationInfo.getStartLine());
		assertEquals(4, locationInfo.getEndLine());
		assertEquals(36, locationInfo.getLength());
		assertEquals(0, locationInfo.getStartColumn());
		assertEquals(1, locationInfo.getEndColumn());
		
	}
	
	@Test
	public void testLocationInfoNoLineEndingAtTheEndOfTheFile() {
		
		String string = "public void test() {\n"
				+ "\n"
				+ "\tmethod();\n\n"
				+ "}";
	
		LocationInfo locationInfo = new LocationInfo(string, "", 23, 34);
		assertEquals(2, locationInfo.getStartLine());
		assertEquals(4, locationInfo.getEndLine());
		assertEquals(12, locationInfo.getLength());
		assertEquals(1, locationInfo.getStartColumn());
		assertEquals(0, locationInfo.getEndColumn());
		
	}

	@Test
	public void testLocationInfoOldUnixStyleLineEndings() {
		
		String string = "public void test() {\r"
				+ "\r"
				+ "\tmethod();\r\r"
				+ "}\r";
		
		LocationInfo locationInfo = new LocationInfo(string, "", 0, 3);
		assertEquals(0, locationInfo.getStartLine());
		assertEquals(0, locationInfo.getEndLine());
		assertEquals(4, locationInfo.getLength());
		assertEquals(0, locationInfo.getStartColumn());
		assertEquals(3, locationInfo.getEndColumn());
	
		locationInfo = new LocationInfo(string, "", 1, 25);
		assertEquals(0, locationInfo.getStartLine());
		assertEquals(2, locationInfo.getEndLine());
		assertEquals(25, locationInfo.getLength());
		assertEquals(1, locationInfo.getStartColumn());
		assertEquals(3, locationInfo.getEndColumn());
		
	}
	
	@Test
	public void testLocationInfoWindowsLineEndings() {
		
		String string = "public void test() {\r\n"
				+ "\r\n"
				+ "\tmethod();\r\n\r\n"
				+ "}\r\n";
		
		LocationInfo locationInfo = new LocationInfo(string, "", 0, 3);
		assertEquals(0, locationInfo.getStartLine());
		assertEquals(0, locationInfo.getEndLine());
		assertEquals(4, locationInfo.getLength());
		assertEquals(0, locationInfo.getStartColumn());
		assertEquals(3, locationInfo.getEndColumn());
	
		locationInfo = new LocationInfo(string, "", 1, 25);
		assertEquals(0, locationInfo.getStartLine());
		assertEquals(2, locationInfo.getEndLine());
		assertEquals(25, locationInfo.getLength());
		assertEquals(1, locationInfo.getStartColumn());
		assertEquals(1, locationInfo.getEndColumn());
		
		locationInfo = new LocationInfo(string, "", 38, 40);
		assertEquals(4, locationInfo.getStartLine());
		assertEquals(5, locationInfo.getEndLine());
		assertEquals(3, locationInfo.getLength());
		assertEquals(0, locationInfo.getStartColumn());
		assertEquals(0, locationInfo.getEndColumn());
		
	}
	
}
