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

import space.arim.dazzleconf.error.BadValueException;
import space.arim.dazzleconf.serialiser.Decomposer;
import space.arim.dazzleconf.serialiser.FlexibleType;
import space.arim.dazzleconf.serialiser.ValueSerialiser;

import java.time.Duration;

public class DurationPermissionSerialiser implements ValueSerialiser<DurationPermission> {

	@Override
	public Class<DurationPermission> getTargetClass() {
		return DurationPermission.class;
	}

	@Override
	public DurationPermission deserialise(FlexibleType flexibleType) throws BadValueException {
		String value = flexibleType.getString();
		Duration duration = new DurationParser().parse(value);
		if (duration.isNegative()) {
			throw flexibleType.badValueExceptionBuilder().message(value + " must be a valid duration").build();
		}
		return new DurationPermission(value, duration);
	}

	@Override
	public Object serialise(DurationPermission value, Decomposer decomposer) {
		return value.value();
	}
}
