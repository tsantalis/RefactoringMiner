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

package space.arim.libertybans.core.config;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.UUID;

final class DisplayableVictim {

	private final Victim victim;
	private final String name;

	static final DisplayableVictim ObWolf = player(UUID.randomUUID(),
			"ObsidianWolf_");
	static final DisplayableVictim Address_198_27_31_42;

	static {
		try {
			Address_198_27_31_42 = address(NetworkAddress.of(
					InetAddress.getByName("198.27.31.42")));
		} catch (UnknownHostException ex) {
			throw new RuntimeException(ex);
		}
	}

	DisplayableVictim(Victim victim, String name) {
		this.victim = victim;
		this.name = name;
	}

	public String display() {
		if (this == ObWolf) {
			return "ObsidianWolf_";
		}
		assert this == Address_198_27_31_42;
		return "198.27.31.42";
	}

	public String displayId() {
		if (this == ObWolf) {
			return ((PlayerVictim) victim).getUUID().toString().replace("-", "");
		}
		assert this == Address_198_27_31_42;
		return "198.27.31.42";
	}

	private static DisplayableVictim player(UUID uuid, String name) {
		return new DisplayableVictim(PlayerVictim.of(uuid), name);
	}

	private static DisplayableVictim address(NetworkAddress address) {
		return new DisplayableVictim(AddressVictim.of(address), null);
	}

	public Victim victim() {
		return victim;
	}

	public String name() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DisplayableVictim that = (DisplayableVictim) o;
		return victim.equals(that.victim) && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		int result = victim.hashCode();
		result = 31 * result + (name != null ? name.hashCode() : 0);
		return result;
	}
}
