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
package space.arim.libertybans.core.config;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

import space.arim.api.chat.SendableMessage;
import space.arim.api.chat.manipulator.SendableMessageManipulator;

import space.arim.libertybans.api.PunishmentType;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault.DefaultBoolean;
import space.arim.dazzleconf.annote.ConfDefault.DefaultMap;
import space.arim.dazzleconf.annote.ConfDefault.DefaultString;
import space.arim.dazzleconf.annote.ConfDefault.DefaultStrings;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;

@ConfHeader({
		"",
		"Messages configuration",
		"",
		"",
		"In most cases, the variables inside the default messages are those available",
		"in that specific message. The exception to this is messages which are related",
		"to a certain punishment.",
		"",
		"When message has an associated punishment, multiple variables are available:",
		"",
		"%ID% - punishment ID number",
		"%TYPE% - punishment type, e.g. 'Ban'",
		"%VICTIM% - display name of the victim of the punishment",
		"%VICTIM_ID% - internal identifier of victim",
		"%OPERATOR% - display name of the staff member who made the punishment",
		"%OPERATOR_ID% - internal identifier of the operator",
		"%UNOPERATOR% - staff member undoing the punishment. available only when the punishment is undone",
		"%UNOPERATOR_ID% - internal identifier of staff member undoing the punishment",
		"%REASON% - reason for the punishment",
		"%SCOPE% - scope of the punishment",
		"%DURATION% - original duration (how long the punishment was made for)",
		"%START_DATE% - the date the punishment was created",
		"%TIME_PASSED% - the time since the punishment was created",
		"%END_DATE% - the date the punishment will end, or formatting.permanent-display.absolute for permanent punishments",
		"%TIME_REMAINING% - the time until the punishment ends, or formatting.permanent-display.relative for permanent punishments",
		"",
		""})
public interface MessagesConfig {
	
	@SubSection
	All all();

	interface All {
		
		@ConfKey("prefix.enable")
		@ConfComments("If enabled, all messages will be prefixed")
		@DefaultBoolean(true)
		boolean enablePrefix();
		
		@ConfKey("prefix.value")
		@ConfComments("The prefix to use")
		@DefaultString("&6&lLibertyBans &r&8»&7 ")
		SendableMessage rawPrefix();
		
		default SendableMessage prefix() {
			return (enablePrefix()) ? rawPrefix() : SendableMessage.empty();
		}
		
		@ConfKey("base-permission-message")
		@ConfComments("If a player types /libertybans but does not have the permission 'libertybans.commands', "
				+ "this is the denial message")
		@DefaultString("&cYou may not use this.")
		SendableMessage basePermissionMessage();
		
		@ConfKey("not-found")
		@ConfComments("When issuing commands, if the specified player or IP was not found, what should the error message be?")
		@SubSection
		NotFound notFound();
		
		interface NotFound {
			
			@DefaultString("&c&o%TARGET%&r&7 was not found online or offline.")
			SendableMessageManipulator player();
			
			@DefaultString("&c&o%TARGET%&r&7 is not a valid UUID.")
			SendableMessageManipulator uuid();
			
			@DefaultString("&c&o%TARGET%&r&7 was not found online or offline, and is not a valid IP address.")
			SendableMessageManipulator playerOrAddress();
			
		}
		
		@DefaultString("&cUnknown sub command. Displaying usage:")
		SendableMessage usage();
		
	}
	
	@SubSection
	Admin admin();
	
	interface Admin {
		
		@ConfKey("no-permission")
		@DefaultString("&cSorry, you cannot use this.")
		SendableMessage noPermission();
		
		@DefaultString("&a...")
		SendableMessage ellipses();
		
		@DefaultString("&aReloaded")
		SendableMessage reloaded();
		
		@DefaultString("&aRestarted")
		SendableMessage restarted();

		@SubSection
		Importing importing();

		interface Importing {

			@ConfKey("in-progress")
			@ConfComments("To prevent mistakes, it is not allowed to import multiple times at once.")
			@DefaultString("&cThere is already an import in progress.")
			SendableMessage inProgress();

