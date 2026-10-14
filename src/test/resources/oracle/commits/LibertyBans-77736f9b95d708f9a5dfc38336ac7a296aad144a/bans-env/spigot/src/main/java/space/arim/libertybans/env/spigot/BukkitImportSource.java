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

package space.arim.libertybans.env.spigot;

import jakarta.inject.Inject;
import org.bukkit.BanList;
import org.bukkit.Server;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.core.importing.ImportException;
import space.arim.libertybans.core.importing.PlatformImportSource;
import space.arim.libertybans.core.importing.PortablePunishment;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BukkitImportSource implements PlatformImportSource {

	private final FactoryOfTheFuture futuresFactory;
	private final ScopeManager scopeManager;
	private final Server server;

	@Inject
	public BukkitImportSource(FactoryOfTheFuture futuresFactory, ScopeManager scopeManager, Server server) {
		this.futuresFactory = futuresFactory;
		this.scopeManager = scopeManager;
		this.server = server;
	}

	@Override
	public Stream<PortablePunishment> sourcePunishments() {
		var namePunishments = sourcePunishmentsFrom(BanList.Type.NAME, (nameTarget) -> {
			return new PortablePunishment.VictimInfo(null, nameTarget, null);
		});
		var addressPunishments = sourcePunishmentsFrom(BanList.Type.IP, (addressTarget) -> {
			InetAddress address;
			try {
				address = InetAddress.getByName(addressTarget);
			} catch (UnknownHostException ex) {
				throw new ImportException("Unable to parse IP address " + addressTarget, ex);
			}
			return new PortablePunishment.VictimInfo(null, null, NetworkAddress.of(address));
		});
		return Stream.concat(namePunishments, addressPunishments);
	}

	private interface BanListTypeHelper {
		PortablePunishment.VictimInfo getVictim(String target);
	}

	/*
	 * Because the Bukkit API is not thread safe, the punishments must be eagerly collected
	 * on the main thread, then moved back to the calling thread.
	 */

	private Stream<PortablePunishment> sourcePunishmentsFrom(BanList.Type banListType, BanListTypeHelper helper) {
		return futuresFactory.supplySync(() -> sourcePunishmentsMainThread(banListType, helper)).join().stream();
	}

	private Set<PortablePunishment> sourcePunishmentsMainThread(BanList.Type banListType, BanListTypeHelper helper) {
		return server.getBanList(banListType).getBanEntries().stream().map((entry) -> {
			String reason = Optional.ofNullable(entry.getReason()).orElse("");
			long start = entry.getCreated().toInstant().getEpochSecond();
			long end = Optional.ofNullable(entry.getExpiration()).map((expiration) -> expiration.toInstant().getEpochSecond()).orElse(0L);
			return new PortablePunishment(
					null,
					new PortablePunishment.KnownDetails(
							PunishmentType.BAN, reason, scopeManager.globalScope(), start, end),
					helper.getVictim(Objects.requireNonNull(entry.getTarget())),
					new PortablePunishment.OperatorInfo(false, null, entry.getSource()),
					true);
		}).collect(Collectors.toUnmodifiableSet());
	}

}
