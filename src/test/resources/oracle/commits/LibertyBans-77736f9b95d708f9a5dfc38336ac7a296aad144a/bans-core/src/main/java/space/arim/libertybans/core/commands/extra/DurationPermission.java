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

import java.time.Duration;
import java.util.Objects;

public final class DurationPermission {

	private transient final String value;
	private final Duration duration;

	DurationPermission(String value, Duration duration) {
		if (duration.isNegative()) { // implicit null check
			throw new IllegalArgumentException("Duration cannot be negative");
		}
		this.value = Objects.requireNonNull(value);
		this.duration = duration;
	}

	String value() {
		return value;
	}

	String permission(PunishmentType type) {
		return "libertybans.dur." + type + "." + value;
	}

	Duration duration() {
		return duration;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DurationPermission that = (DurationPermission) o;
		return duration.equals(that.duration);
	}

	@Override
	public int hashCode() {
		return duration.hashCode();
	}

	@Override
	public String toString() {
		return "DurationPermission{" +
				"value='" + value + '\'' +
				", duration=" + duration +
				'}';
	}
}
