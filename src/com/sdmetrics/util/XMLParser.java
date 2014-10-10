/*
 * SDMetrics Open Core for UML design measurement
 * Copyright (c) 2002-2011 Juergen Wuest
 * To contact the author, see <http://www.sdmetrics.com/Contact.html>.
 * 
 * This file is part of the SDMetrics Open Core.
 * 
 * SDMetrics Open Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
    
 * SDMetrics Open Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with SDMetrics Open Core.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.sdmetrics.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * Encapsulates access to the XML Parser. Creates an instance of a
 * non-validating SAX2 XML reader, and provides a simple method to parse an XML
 * file.
 */
public class XMLParser {
	/** Conditional compilation: XML parser prints stack traces on parse errors. */
	private final static boolean DEBUG = false;

	/** The SAX XML reader. */
	private XMLReader parser;
	/** Enhanced error message when the XML parsing fails. */
	private String errorMessage;

	/**
	 * Constructs a non-validating SAX parser.
	 * 
	 * @throws Exception The SAX parser could not be initialized.
	 */
	public XMLParser() throws Exception {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setValidating(false);
		spf.setNamespaceAware(false);

		SAXParser saxParser = spf.newSAXParser();
		parser = saxParser.getXMLReader();

		// Install an Entity Resolver to intercept all calls to external DTDs.
		// Keeps the SAX parser from trying to access any DTDs from the web or
		// elsewhere.
		parser.setEntityResolver(new EntityResolver() {
			@Override
			public InputSource resolveEntity(String publicId, String systemId) {
				return new InputSource(new StringReader(""));
			}
		});
	}

	/**
	 * Parses the XML file at the specified URI
	 * 
	 * @param uri URI of the XML file to parse.
	 * @param dh Default handler to process the XML elements
	 * @throws Exception A problem occurred while parsing
	 */
	public void parse(String uri, ContentHandler dh) throws Exception {
		parser.setContentHandler(dh);
		if (uri == null || uri.length() == 0) {
			errorMessage = "No input file specified.";
			throw new IllegalArgumentException(errorMessage);
		}

		// escape special characters, white spaces etc in the URI
		String uriToUse = uri;
		if (!uri.startsWith("file:")) {
			File tmp = new File(uri);
			if (tmp.exists() && tmp.isFile()) {
				URI realURI = tmp.toURI();
				uriToUse = realURI.toASCIIString();
			}
		}

		// parse file, enhance exception message on error
		Exception ex = null;
		try {
			parser.parse(uriToUse);
		} catch (org.xml.sax.SAXParseException spe) {
			errorMessage = "Parse error in line " + spe.getLineNumber() + ": "
					+ spe.getMessage();
			ex = spe;
		} catch (org.xml.sax.SAXException se) {
			errorMessage = se.getMessage();
			ex = se;
		} catch (IOException e) {
			errorMessage = "Could not open file '" + uri + "'.";
			ex = e;
		} catch (Exception e) {
			errorMessage = "Internal parser error: " + e.getMessage();
			ex = e;
		}

		// rethrow the exception
		if (ex != null) {
			if (DEBUG) {
				ex.printStackTrace(System.err);
			}
			throw ex;
		}
	}

	/**
	 * Gets an enhanced error message when parsing of a file failed. The
	 * enhanced error message may include line numbers pinpointing the source of
	 * the problem in the XMI file, or other clues what went wrong.
	 * 
	 * @return Description of the error.
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
}
