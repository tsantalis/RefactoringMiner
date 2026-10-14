/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.core.commands.extra;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContainsCITest {

	private final Set<String> strings = Set.of("first", "Second string", "more words");

	@Test
	public void contained() {
		assertTrue(ContainsCI.containsIgnoreCase(strings, "first"), "Contained simply");
		assertTrue(ContainsCI.containsIgnoreCase(strings, "SECOND string"), "Contained ignoring case");
	}

	@Test
	public void notContained() {
		assertFalse(ContainsCI.containsIgnoreCase(strings, "not contained"));
	}
}
