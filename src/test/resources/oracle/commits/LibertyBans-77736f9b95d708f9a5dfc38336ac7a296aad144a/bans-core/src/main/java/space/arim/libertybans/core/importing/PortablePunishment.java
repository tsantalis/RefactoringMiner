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

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.scope.ServerScope;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PortablePunishment {

	private final Integer foreignId;
	private final KnownDetails knownDetails;
	private final VictimInfo victimInfo;
	private final OperatorInfo operatorInfo;
	private final boolean active;

	public PortablePunishment(Integer foreignId, KnownDetails knownDetails,
							  VictimInfo victimInfo, OperatorInfo operatorInfo, boolean active) {
		this.foreignId = foreignId;
		this.knownDetails = Objects.requireNonNull(knownDetails);
		this.victimInfo = Objects.requireNonNull(victimInfo);
		this.operatorInfo = Objects.requireNonNull(operatorInfo);
		this.active = active;
	}

	public Optional<Integer> foreignId() {
		return Optional.ofNullable(foreignId);
	}

	public KnownDetails knownDetails() {
		return knownDetails;
	}

	public VictimInfo victimInfo() {
		return victimInfo;
	}

	public OperatorInfo operatorInfo() {
		return operatorInfo;
	}

	public boolean active() {
		return active;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PortablePunishment that = (PortablePunishment) o;
		return Objects.equals(foreignId, that.foreignId)
				&& knownDetails.equals(that.knownDetails)
				&& victimInfo.equals(that.victimInfo)
				&& operatorInfo.equals(that.operatorInfo)
				&& active == that.active;
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode(foreignId);
		result = 31 * result + knownDetails.hashCode();
		result = 31 * result + victimInfo.hashCode();
		result = 31 * result + operatorInfo.hashCode();
		result = 31 * result + (active ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		return "PortablePunishment{" +
				"foreignId=" + foreignId +
				", knownDetails=" + knownDetails +
				", victimInfo=" + victimInfo +
				", operatorInfo=" + operatorInfo +
				", active=" + active +
				'}';
	}

	public static final class KnownDetails {

		private final PunishmentType type;
		private final String reason;
		private final ServerScope scope;
		private final long start;
		private final long end;

		public KnownDetails(PunishmentType type, String reason, ServerScope scope, long start, long end) {
			this.type = Objects.requireNonNull(type);
			this.reason = Objects.requireNonNull(reason);
			this.scope = Objects.requireNonNull(scope);
			this.start = start;
			this.end = end;
		}

		public PunishmentType type() {
			return type;
		}

		public String reason() {
			return reason;
		}

		public ServerScope scope() {
			return scope;
		}

		public long start() {
			return start;
		}

		public long end() {
			return end;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			KnownDetails that = (KnownDetails) o;
			return type == that.type
					&& reason.equals(that.reason)
					&& scope.equals(that.scope)
					&& start == that.start && end == that.end;
		}

		@Override
		public int hashCode() {
			int result = type.hashCode();
			result = 31 * result + reason.hashCode();
			result = 31 * result + scope.hashCode();
			result = 31 * result + (int) (start ^ (start >>> 32));
			result = 31 * result + (int) (end ^ (end >>> 32));
			return result;
		}

		@Override
		public String toString() {
			return "KnownDetails{" +
					"type=" + type +
					", reason='" + reason + '\'' +
					", scope=" + scope +
					", start=" + start +
					", end=" + end +
					'}';
		}
	}

	public static final class VictimInfo {

		private final UUID uuid;
		private final String name;
		private final NetworkAddress address;
		private final Victim overrideVictim;

		public VictimInfo(UUID uuid, String name, NetworkAddress address) {
			this(uuid, name, address, null);
		}

		public VictimInfo(UUID uuid, String name, NetworkAddress address, Victim overrideVictim) {
			if (uuid == null && name == null && address == null) {
				throw new IllegalArgumentException("One of uuid, username, or address must be nonnull");
			}
			this.uuid = uuid;
			this.name = name;
			this.address = address;
			this.overrideVictim = overrideVictim;
		}

		public Optional<UUID> uuid() {
			return Optional.ofNullable(uuid);
		}

		public Optional<String> name() {
			return Optional.ofNullable(name);
		}

		public Optional<NetworkAddress> address() {
			return Optional.ofNullable(address);
		}

		public Optional<Victim> overrideVictim() {
			return Optional.ofNullable(overrideVictim);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			VictimInfo that = (VictimInfo) o;
			if (overrideVictim != null) {
				return overrideVictim.equals(that.overrideVictim);
			}
			if (uuid != null) {
				return uuid.equals(that.uuid);
			}
			if (address != null) {
				return address.equals(that.address);
			}
			return name.equals(that.name);
		}

		@Override
		public int hashCode() {
			if (overrideVictim != null) {
				return 31 + overrideVictim.hashCode();
			}
			if (uuid != null) {
				return 31 + uuid.hashCode();
			}
			if (address != null) {
				return 31 + address.hashCode();
			}
			return 31 + name.hashCode();
		}

		@Override
		public String toString() {
			return "VictimInfo{" +
					"uuid=" + uuid +
					", name='" + name + '\'' +
					", address=" + address +
					", overrideVictim=" + overrideVictim +
					'}';
		}
	}

	public static final class OperatorInfo {

		private final boolean console;
		private final UUID uuid;
		private final String name;

		public OperatorInfo(boolean console, UUID uuid, String name) {
			if (!console && uuid == null & name == null) {
				throw new IllegalArgumentException("If !console, one of uuid or name must be nonnull");
			}
			this.console = console;
			this.uuid = uuid;
			this.name = name;
		}

		public boolean console() {
			return console;
		}

		public Optional<UUID> uuid() {
			return Optional.ofNullable(uuid);
		}

		public Optional<String> name() {
			return Optional.ofNullable(name);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			OperatorInfo that = (OperatorInfo) o;
			if (console) {
				return that.console;
			}
			if (uuid != null) {
				return uuid.equals(that.uuid);
			}
			return name.equals(that.name);
		}

		@Override
		public int hashCode() {
			if (console) {
				return 1;
			}
			if (uuid != null) {
				return 31 + uuid.hashCode();
			}
			return 31 + name.hashCode();
		}

		@Override
		public String toString() {
			return "OperatorInfo{" +
					"console=" + console +
					", uuid=" + uuid +
					", name='" + name + '\'' +
					'}';
		}
	}
}
