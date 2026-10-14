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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.config.MainConfig;
import space.arim.libertybans.core.env.CmdSender;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DurationPermissionCheckTest {

	private final CmdSender sender;
	private final MainConfig config;

	private final DurationPermissionCheck check;

	public DurationPermissionCheckTest(@Mock CmdSender sender, @Mock MainConfig config) {
		this.sender = sender;
		this.config = config;

		check = new DurationPermissionCheck(sender, config);
	}

	private boolean isBanPermitted(Duration duration) {
		return check.isDurationPermitted(PunishmentType.BAN, duration);
	}

	@Test
	public void isDurationPermitted() {
		DurationPermissionsConfig durationPermissions = mock(DurationPermissionsConfig.class);
		when(config.durationPermissions()).thenReturn(durationPermissions);
		when(durationPermissions.enable()).thenReturn(true);
		when(durationPermissions.permissionsToCheck()).thenReturn(Set.of(
				new DurationPermission("1m", Duration.ofMinutes(1L)),
				new DurationPermission("4h", Duration.ofHours(4L)),
				new DurationPermission("perm", Duration.ZERO)));

		assertFalse(isBanPermitted(Duration.ofHours(3L)), "User has no permissions");

		when(sender.hasPermission("libertybans.dur.ban.1m")).thenReturn(true);
		assertFalse(isBanPermitted(Duration.ofHours(3L)), "User does not have sufficient permission");
		assertFalse(isBanPermitted(Duration.ZERO), "User cannot ban permanently");

		when(sender.hasPermission("libertybans.dur.ban.4h")).thenReturn(true);
		assertTrue(isBanPermitted(Duration.ofHours(3L)), "User has permission for 4h which is greater than 3h");
		assertFalse(isBanPermitted(Duration.ofDays(1L)), "User does not have sufficient permission for 1 day");

		when(sender.hasPermission("libertybans.dur.ban.perm")).thenReturn(true);
		assertTrue(isBanPermitted(Duration.ofDays(30L)), "User can ban permanently so 30 days is acceptable");
		assertTrue(isBanPermitted(Duration.ZERO), "User can ban permanently");
	}
}
