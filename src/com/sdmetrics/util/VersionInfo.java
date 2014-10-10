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

import java.util.Arrays;
import java.util.List;

/** Provides program version information to the application. */
public class VersionInfo {
	/**
	 * List of version numbers used in project files, in chronological ascending
	 * order.
	 */
	public final static List<String> XML_FILE_VERSIONS = Arrays.asList("1.0",
			"1.1", "1.2", "1.3", "2.0", "2.1", "2.2");

	/** The current name of the file that stores the application properties. */
	public final static String PROPERTIES_FILE = ".SDMetrics2.2.props";
	/** The previous files that stored the application properties. */
	final static String[] oldVersions = { ".SDMetricsDemo2.2.props",
			".SDMetrics2.1.props", ".SDMetricsDemo2.1.props",
			".SDMetrics2.0.props", ".SDMetricsDemo2.0.props",
			".SDMetrics1.3.props", ".SDMetricsDemo1.3.props",
			".SDMetrics1.2.props", ".SDMetricsDemo1.2.props",
			".SDMetrics.props" };
	/** Current version number */
	public final static String VERSION_NUMBER = "V2.2";
	/** Program name+version. */
	public final static String VERSION = "SDMetrics " + VERSION_NUMBER;
	/** Copyright string, using unicode characters. */
	public final static String COPYRIGHT = "\u00a9 2002-2011 by J\u00FCrgen W\u00FCst";
	/** Copyright string, using only ASCII characters. */
	public final static String COPYRIGHTASCII = "(c) 2002-2011 by Juergen Wuest";
}
