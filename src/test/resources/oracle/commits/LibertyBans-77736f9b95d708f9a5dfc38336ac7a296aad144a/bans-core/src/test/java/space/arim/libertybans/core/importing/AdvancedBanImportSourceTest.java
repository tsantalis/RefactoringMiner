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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.api.util.web.UUIDUtil;
import space.arim.jdbcaesar.JdbCaesar;
import space.arim.jdbcaesar.JdbCaesarBuilder;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.scope.ScopeImpl;

import javax.sql.DataSource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(LocalDatabaseSetup.class)
@LocalDatabaseSetup.Hsqldb
public class AdvancedBanImportSourceTest {

	private AdvancedBanImportSource importSource;
	private JdbCaesar jdbCaesar;

	private ServerScope globalScope = ScopeImpl.GLOBAL;

	@BeforeEach
	public void setup(ConnectionSource connectionSource) throws SQLException {
		ScopeManager scopeManager = mock(ScopeManager.class);
		globalScope = mock(ServerScope.class);
		lenient().when(scopeManager.globalScope()).thenReturn(globalScope);

		ImportConfig importConfig = mock(ImportConfig.class);
		ImportConfig.AdvancedBanSettings advancedBanSettings = mock(ImportConfig.AdvancedBanSettings.class);
		when(importConfig.retrievalSize()).thenReturn(200);
		when(importConfig.advancedBan()).thenReturn(advancedBanSettings);
		when(advancedBanSettings.toConnectionSource()).thenReturn(connectionSource);

		importSource = new AdvancedBanImportSource(importConfig, scopeManager);

		DataSource dataSource = mock(DataSource.class);
		lenient().when(dataSource.getConnection()).thenAnswer((invocation) -> connectionSource.openConnection());
		jdbCaesar = new JdbCaesarBuilder().dataSource(dataSource).build();

		new Schemas(jdbCaesar).setupAdvancedBan();
	}

	private void insertAdvancedBan(String table,
								   String name, String uuid, String reason, String operator,
								   String punishmentType, long startTime, long endTime) {
		jdbCaesar.query(
				"INSERT INTO " + table + " " +
						"(name, uuid, reason, operator, punishmentType, start, end, calculation) " +
						"VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
				.params(name, uuid, reason, operator, punishmentType, startTime, endTime, null)
				.voidResult().execute();
	}

	private void insertActiveNoCopyToHistory(String name, String uuid, String reason, String operator,
											 String punishmentType, long startTime, long endTime) {
		insertAdvancedBan("Punishments", name, uuid, reason, operator, punishmentType, startTime, endTime);
	}

	private void insertHistory(String name, String uuid, String reason, String operator,
							   String punishmentType, long startTime, long endTime) {
		insertAdvancedBan("PunishmentHistory", name, uuid, reason, operator, punishmentType, startTime, endTime);
	}

	private void insertActive(String name, String uuid, String reason, String operator,
							  String punishmentType, long startTime, long endTime) {
		insertHistory(name, uuid, reason, operator, punishmentType, startTime, endTime);
		insertActiveNoCopyToHistory(name, uuid, reason, operator, punishmentType, startTime, endTime);
	}

	private Set<PortablePunishment> sourcePunishments() {
		return importSource.sourcePunishments().collect(Collectors.toUnmodifiableSet());
	}

	@Test
	public void empty() {
		assertEquals(Set.of(), sourcePunishments());
	}

	@ParameterizedTest
	@ValueSource(strings= {"BAN", "TEMP_BAN", "IP_BAN"})
	public void activeReplacesHistorical(String punishmentType) {
		String uuidString = "394aef836e3f482b9988933cabe3b1cd";
		UUID uuid = UUIDUtil.fromShortString(uuidString);
		long startTime = System.currentTimeMillis();
		long endTime = startTime - 1000L;
		insertHistory(
				"creeperfreddy", uuidString, "bug exploit",
				"CONSOLE", punishmentType, startTime, endTime);

		PortablePunishment historicalPunishment = new PortablePunishment(0,
				new PortablePunishment.KnownDetails(
						PunishmentType.BAN, "bug exploit", globalScope,
						startTime / 1_000L, endTime / 1_000L),
				new PortablePunishment.VictimInfo(uuid, "creeperfreddy", null),
				new PortablePunishment.OperatorInfo(true, null, null),
				false);
		assertEquals(Set.of(historicalPunishment), sourcePunishments());

		/*
		 * Insert identical active punishment, and verify replacement in stream
		 */

		insertActiveNoCopyToHistory(
				"creeperfreddy", uuidString, "bug exploit",
				"CONSOLE", punishmentType, startTime, endTime);

		PortablePunishment activePunishment = new PortablePunishment(0,
				historicalPunishment.knownDetails(),
				historicalPunishment.victimInfo(),
				historicalPunishment.operatorInfo(),
				true);
		assertEquals(Set.of(activePunishment), sourcePunishments());
	}

	@Test
	public void skipNotes() {
		String uuidString = "00140fe8de0841e9ab79e53b9f3f0fbd";
		long startTime = System.currentTimeMillis();
		long endTime = startTime - 1000L;
		insertActive(
				"Ecotastic", uuidString, "noteworthy",
				"A248", "NOTE", startTime, endTime);
		assertEquals(Set.of(), sourcePunishments());
	}

	@Test
	public void missingUniqueId() {
		insertActive("Zalliah", null, "Banned for using Kill Aura",
				"Ecotastic", "BAN", 0, System.currentTimeMillis());
		assertThrows(ThirdPartyCorruptDataException.class, this::sourcePunishments);
	}

	@Test
	public void ipBan() throws UnknownHostException {
		String victimName = "MrBunnyBear";
		String victimIp = "195.140.213.201";
		long startTime = 1593013804802L;
		insertActive(victimName, victimIp, "No reason stated.",
				"Ecotastic", "IP_BAN", startTime, -1L);
		PortablePunishment expectedPunishment = new PortablePunishment(0,
				new PortablePunishment.KnownDetails(
						PunishmentType.BAN, "No reason stated.", globalScope,
						startTime / 1_000L, 0L),
				new PortablePunishment.VictimInfo(
						null, victimName, NetworkAddress.of(InetAddress.getByName(victimIp))),
				new PortablePunishment.OperatorInfo(
						false, null, "Ecotastic"),
				true);
		assertEquals(Set.of(expectedPunishment), sourcePunishments());
	}

}
