/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.commands;

import java.time.Duration;
import java.util.Set;

class DurationParser {

	private final String argument;
	private final Set<String> permanentArguments;
	
	private static final Set<String> defaultPermanentArguments = Set.of("perm");
	
	DurationParser(String argument, Set<String> permanentArguments) {
		this.argument = argument;
		this.permanentArguments = Set.copyOf(permanentArguments);
	}
	
	DurationParser(String argument) {
		this(argument, defaultPermanentArguments);
	}

	Duration parse() {
		if (ContainsCI.containsIgnoreCase(permanentArguments, argument)) {
			return Duration.ZERO;
		}
		char[] characters = argument.toCharArray();
		int unitIndex = 0;
		for (int n = 0; n < characters.length; n++) {
			if (!Character.isDigit(characters[n])) {
				unitIndex = n;
				break;
			}
		}
		if (unitIndex == 0) {
			return Duration.ZERO;
		}
		long number = Long.parseLong(argument.substring(0, unitIndex));
		switch (argument.substring(unitIndex)) {
		case "Y":
		case "y":
			return Duration.ofDays(365L * number);
		case "MO":
		case "mo":
			return Duration.ofDays(30L * number);
		case "D":
		case "d":
			return Duration.ofDays(number);
		case "M":
		case "m":
			return Duration.ofMinutes(number);
		default:
			break;
		}
		return Duration.ZERO;
	}
	
}
