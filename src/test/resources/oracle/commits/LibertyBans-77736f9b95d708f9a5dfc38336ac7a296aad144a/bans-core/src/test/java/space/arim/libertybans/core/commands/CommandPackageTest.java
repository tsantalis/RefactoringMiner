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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandPackageTest {
	
	@ParameterizedTest
	@ArgumentsSource(CommandPackageArgumentsProvider.class)
	@CommandPackageArgumentsProvider.CommandArgs(cmd = "Cmd", args = {})
	public void noArguments(CommandPackage cmdPackage) {
		assertEquals(cmdPackage.getCommand(), "cmd");
		assertFalse(cmdPackage.hasNext());
		assertTrue(cmdPackage.allRemaining().isEmpty());
		assertThrows(NoSuchElementException.class, cmdPackage::next);
	}

	@ParameterizedTest
	@ArgumentsSource(CommandPackageArgumentsProvider.class)
	@CommandPackageArgumentsProvider.CommandArgs(cmd = "cmd", args = {"arg"})
	public void oneArgument(CommandPackage cmdPackage) {
		assertTrue(cmdPackage.hasNext());
		CommandPackage copy = cmdPackage.copy();

		assertEquals("arg", cmdPackage.allRemaining());
		assertThrows(NoSuchElementException.class, cmdPackage::next);

		assertEquals("arg", copy.next());
		assertThrows(NoSuchElementException.class, copy::next);
	}

	@ParameterizedTest
	@ArgumentsSource(CommandPackageArgumentsProvider.class)
	@CommandPackageArgumentsProvider.CommandArgs(cmd = "cmd", args = {"arg", "second", "another"})
	public void multipleArguments(CommandPackage cmdPackage) {
		assertTrue(cmdPackage.hasNext());
		CommandPackage copy = cmdPackage.copy();

		assertEquals("arg second another", cmdPackage.allRemaining());
		assertThrows(NoSuchElementException.class, cmdPackage::next);

		assertEquals("arg", copy.next());
		assertEquals("second", copy.next());
	}
	
}
