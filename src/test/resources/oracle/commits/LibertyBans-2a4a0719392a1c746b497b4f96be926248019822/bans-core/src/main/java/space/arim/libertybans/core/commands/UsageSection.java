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

import java.util.Locale;

import space.arim.api.chat.SendableMessage;
import space.arim.api.chat.serialiser.LegacyCodeSerialiser;
import space.arim.api.chat.serialiser.SendableMessageSerialiser;

enum UsageSection {

	PUNISH("&e/ban, /ipban &7- ban players",
			"&e/mute, /ipmute &7- mute players",
			"&e/warn, /ipwarn &7- warn players",
			"&e/kick, /ipkick &7- kick players"),
	UNPUNISH("&e/unban, /unbanip &7- undo a ban",
			"&e/unmute, /unmuteip &7- undo a mute",
			"&e/unwarn, /unwarnip &7- undo a warn"),
	LIST("&e/banlist &7- view active bans",
			"&e/mutelist &7- view active mutes",
			"&e/history &7- view a player's history",
			"&e/warns &7- view a player's warns",
			"&e/blame &7- view another staff member's punishments"),
	OTHER("&e/libertybans &7usage - shows this",
			"&e/libertybans &7reload - reload config.yml and language configuration",
			"&e/libertybans &7restart - perform a full restart, reloads everything including database connections",
			"&e/libertybans &7debug - outputs debug information");
	
	private final SendableMessage content;
	
	UsageSection(String...commands) {
		String name = name();
		String headerString = "&b" + name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT) + " commands:";

		SendableMessageSerialiser serialiser = LegacyCodeSerialiser.getInstance('&');
		SendableMessage header = serialiser.deserialise(headerString);
		SendableMessage body = serialiser.deserialise("\n" + String.join("\n", commands));
		content = header.concatenate(body);
	}
	
	SendableMessage getContent() {
		return content;
	}
	
}
