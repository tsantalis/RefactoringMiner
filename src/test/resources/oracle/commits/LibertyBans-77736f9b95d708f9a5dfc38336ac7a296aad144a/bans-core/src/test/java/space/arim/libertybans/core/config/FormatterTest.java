
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

package space.arim.libertybans.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.api.chat.manipulator.SendableMessageManipulator;
import space.arim.api.chat.serialiser.SimpleTextSerialiser;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.service.Time;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FormatterTest {

	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();
	private final Configs configs;
	private final InternalScopeManager scopeManager;
	private final UUIDManager uuidManager;

	private final MessagesConfig messagesConfig;
	private final ServerScope globalScope;

	private final Formatter formatter;

	/**
	 * 1 January 2021
	 */
	private static final Instant INSTANT_2021_01_01 = Instant.ofEpochSecond(1609459200L);
	/**
	 * The current time used by the test; 5 January 2021
	 */
	private static final Instant INSTANT_2021_01_05 = INSTANT_2021_01_01.plus(Duration.ofDays(4L));

	public FormatterTest(@Mock Configs configs, @Mock InternalScopeManager scopeManager,
						 @Mock UUIDManager uuidManager, @Mock MessagesConfig messagesConfig,
						 @Mock ServerScope globalScope) {
		this.configs = configs;
		this.scopeManager = scopeManager;
		this.uuidManager = uuidManager;
		this.messagesConfig = messagesConfig;
		this.globalScope = globalScope;

		formatter = new Formatter(futuresFactory, configs, scopeManager, uuidManager,
				Time.fixed(INSTANT_2021_01_05));
	}

	@BeforeEach
	public void setupMocks() {
		when(configs.getMessagesConfig()).thenReturn(messagesConfig);

		lenient().when(scopeManager.globalScope()).thenReturn(globalScope);
		lenient().when(scopeManager.getServer(same(globalScope), any())).thenAnswer(
				invocationOnMock -> invocationOnMock.getArgument(1, String.class));
	}

	private <T> CentralisedFuture<T> completedFuture(T value) {
		return futuresFactory.completedFuture(value);
	}

	private void setTimeConf(MessagesConfig.Misc.Time timeConf) {
		MessagesConfig.Misc messagesConfigMisc = mock(MessagesConfig.Misc.class);
		when(messagesConfig.misc()).thenReturn(messagesConfigMisc);
		when(messagesConfigMisc.time()).thenReturn(timeConf);
	}

	private MessagesConfig.Misc.Time simpleTimeConf() {
		MessagesConfig.Misc.Time timeConf = mock(MessagesConfig.Misc.Time.class);
		when(timeConf.fragments()).thenReturn(Map.of(
				ChronoUnit.SECONDS, "%VALUE% seconds",
				ChronoUnit.MINUTES, "%VALUE% minutes",
				ChronoUnit.HOURS, "%VALUE% hours",
				ChronoUnit.DAYS, "%VALUE% days"));
		lenient().when(timeConf.useComma()).thenReturn(true);
		lenient().when(timeConf.and()).thenReturn("and ");
		lenient().when(timeConf.fallbackSeconds()).thenReturn("%VALUE% seconds");
		return timeConf;
	}

	@Test
	public void formatRelative() {
		setTimeConf(simpleTimeConf());

		assertEquals("3 minutes, and 5 seconds",
				formatter.formatRelative(3L * 60L + 5L));
		assertEquals("4 hours, 15 minutes, and 58 seconds",
				formatter.formatRelative(4L * 60L * 60L + 15L * 60L + 58L));
		assertEquals("2 days, 4 minutes, and 40 seconds",
				formatter.formatRelative(2L * 24L * 60L * 60L + 4L * 60L + 40L));
	}

	private void setSimpleDateFormatting() {
		MainConfig mainConfig = mock(MainConfig.class);
		MainConfig.DateFormatting dateFormatting = mock(MainConfig.DateFormatting.class);

		when(configs.getMainConfig()).thenReturn(mainConfig);
		when(mainConfig.dateFormatting()).thenReturn(dateFormatting);
		when(dateFormatting.formatAndPattern()).thenReturn(new DateTimeFormatterWithPattern("dd/MM/yyyy kk:mm"));
		when(dateFormatting.zoneId()).thenReturn(ZoneOffset.UTC);
	}

	private void setSimpleMessagesFormatting() {
		MessagesConfig.Formatting formatting = mock(MessagesConfig.Formatting.class);
		when(messagesConfig.formatting()).thenReturn(formatting);
		lenient().when(formatting.consoleDisplay()).thenReturn("Console");
		when(formatting.globalScopeDisplay()).thenReturn("global");
		when(formatting.punishmentTypeDisplay()).thenReturn(Map.of());
	}

	private void setupSimpleDefaults() {
		setTimeConf(simpleTimeConf());
		setSimpleDateFormatting();
		setSimpleMessagesFormatting();
	}

	@ParameterizedTest
	@ArgumentsSource(FormatterTestArgumentsProvider.class)
	public void formatPunishment(FormatterTestInfo testInfo) {
		setupSimpleDefaults();

		Instant start = INSTANT_2021_01_01;
		Instant end = INSTANT_2021_01_05.plus(Duration.ofHours(2L)).plus(Duration.ofMinutes(15L));
		Punishment punishment = punishmentFor(testInfo, start, end);

		String layout = "%TYPE% > %OPERATOR% enacted against %VICTIM% for %DURATION% due to %REASON%. " +
				"Starts on %START_DATE%. Ends on %END_DATE%. Remaining time is %TIME_REMAINING%. " +
				"Time passed is %TIME_PASSED%. Operator ID is %OPERATOR_ID%; Victim ID is %VICTIM_ID%.";
		String expectedFormat = testInfo.formatVariables(layout)
				.replace("%DURATION%", "4 days, 2 hours, and 15 minutes")
				.replace("%TIME_REMAINING%", "2 hours, and 15 minutes")
				.replace("%START_DATE%", "01/01/2021 24:00")
				.replace("%END_DATE%", "05/01/2021 02:15")
				.replace("%TIME_PASSED%", "4 days");

		assertEquals(expectedFormat, format(punishment, layout));
	}

	/**
	 * Tests the "29 days, 23 hours, 59 minutes" issue. The current solution uses a
	 * 'margin of initialization' under which the time remaining is considered equal
	 * to the punishment duration.
	 */
	@Test
	public void marginOfInitialization() {
		setupSimpleDefaults();

		FormatterTestInfo testInfo = new FormatterTestInfo(
				PunishmentType.BAN,
				DisplayableVictim.ObWolf, DisplayableOperator.CONSOLE,
				"global", "despite being banned, you should see a good time display");

		Instant start = INSTANT_2021_01_05.minus(Duration.ofSeconds(1L));
		Instant end = start.plus(Duration.ofHours(3L));
		Punishment punishment = punishmentFor(testInfo, start, end);

		String layout = "%TYPE% > %OPERATOR% enacted against %VICTIM% for %DURATION% due to %REASON%. " +
				"Starts on %START_DATE%. Ends on %END_DATE%. Remaining time is %TIME_REMAINING%. " +
				"Time passed is %TIME_PASSED%. Operator ID is %OPERATOR_ID%; Victim ID is %VICTIM_ID%.";
		String expectedFormat = testInfo.formatVariables(layout)
				.replace("%DURATION%", "3 hours")
				.replace("%TIME_REMAINING%", "3 hours")
				.replace("%START_DATE%", "04/01/2021 23:59")
				.replace("%END_DATE%", "05/01/2021 02:59")
				.replace("%TIME_PASSED%", "1 seconds");

		assertEquals(expectedFormat, format(punishment, layout));
	}

	private String format(Punishment punishment, String layout) {
		var layoutMessage = SimpleTextSerialiser.getInstance().deserialise(layout);
		var layoutMessageManipulator = SendableMessageManipulator.create(layoutMessage);
		var formatFuture = formatter.formatWithPunishment(layoutMessageManipulator, punishment);
		return SimpleTextSerialiser.getInstance().serialise(formatFuture.join());
	}

	private Punishment punishmentFor(FormatterTestInfo testInfo, Instant start, Instant end) {
		Punishment punishment = mock(Punishment.class);
		when(punishment.getType()).thenReturn(testInfo.type());

		setVictim(punishment, testInfo.victim());
		setOperator(punishment, testInfo.operator());

		when(punishment.getReason()).thenReturn(testInfo.reason());

		// Scope
		String serverScope = testInfo.serverScope();
		if (serverScope.isEmpty()) {
			when(punishment.getScope()).thenReturn(globalScope);
		} else {
			ServerScope scope = specificScope(serverScope);
			when(punishment.getScope()).thenReturn(scope);
		}

		// Start and end
		setStartAndEnd(punishment, start, end);

		return punishment;
	}

	private ServerScope specificScope(String server) {
		ServerScope scope = mock(ServerScope.class);
		when(scopeManager.getServer(same(scope), any())).thenReturn(server);
		return scope;
	}

	private void setVictim(Punishment punishment, DisplayableVictim displayableVictim) {
		Victim victim = displayableVictim.victim();
		when(punishment.getVictim()).thenReturn(victim);
		if (victim instanceof PlayerVictim) {
			when(uuidManager.lookupName(((PlayerVictim) victim).getUUID()))
					.thenReturn(completedFuture(Optional.of(displayableVictim.name())));
		}
	}

	private void setOperator(Punishment punishment, DisplayableOperator displayableOperator) {
		Operator operator = displayableOperator.operator();
		when(punishment.getOperator()).thenReturn(operator);
		if (operator instanceof PlayerOperator) {
			when(uuidManager.lookupName(((PlayerOperator) operator).getUUID()))
					.thenReturn(completedFuture(Optional.of(displayableOperator.name())));
		}
	}

	private void setStartAndEnd(Punishment punishment, Instant start, Instant end) {
		when(punishment.getStartDate()).thenReturn(start);
		when(punishment.getEndDate()).thenReturn(end);
		when(punishment.getStartDateSeconds()).thenReturn(start.getEpochSecond());
		when(punishment.getEndDateSeconds()).thenReturn(end.getEpochSecond());
		when(punishment.isPermanent()).thenReturn(end.equals(Instant.MAX));
	}
}
