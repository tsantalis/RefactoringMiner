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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.libertybans.bootstrap.plugin.PluginInfo;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.service.FuturePoster;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

@Singleton
public class CommandsCore implements Commands {

	private final FactoryOfTheFuture futuresFactory;
	private final FuturePoster futurePoster;
	private final Configs configs;
	private final Provider<ArgumentParser> argumentParser;
	
	private final List<SubCommandGroup> subCommands;
	
	private static final String BASE_COMMAND_PERMISSION = "libertybans.commands";
	
	@Inject
	public CommandsCore(FactoryOfTheFuture futuresFactory, Configs configs, Provider<ArgumentParser> argumentParser,
			FuturePoster futurePoster,
			PlayerPunishCommands playerPunish, AddressPunishCommands addressPunish,
			PlayerUnpunishCommands playerUnpunish, AddressUnpunishCommands addressUnpunish,
			ListCommands list, AdminCommands admin) {
		this.futuresFactory = futuresFactory;
		this.futurePoster = futurePoster;
		this.configs = configs;
		this.argumentParser = argumentParser;
		subCommands = List.of(playerPunish, addressPunish, playerUnpunish, addressUnpunish, list, admin);
	}
	
	FactoryOfTheFuture futuresFactory() {
		return futuresFactory;
	}
	
	Configs configs() {
		return configs;
	}
	
	ArgumentParser argumentParser() {
		return argumentParser.get();
	}
	
	FuturePoster futurePoster() {
		return futurePoster;
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
			sendUsage(sender, command, firstArg.equals("usage") || firstArg.equals("help"));
			return;
		}
		CommandExecution execution = subCommand.execute(sender, command, firstArg);
		execution.execute();
	}
	
	/*
	 * Usage
	 */
	
	private void sendUsage(CmdSender sender, CommandPackage command, boolean explicit) {
		if (!explicit) {
			sender.sendMessage(configs.getMessagesConfig().all().usage());
		}
		UsageSection[] sections = UsageSection.values();
		int page = 1;
		if (command.hasNext()) {
			try {
				page = Integer.parseInt(command.next());
			} catch (NumberFormatException ignored) {}
			if (page <= 0 || page > sections.length) {
				page = 1;
			}
		}
		UsageSection section = sections[page - 1];
		sender.sendMessageNoPrefix(section.getContent());
		sender.sendLiteralMessage("&ePage " + page + "/4. &7Use /libertybans usage <page> to navigate");
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
		List<String> filtered = new ArrayList<>();
		for (String suggestion : unfilteredSuggestions) {
			if (suggestion.startsWith(argument)) {
				filtered.add(suggestion);
			}
		}
		filtered.sort(null);
		return List.copyOf(filtered);
	}
	
}
