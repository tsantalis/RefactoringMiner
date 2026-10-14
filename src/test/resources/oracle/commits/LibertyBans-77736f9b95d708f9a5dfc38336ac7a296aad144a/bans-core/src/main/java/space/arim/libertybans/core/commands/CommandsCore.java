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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import space.arim.libertybans.bootstrap.plugin.PluginInfo;
import space.arim.libertybans.core.commands.usage.UsageGlossary;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.env.CmdSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Singleton
public class CommandsCore implements Commands {

	private final Configs configs;
	private final UsageGlossary usage;
	
	private final List<SubCommandGroup> subCommands;
	
	static final String BASE_COMMAND_PERMISSION = "libertybans.commands";

	CommandsCore(Configs configs, UsageGlossary usage, List<SubCommandGroup> subCommands) {
		this.configs = configs;
		this.usage = usage;
		this.subCommands = subCommands;
	}

	@Inject
	public CommandsCore(Configs configs, UsageGlossary usage,
			PlayerPunishCommands playerPunish, AddressPunishCommands addressPunish,
			PlayerUnpunishCommands playerUnpunish, AddressUnpunishCommands addressUnpunish,
			ListCommands list, AdminCommands admin, ImportCommands importing) {
		this(configs, usage, List.of(
				playerPunish, addressPunish, playerUnpunish, addressUnpunish, list, admin, importing));
	}
	
	private SubCommandGroup getMatchingSubCommand(String firstArg) {
		for (SubCommandGroup subCommand : subCommands) {
			if (subCommand.matches(firstArg)) {
				return subCommand;
			}
		}
		return null;
	}
	
	/*
	 * Main command handler
	 */
	
	@Override
	public void execute(CmdSender sender, CommandPackage command) {
		if (!sender.hasPermission(BASE_COMMAND_PERMISSION)) {
			sender.sendMessage(configs.getMessagesConfig().all().basePermissionMessage());
			return;
		}
		if (!command.hasNext()) {
			sender.sendLiteralMessage(
					"&7Running LibertyBans &r&e" +  PluginInfo.VERSION + "&7. Use '/libertybans usage' for help");
			return;
		}
		String firstArg = command.next().toLowerCase(Locale.ROOT);
		SubCommandGroup subCommand = getMatchingSubCommand(firstArg);
		if (subCommand == null) {
			usage.sendUsage(sender, command, firstArg.equals("usage") || firstArg.equals("help"));
			return;
		}
		CommandExecution execution = subCommand.execute(sender, command, firstArg);
		execution.execute();
	}
	
	/*
	 * Tab completion
	 */
	
	@Override
	public List<String> suggest(CmdSender sender, String[] args) {
		if (args.length == 0 || !sender.hasPermission(BASE_COMMAND_PERMISSION)
				|| !configs.getMainConfig().commands().tabComplete()) {
			return List.of();
		}
		String firstArg = args[0].toLowerCase(Locale.ROOT);
		SubCommandGroup subCommand = getMatchingSubCommand(firstArg);
		if (subCommand == null) {
			return List.of();
		}
		int argIndex = args.length - 1;
		Collection<String> unfiltered = subCommand.suggest(sender, firstArg, argIndex);
		return filterAndSort(unfiltered, args[argIndex]);
	}
	
	private List<String> filterAndSort(Collection<String> unfilteredSuggestions, String argument) {
		if (unfilteredSuggestions.isEmpty()) {
			return List.of();
		}
		List<String> filtered = new ArrayList<>(unfilteredSuggestions);
		filtered.removeIf((suggestion) -> !suggestion.startsWith(argument));
		filtered.sort(null);
		return List.copyOf(filtered);
	}
	
}
