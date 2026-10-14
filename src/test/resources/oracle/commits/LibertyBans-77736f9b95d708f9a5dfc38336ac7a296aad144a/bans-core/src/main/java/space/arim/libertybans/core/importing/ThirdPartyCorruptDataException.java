/*
 * LibertyBans
 * Copyright © 2020 Anand Beh
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

package space.arim.libertybans.core.importing;

public class ThirdPartyCorruptDataException extends ImportException {

	private static final String PREFIX = "LibertyBans attempted to import data from another plugin, " +
			"but unexpectedly received malformed data. Reason: ";

	public ThirdPartyCorruptDataException(String message) {
		super(PREFIX + message);
	}

	public ThirdPartyCorruptDataException(Throwable cause) {
		super(PREFIX + "exception", cause);
	}

	public ThirdPartyCorruptDataException(String message, Throwable cause) {
		super(PREFIX + message, cause);
	}

}
