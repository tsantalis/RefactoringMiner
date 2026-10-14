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
package space.arim.libertybans.core.uuid;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class StandardNameValidatorTest {

	private final NameValidator validator = new StandardNameValidator();

	@ParameterizedTest
	@ValueSource(strings = {"A248", "ObsidianWolf_", "lol_haha_dead"})
	public void validNames(String name) {
		assertTrue(validator.validateNameArgument(name));
	}

	@Test
	public void nonAlphaNumericUnderscore() {
		assertFalse(validator.validateNameArgument("NameWith=Sign"));
	}

	@Test
	public void tooManyCharacters() {
		assertFalse(validator.validateNameArgument("ThisNameHasMoreThan16Characters"));
	}
	
}
