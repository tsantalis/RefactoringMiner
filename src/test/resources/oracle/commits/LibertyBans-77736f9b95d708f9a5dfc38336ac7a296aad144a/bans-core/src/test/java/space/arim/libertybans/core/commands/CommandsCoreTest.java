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

package space.arim.libertybans.core.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.api.chat.SendableMessage;
import space.arim.api.chat.serialiser.SimpleTextSerialiser;
import space.arim.libertybans.core.commands.usage.UsageGlossary;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.env.CmdSender;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CommandsCoreTest {

	private final Configs configs;
	private final UsageGlossary usage;
	private final CmdSender sender;

	public CommandsCoreTest(@Mock Configs configs, @Mock UsageGlossary usage, @Mock CmdSender sender) {
		this.configs = configs;
		this.usage = usage;
		this.sender = sender;
	}

	private CommandsCore newCommandsCore(List<SubCommandGroup> subCommands) {
		return new CommandsCore(configs, usage, subCommands);
	}

	@Test
	public void noPermission() {
		SendableMessage noPermMessage = SimpleTextSerialiser.getInstance().deserialise("No permission");

		MessagesConfig messagesConfig = mock(MessagesConfig.class);
		MessagesConfig.All all = mock(MessagesConfig.All.class);
		when(configs.getMessagesConfig()).thenReturn(messagesConfig);
		when(messagesConfig.all()).thenReturn(all);
		when(all.basePermissionMessage()).thenReturn(noPermMessage);

		CommandPackage dummyCommand = new ArrayCommandPackage("cmd", "args");
		newCommandsCore(List.of()).execute(sender, dummyCommand);

		verify(sender).sendMessage(noPermMessage);
	}

	private void addBasePermission() {
		when(sender.hasPermission(CommandsCore.BASE_COMMAND_PERMISSION)).thenReturn(true);
	}

	@Test
	public void usageUnknownCommand() {
		addBasePermission();

		CommandPackage dummyCommand = new ArrayCommandPackage("cmd", "args");
		newCommandsCore(List.of()).execute(sender, dummyCommand);

		verify(usage).sendUsage(sender, dummyCommand, false);
	}

	@Test
	public void explicitUsage() {
		addBasePermission();

		CommandPackage dummyCommand = new ArrayCommandPackage("cmd", "help");
		newCommandsCore(List.of()).execute(sender, dummyCommand);

		verify(usage).sendUsage(sender, dummyCommand, true);
	}

	@Test
	public void matchCommand() {
		addBasePermission();

		SubCommandGroup subCommandOne = mock(SubCommandGroup.class);
		CommandExecution subCommandOneExecution = mock(CommandExecution.class);
		when(subCommandOne.matches("one")).thenReturn(true);
		when(subCommandOne.execute(any(), any(), any())).thenReturn(subCommandOneExecution);
		SubCommandGroup subCommandTwo = mock(SubCommandGroup.class);

		CommandPackage commandOne = new ArrayCommandPackage("cmd", "one");
		newCommandsCore(List.of(subCommandOne, subCommandTwo)).execute(sender, commandOne);

		verify(subCommandOne).execute(sender, commandOne, "one");
		verify(subCommandOneExecution).execute();
	}

	@Test
	public void matchNoCommand() {
		addBasePermission();

		SubCommandGroup subCommandOne = mock(SubCommandGroup.class);
		SubCommandGroup subCommandTwo = mock(SubCommandGroup.class);

		CommandPackage commandNone = new ArrayCommandPackage("cmd", "none");
		newCommandsCore(List.of(subCommandOne, subCommandTwo)).execute(sender, commandNone);

		verify(usage).sendUsage(sender, commandNone, false);
	}

}
