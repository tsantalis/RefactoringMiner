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

package space.arim.libertybans.core.service;

import java.time.Clock;
import java.time.Instant;

/**
 * Timer which is very similar to {@link Clock}
 *
 */
public abstract class Time {

	/**
	 * Retrieves the current time in seconds
	 *
	 * @return the current time
	 */
	public abstract long currentTime();

	/**
	 * Gets a live timer. Commonly used during normal operation
	 *
	 * @return a live timer
	 */
	public static Time live() {
		return Live.INSTANCE;
	}

	/**
	 * Gets a fixed timer. Useful for testing
	 *
	 * @param instant the fixed timestamp
	 * @return a fixed timer
	 */
	public static Time fixed(Instant instant) {
		return new Fixed(instant.getEpochSecond());
	}

	private static final class Live extends Time {

		static final Live INSTANCE = new Live();
		private Live() {}

		@Override
		public long currentTime() {
			return System.currentTimeMillis() / 1_000L;
		}

		@Override
		public String toString() {
			return "Live.INSTANCE";
		}
	}

	private static class Fixed extends Time {

		private final long timestamp;

		Fixed(long timestamp) {
			this.timestamp = timestamp;
		}

		@Override
		public long currentTime() {
			return timestamp;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Fixed fixed = (Fixed) o;
			return timestamp == fixed.timestamp;
		}

		@Override
		public int hashCode() {
			return (int) (timestamp ^ (timestamp >>> 32));
		}

		@Override
		public String toString() {
			return "Fixed{" +
					"timestamp=" + timestamp +
					'}';
		}
	}
}
