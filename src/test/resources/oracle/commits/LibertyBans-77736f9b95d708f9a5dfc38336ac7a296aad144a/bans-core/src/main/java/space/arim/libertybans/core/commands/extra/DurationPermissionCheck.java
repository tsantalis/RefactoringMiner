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

package space.arim.libertybans.core.commands.extra;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.config.MainConfig;
import space.arim.libertybans.core.env.CmdSender;

import java.time.Duration;

public class DurationPermissionCheck {

	private final CmdSender sender;
	private final MainConfig config;

	public DurationPermissionCheck(CmdSender sender, MainConfig config) {
		this.sender = sender;
		this.config = config;
	}

	public boolean isDurationPermitted(PunishmentType type, Duration attemptedDuration) {
		if (type == PunishmentType.KICK) {
			return attemptedDuration.isZero();
		}
		if (!config.durationPermissions().enable()) {
			return true;
		}
		Duration greatestPermitted = getGreatestPermittedDuration(type);
		if (greatestPermitted.isZero()) {
			// User can punish permanently
			return true;
		}
		if (attemptedDuration.isZero()) {
			// User cannot punish permanently but attempted duration is permanent
			return false;
		}
		// User can punish only if their permitted duration is large enough
		return greatestPermitted.compareTo(attemptedDuration) >= 0;
	}

	private Duration getGreatestPermittedDuration(PunishmentType type) {
		Duration greatestPermission = Duration.ofNanos(-1L);
		for (DurationPermission durationPermission : config.durationPermissions().permissionsToCheck()) {
			if (!sender.hasPermission(durationPermission.permission(type))) {
				continue;
			}
			Duration durationValue = durationPermission.duration();
			if (durationValue.isZero()) {
				// User has permission for permanent punishment
				return Duration.ZERO;
			}
			if (durationValue.compareTo(greatestPermission) > 0) {
				greatestPermission = durationValue;
			}
		}
		return greatestPermission;
	}

}
