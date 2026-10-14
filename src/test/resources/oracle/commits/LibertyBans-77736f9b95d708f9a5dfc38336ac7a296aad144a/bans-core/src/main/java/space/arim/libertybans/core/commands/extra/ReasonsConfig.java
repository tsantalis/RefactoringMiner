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

public interface ReasonsConfig {

	@ConfKey("permit-blank")
	@ConfComments({"Are blank reasons permitted?",
			"If blank reasons are not permitted, staff must specify the full reason",
			"If blank reasons are permitted, the default reason is used"})
	@ConfDefault.DefaultBoolean(false)
	boolean permitBlank();

	@ConfKey("default-reason:")
	@ConfComments("If the above is true, what is the default reason to use when staff do not specify a reason?")
	@ConfDefault.DefaultString("No reason stated.")
	String defaultReason();

}
