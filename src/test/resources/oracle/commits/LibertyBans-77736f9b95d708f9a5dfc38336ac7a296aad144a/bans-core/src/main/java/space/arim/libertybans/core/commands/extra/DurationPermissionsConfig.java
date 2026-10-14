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

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.ConfSerialisers;

import java.util.Set;

@ConfSerialisers(DurationPermissionSerialiser.class)
public interface DurationPermissionsConfig {

	@ConfComments("If disabled, players are not checked for duration permissions.")
	@ConfDefault.DefaultBoolean(false)
	boolean enable();

	@ConfKey("permissions-to-check")
	@ConfComments({"Which duration permissions should a staff member be checked for?",
			"Specify all the durations which you want to explicitly grant permission for."})
	@ConfDefault.DefaultStrings({"perm", "30d", "10d", "4h"})
	Set<DurationPermission> permissionsToCheck();

}
