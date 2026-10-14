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

public class NameValidatorTest {

	private final NameValidator validator = new StandardNameValidator();
	
	@Test
	public void testBadNameArguments() {
		// Valid name
		assertTrue(validator.validateNameArgument("A248"));
		
		// Invalid name with non alphanumeric/underscore
		assertFalse(validator.validateNameArgument("NameWith=Sign"));
		
		// Valid name with underscore
		assertTrue(validator.validateNameArgument("Name_Underscored"));
		
		// Invalid name of too much length
		assertFalse(validator.validateNameArgument("ThisNameHasMoreThan16Characters"));
	}
	
}