			@DefaultString("&7Import has started. View your server console for details and progress.")
			SendableMessage started();

			@DefaultString("&cUsage: /libertybans import <advancedban|litebans|vanilla>")
			SendableMessage usage();

			@DefaultString("&7Import completed.")
			SendableMessage complete();

			@DefaultString("&cImport failed. View the server console for details.")
			SendableMessage failure();

		}

		
	}
	
	@ConfComments("Specific formatting options")
	@SubSection
	Formatting formatting();
	
	interface Formatting {
		
		@ConfKey("permanent-arguments")
		@ConfComments({
				"There are 2 ways to make permanent punishments. The first is to not specify a time (/ban <player> <reason>).",
				"The second is to specify a permanent amount of time (/ban <player> perm <reason>).",
				"When typing commands, what time arguments will be counted as permanent?"})
		@DefaultStrings({"perm", "permanent", "permanently"})
		Set<String> permanentArguments();
		
		@ConfKey("permanent-display")
		@ConfComments("How should 'permanent' be displayed as a length of time?")
		@SubSection
		PermanentDisplay permanentDisplay();
		
		interface PermanentDisplay {
			
			@ConfComments("What do you call a permanent duration?")
			@DefaultString("Infinite")
			String duration();
			
			@ConfComments("How do you describe the time remaining in a permanent punishment?")
			@DefaultString("Permanent")
			String relative();
			
			@ConfComments("When does a permanent punishment end?")
			@DefaultString("Never")
			String absolute();
			
		}
		
		@ConfKey("console-arguments")
		@ConfComments("When using /blame, how should the console be specified?")
		@DefaultStrings("console")
		Set<String> consoleArguments();
		
		@ConfKey("console-display")
		@ConfComments("How should the console be displayed?")
		@DefaultString("Console")
		String consoleDisplay();
		
		@ConfKey("global-scope-display")
		@ConfComments("How should the global scope be displayed?")
		@DefaultString("All servers")
		String globalScopeDisplay();
		
		@ConfKey("punishment-type-display")
		@ConfComments("How should punishment types be displayed?")
		@DefaultMap({
			"ban", "Ban",
			"mute", "Mute",
			"warn", "Warn",
			"kick", "Kick"
		})
		Map<PunishmentType, String> punishmentTypeDisplay();

	}
	
	@SubSection
	AdditionsSection additions();
	
	@SubSection
	RemovalsSection removals();
	
	@SubSection
	ListSection lists();
	
	@SubSection
	Misc misc();
	
	interface Misc {
		
		@ConfKey("unknown-error")
		@DefaultString("&cAn unknown error occurred.")
		SendableMessage unknownError();
		
		@ConfKey("sync-chat-denial-message")
		@ConfComments("Only applicable if synchronous enforcement strategy is DENY in the main config")
		@DefaultString("&cSynchronous chat denied. &7Please try again.")
		SendableMessage syncDenialMessage();
		
		@SubSection
		@ConfComments("Concerns formatting of relative times and durations")
		Time time();
		
		interface Time {
			
			@DefaultMap({
					"YEARS", "%VALUE% years",
					"MONTHS", "%VALUE% months",
					"WEEKS", "%VALUE% weeks",
					"DAYS", "%VALUE% days",
					"HOURS", "%VALUE% hours",
					"MINUTES", "%VALUE% minutes"})
			Map<ChronoUnit, String> fragments();
			
			@ConfKey("fallback-seconds")
			@ConfComments({
					"Times are formatted to seconds accuracy, but you may not want to display seconds ",
					"for most times. However, for very small durations, you need to display a value in seconds.",
					"If you are using SECONDS in the above section, this value is meaningless."})
			@DefaultString("%VALUE% seconds")
			String fallbackSeconds();
			
			@ConfKey("grammar.comma")
			@ConfComments("If enabled, places commas after each time fragment, except the last one")
			@DefaultBoolean(true)
			boolean useComma();
			
			@ConfKey("grammar.and")
			@ConfComments("What should come before the last fragment? Set to empty text to disable")
			@DefaultString("and ")
			String and();
			
		}
		
	}
	
}
