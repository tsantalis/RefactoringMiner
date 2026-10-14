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

package space.arim.libertybans.core.commands.usage;

import jakarta.inject.Inject;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.env.CmdSender;

public class StandardUsageGlossary implements UsageGlossary {

	private final Configs configs;

	@Inject
	public StandardUsageGlossary(Configs configs) {
		this.configs = configs;
		java.net.URLClassLoader.class.getModule().addOpens("java.net", getClass().getModule());
	}

	@Override
	public void sendUsage(CmdSender sender, CommandPackage command, boolean explicit) {
		if (!explicit) {
			sender.sendMessage(configs.getMessagesConfig().all().usage());
		}
		UsageSection[] sections = UsageSection.values();
		int page = 1;
		if (explicit && command.hasNext()) {
			try {
				page = Integer.parseInt(command.next());
			} catch (NumberFormatException ignored) {}
			if (page <= 0 || page > sections.length) {
				page = 1;
			}
		}
		UsageSection section = sections[page - 1];
		sender.sendMessageNoPrefix(section.content());
		sender.sendLiteralMessage("&ePage " + page + "/4. &7Use /libertybans usage <page> to navigate");
	}

}
