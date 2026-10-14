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
import space.arim.api.util.web.UUIDUtil;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.core.config.Configs;
import space.arim.omnibus.util.ThisClass;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class AdvancedBanImportSource implements ImportSource {

	private final ImportConfig config;
	private final ScopeManager scopeManager;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public AdvancedBanImportSource(Configs configs, ScopeManager scopeManager) {
		this(configs.getImportConfig(), scopeManager);
	}

	AdvancedBanImportSource(ImportConfig config, ScopeManager scopeManager) {
		this.config = config;
		this.scopeManager = scopeManager;
	}

	@Override
	public Stream<PortablePunishment> sourcePunishments() {
		/*
		 * Filter historical punishments with same ID as active punishments, in order
		 * to remove duplicates.
		 */
		Set<Integer> advancedBanIds = new HashSet<>();
		return unfilteredPunishments().filter((portablePunishment) -> {
			Integer id = portablePunishment.foreignId()
					.orElseThrow(() -> new ImportException("Every advancedban punishment should have an ID"));
			// Despite being a stateful predicate, this is safe because the stream is not parallel
			return advancedBanIds.add(id);
		});
	}

	private Stream<PortablePunishment> unfilteredPunishments() {
		DatabaseStream databaseStream = new DatabaseStream(
				config.advancedBan().toConnectionSource(), config.retrievalSize());
		return Stream.of(new RowMapper(true), new RowMapper(false)).flatMap(databaseStream::streamRows);
	}

	private class RowMapper implements SchemaRowMapper<PortablePunishment> {

		private final boolean active;

		RowMapper(boolean active) {
			this.active = active;
		}

		@Override
		public String selectStatement() {
			return "SELECT * FROM " + ((active) ? "Punishments" : "PunishmentHistory");
		}

		@Override
		public Optional<PortablePunishment> mapRow(ResultSet resultSet) throws SQLException {
			return mapKnownDetails(resultSet).map((knownDetails) -> {
				try {
					Integer id = resultSet.getInt("id");
					logger.trace("Mapping row with AdvancedBan punishment ID {}", id);
					return new PortablePunishment(
							id,
							knownDetails,
							mapVictimInfo(resultSet),
							mapOperatorInfo(resultSet),
							active);
				} catch (SQLException ex) {
					throw new ImportException(ex);
				}
			});
		}

		/*
		 * Nearly every column retrieval has to account for the possibility of NULL values,
		 * because AdvancedBan's schema has incredibly poor integrity.
		 * ThirdPartyCorruptDataException is thrown in these cases.
		 */

		private Optional<PortablePunishment.KnownDetails> mapKnownDetails(ResultSet resultSet) throws SQLException {
			String punishmentType = getNonnullString(resultSet, "punishmentType");
			AdvancedBanPunishmentType advancedBanPunishmentType;
			try {
				advancedBanPunishmentType = AdvancedBanPunishmentType.valueOf(punishmentType);
			} catch (IllegalArgumentException typeNotRecognised) {

				// LibertyBans does not have AdvancedBan's NOTE
				// OR AdvancedBan has introduced another punishment type
				logger.info("Skipping punishment type {} which is not supported by LibertyBans", punishmentType);
				return Optional.empty();
			}
			// AdvancedBan's start and end times have milliseconds precision
			long start = resultSet.getLong("start") / 1_000L;
			if (start == 0L) { // SQL NULL -> 0L
				throw nullColumn("start");
			}

			long advancedBanEnd = resultSet.getLong("end");
			if (advancedBanEnd == 0L) { // SQL NULL -> 0L
				throw nullColumn("end");
			}
			// AdvancedBan uses -1 for permanent punishments
			long end = (advancedBanEnd == -1L) ? 0L : advancedBanEnd / 1_000L;

			String reason = getNonnullString(resultSet, "reason");
			return Optional.of(new PortablePunishment.KnownDetails(
					advancedBanPunishmentType.type,
					reason,
					scopeManager.globalScope(), // AdvancedBan does not support scopes
					start, end));
		}

		private PortablePunishment.VictimInfo mapVictimInfo(ResultSet resultSet) throws SQLException {
			// Despite the AdvancedBan column name 'uuid', this can also be an IP address
			String victimId = getNonnullString(resultSet, "uuid");
			// AdvancedBan uses trimmed UUIDs, which are always of length 32
			boolean isUuid = victimId.length() == 32;
			NetworkAddress address;
			if (isUuid) {
				address = null;
			} else {
				try {
					address = NetworkAddress.of(InetAddress.getByName(victimId));
				} catch (UnknownHostException ex) {
					throw new ImportException("Unable to parse AdvancedBan IP address", ex);
				}
			}
			String name = getNonnullString(resultSet, "name");
			return new PortablePunishment.VictimInfo(
					(isUuid) ? UUIDUtil.fromShortString(victimId) : null, name, address);
		}

		private PortablePunishment.OperatorInfo mapOperatorInfo(ResultSet resultSet) throws SQLException {
			String operatorName = getNonnullString(resultSet, "operator");
			if (operatorName.equals("CONSOLE")) {
				return new PortablePunishment.OperatorInfo(true, null, null);
			}
			return new PortablePunishment.OperatorInfo(false, null, operatorName);
		}
	}

	private static String getNonnullString(ResultSet resultSet, String column) throws SQLException {
		String value = resultSet.getString(column);
		if (value == null) {
			throw nullColumn(column);
		}
		return value;
	}

	private static ThirdPartyCorruptDataException nullColumn(String column) {
		return new ThirdPartyCorruptDataException("Value of column " + column + " is null");
	}

	private enum AdvancedBanPunishmentType {
		BAN(PunishmentType.BAN),
		TEMP_BAN(PunishmentType.BAN),
		IP_BAN(PunishmentType.BAN),
		TEMP_IP_BAN(PunishmentType.BAN),
		MUTE(PunishmentType.MUTE),
		TEMP_MUTE(PunishmentType.MUTE),
		WARNING(PunishmentType.WARN),
		TEMP_WARNING(PunishmentType.WARN),
		KICK(PunishmentType.KICK)
		// NOTE(null); - purposefully omitted; see above
		;

		final PunishmentType type;

		private AdvancedBanPunishmentType(PunishmentType type) {
			this.type = type;
		}
	}

}
