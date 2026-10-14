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

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.config.Configs;
import space.arim.omnibus.util.ThisClass;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class LiteBansImportSource implements ImportSource {

	private final ImportConfig config;
	private final ScopeManager scopeManager;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public LiteBansImportSource(Configs configs, ScopeManager scopeManager) {
		this(configs.getImportConfig(), scopeManager);
	}

	LiteBansImportSource(ImportConfig config, ScopeManager scopeManager) {
		this.config = config;
		this.scopeManager = scopeManager;
	}

	@Override
	public Stream<PortablePunishment> sourcePunishments() {
		DatabaseStream databaseStream = new DatabaseStream(
				config.litebans().toConnectionSource(), config.retrievalSize());
		return Arrays.stream(LiteBansTable.values()).map(RowMapper::new).flatMap(databaseStream::streamRows);
	}

	private class RowMapper implements SchemaRowMapper<PortablePunishment> {

		private final LiteBansTable table;

		RowMapper(LiteBansTable table) {
			this.table = table;
		}

		@Override
		public String selectStatement() {
			return "SELECT * FROM " + config.litebans().tablePrefix() + table;
		}

		@Override
		public Optional<PortablePunishment> mapRow(ResultSet resultSet) throws SQLException {
			Integer id = resultSet.getInt("id");
			if (resultSet.getBoolean("ipban_wildcard")) {
				logger.info("Skipped LiteBans wildcard IP ban {} which is not supported by LibertyBans", id);
				return Optional.empty();
			}
			boolean active = table != LiteBansTable.KICKS && resultSet.getBoolean("active");
			logger.trace("Mapping row with LiteBans punishment ID {}", id);
			return Optional.of(new PortablePunishment(
					id,
					mapKnownDetails(resultSet),
					mapVictimInfo(resultSet),
					mapOperatorInfo(resultSet),
					active));
		}

		private PortablePunishment.KnownDetails mapKnownDetails(ResultSet resultSet) throws SQLException {
			String reason = resultSet.getString("reason");
			if (reason.length() > 256) {
				// LiteBans permits longer reasons
				reason = reason.substring(0, 256);
				logger.info("Trimmed reason of excessively long LiteBans punishment");
			}
			// LiteBans' start and end times have milliseconds precision
			long start = resultSet.getLong("time") / 1_000L;

			long liteBansUntil = resultSet.getLong("until");
			// Sometimes LiteBans uses 0 for permanent punishments, sometimes -1
			long end = (liteBansUntil == -1L) ? 0L : liteBansUntil / 1_000L;

			String liteBansScope = resultSet.getString("server_scope");
			ServerScope scope = (liteBansScope == null || liteBansScope.equals("*")) ?
					scopeManager.globalScope() : scopeManager.specificScope(liteBansScope);

			return new PortablePunishment.KnownDetails(table.type, reason, scope, start, end);
		}

		private PortablePunishment.VictimInfo mapVictimInfo(ResultSet resultSet) throws SQLException {
			String uuidString = getNonnullString(resultSet, "uuid");
			String ipString = getNonnullString(resultSet, "ip");
			UUID uuid = UUID.fromString(uuidString);
			NetworkAddress address;
			try {
				address = NetworkAddress.of(InetAddress.getByName(ipString));
			} catch (UnknownHostException ex) {
				throw new ImportException("Unable to parse LiteBans IP address", ex);
			}
			boolean ipban = resultSet.getBoolean("ipban");
			Victim victim = (ipban) ? AddressVictim.of(address) : PlayerVictim.of(uuid);
			return new PortablePunishment.VictimInfo(uuid, null, address, victim);
		}

		private PortablePunishment.OperatorInfo mapOperatorInfo(ResultSet resultSet) throws SQLException {
			String operatorId = getNonnullString(resultSet, "banned_by_uuid");
			if (operatorId.equals("CONSOLE")) {
				return new PortablePunishment.OperatorInfo(true, null, null);
			}
			return new PortablePunishment.OperatorInfo(false, UUID.fromString(operatorId),
					resultSet.getString("banned_by_name")); // okay if banned_by_name is null
		}

	}

	private static String getNonnullString(ResultSet resultSet, String column) throws SQLException {
		String value = resultSet.getString(column);
		if (value == null) {
			throw new ThirdPartyCorruptDataException("Value of column " + column + " is null");
		}
		return value;
	}

	private enum LiteBansTable {

		BANS(PunishmentType.BAN),
		MUTES(PunishmentType.MUTE),
		WARNINGS(PunishmentType.WARN),
		KICKS(PunishmentType.KICK);

		final PunishmentType type;

		private LiteBansTable(PunishmentType type) {
			this.type = type;
		}

		@Override
		public String toString() {
			return name().toLowerCase(Locale.ROOT);
		}
	}
}
