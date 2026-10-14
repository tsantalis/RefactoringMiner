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

import java.time.ZoneId;
import java.util.Set;

import space.arim.libertybans.core.selector.EnforcementConfig;
import space.arim.libertybans.core.uuid.UUIDResolutionConfig;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault.DefaultBoolean;
import space.arim.dazzleconf.annote.ConfDefault.DefaultString;
import space.arim.dazzleconf.annote.ConfDefault.DefaultStrings;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;

@ConfHeader({
		"",
		"",
		"The main LibertyBans configuration",
		"All options here can be updated with /libertybans reload",
		"",
		""})
public interface MainConfig {

	@ConfKey("lang-file")
	@ConfComments({"What language file should be used for messages?",
			"For example, 'en' means LibertyBans will look for a file called 'messages_en.yml'"})
	@DefaultString("en")
	String langFile();
	
	@ConfKey("date-formatting")
	@SubSection
	DateFormatting dateFormatting();
	
	@ConfHeader("Formatting of absolute dates")
	interface DateFormatting {
		
		@ConfKey("format")
		@ConfComments("How should dates be formatted? Follows Java's DateTimeFormatter.ofPattern")
		@DefaultString("dd/MM/yyyy kk:mm")
		DateTimeFormatterWithPattern formatAndPattern();
		
		@ConfKey("timezone")
		@ConfComments({"Do you want to override the timezone? If 'default', the system default timezone is used",
			"The value must be in continent/city format, such as 'America/New_York'. Uses Java's ZoneId.of"})
		@DefaultString("default")
		ZoneId zoneId();
		
	}
	
	@SubSection
	Reasons reasons();
	
	interface Reasons {
		
		@ConfKey("permit-blank")
		@ConfComments({"Are blank reasons permitted?",
			"If blank reasons are not permitted, staff must specify the full reason",
			"If blank reasons are permitted, the default reason is used"})
		@DefaultBoolean(false)
		boolean permitBlank();
		
		@ConfKey("default-reason:")
		@ConfComments("If the above is true, what is the default reason to use when staff do not specify a reason?")
		@DefaultString("No reason stated.")
		String defaultReason();
		
	}
	
	@SubSection
	DurationPermissions durationPermissions();
	
	interface DurationPermissions {
		
		@ConfComments("If disabled, players are not checked for duration permissions.")
		@DefaultBoolean(false)
		boolean enable();
		
		@ConfKey("permissions-to-check")
		@ConfComments({"Which duration permissions should a staff member be checked for?",
				"Specify all the durations which you want to explicitly grant permission for."})
		@DefaultStrings({"perm", "30d", "10d", "4h"})
		Set<String> permissionsToCheck();
		
	}
	
	@SubSection
	EnforcementConfig enforcement(); // Sensitive name used in integration testing
	
	@ConfKey("player-uuid-resolution")
	@SubSection
	UUIDResolutionConfig uuidResolution();
	
	@SubSection
	Commands commands();
	
	interface Commands {
		
		@ConfKey("enable-tab-completion")
		@ConfComments("Whether to enable tab completion")
		@DefaultBoolean(true)
		boolean tabComplete();
		
		@ConfComments({"What commands should be registered as aliases for libertybans commands?",
			"For each command listed here, '/<command>' will be equal to '/libertybans <command>'"})
		@DefaultStrings({
			"ban", "ipban",
		    "mute", "ipmute",
		    "warn", "ipwarn",
		    "kick", "ipkick",
		    "unban", "unbanip",
		    "unmute", "unmuteip",
		    "unwarn", "unwarnip",
		    "banlist",
		    "mutelist",
		    "history",
		    "warns",
		    "blame"
		})
		Set<String> aliases();
		
	}
	
}
