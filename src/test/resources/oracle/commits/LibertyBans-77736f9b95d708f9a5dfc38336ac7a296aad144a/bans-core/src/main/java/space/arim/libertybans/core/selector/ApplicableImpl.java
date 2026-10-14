/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.selector;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import space.arim.libertybans.core.punish.Association;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.libertybans.core.punish.PunishmentCreator;

@Singleton
public class ApplicableImpl {

	private final Configs configs;
	private final FactoryOfTheFuture futuresFactory;
	private final Provider<InternalDatabase> dbProvider;
	private final PunishmentCreator creator;
	
	@Inject
	public ApplicableImpl(Configs configs, FactoryOfTheFuture futuresFactory, Provider<InternalDatabase> dbProvider,
			PunishmentCreator creator) {
		this.configs = configs;
		this.futuresFactory = futuresFactory;
		this.dbProvider = dbProvider;
		this.creator = creator;
	}
	
	private Map.Entry<String, Object[]> getApplicabilityQuery(UUID uuid, NetworkAddress address, PunishmentType type) {
		String statement;
		Object[] args;

		long currentTime = MiscUtil.currentTime();
		AddressStrictness strictness = configs.getMainConfig().enforcement().addressStrictness();
		switch (strictness) {
		case LENIENT:
			statement = "SELECT `id`, `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end` "
					+ "FROM `libertybans_simple_" + type + "s` "
					+ "WHERE (`end` = 0 OR `end` > ?) AND `type` = ? "
					+ "AND ((`victim_type` = 'PLAYER' AND `victim` = ?) OR (`victim_type` = 'ADDRESS' AND `victim` = ?))";
			args = new Object[] {currentTime, type, uuid, address};
			break;
		case NORMAL:
			statement = "SELECT `id`, `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end`, `uuid` "
					+ "FROM `libertybans_applicable_" + type + "s` "
					+ "WHERE (`end` = 0 OR `end` > ?) AND `type` = ? AND `uuid` = ?";
			args = new Object[] {currentTime, type, uuid};
			break;
		case STRICT:
			statement = "SELECT `id`, `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end`, `uuid` "
					+ "FROM `libertybans_applicable_" + type + "s` `appl` INNER JOIN `libertybans_strict_links` `links` "
					+ "ON `appl`.`uuid` = `links`.`uuid1` "
					+ "WHERE (`end` = 0 OR `end` > ?) AND `type` = ? AND `links`.`uuid2` = ?";
			args = new Object[] {currentTime, type, uuid};
			break;
		default:
			throw MiscUtil.unknownAddressStrictness(strictness);
		}
		return Map.entry(statement, args);
	}
	
	CentralisedFuture<Punishment> executeAndCheckConnection(UUID uuid, String name, NetworkAddress address) {
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {

			Map.Entry<String, Object[]> query = getApplicabilityQuery(uuid, address, PunishmentType.BAN);
			long currentTime = MiscUtil.currentTime();

			return database.jdbCaesar().transaction().body((querySource, controller) -> {

				Association association = new Association(uuid, querySource);
				association.associateCurrentName(name, currentTime);
				association.associateCurrentAddress(address, currentTime);

				return querySource.query(
						query.getKey())
						.params(query.getValue())
						.singleResult((resultSet) -> {
							return creator.createPunishment(resultSet.getInt("id"), PunishmentType.BAN,
									database.getVictimFromResult(resultSet), database.getOperatorFromResult(resultSet),
									database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
									database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
						}).execute();
			}).execute();
		});
	}
	
	private CentralisedFuture<Punishment> getApplicablePunishment0(UUID uuid, NetworkAddress address, PunishmentType type) {
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {

			Map.Entry<String, Object[]> query = getApplicabilityQuery(uuid, address, type);
			return database.jdbCaesar().query(
					query.getKey())
					.params(query.getValue())
					.singleResult((resultSet) -> {
						return creator.createPunishment(resultSet.getInt("id"), type,
								database.getVictimFromResult(resultSet), database.getOperatorFromResult(resultSet),
								database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
								database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
					}).execute();
		});
	}

	CentralisedFuture<Punishment> getApplicablePunishment(UUID uuid, NetworkAddress address, PunishmentType type) {
		Objects.requireNonNull(type, "type");
		if (type == PunishmentType.KICK) {
			// Kicks are never active
			return futuresFactory.completedFuture(null);
		}
		return getApplicablePunishment0(uuid, address, type);
	}
	
	CentralisedFuture<Punishment> getApplicableMute(UUID uuid, NetworkAddress address) {
		return getApplicablePunishment0(uuid, address, PunishmentType.MUTE);
	}

}
