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
package space.arim.libertybans.core.config;

import jakarta.inject.Inject;
import space.arim.api.chat.SendableMessage;
import space.arim.api.chat.manipulator.SendableMessageManipulator;
import space.arim.api.chat.serialiser.JsonSkSerialiser;
import space.arim.api.util.web.UUIDUtil;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.service.Time;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.UnaryOperator;

public class Formatter implements InternalFormatter {

	private final FactoryOfTheFuture futuresFactory;
	private final Configs configs;
	private final InternalScopeManager scopeManager;
	private final UUIDManager uuidManager;
	private final Time time;

	private static final long MARGIN_OF_INITIATION = 10; // seconds
	
	@Inject
	public Formatter(FactoryOfTheFuture futuresFactory, Configs configs, InternalScopeManager scopeManager,
					 UUIDManager uuidManager, Time time) {
		this.futuresFactory = futuresFactory;
		this.configs = configs;
		this.scopeManager = scopeManager;
		this.uuidManager = uuidManager;
		this.time = time;
	}
	
	private MessagesConfig messages() {
		return configs.getMessagesConfig();
	}
	
	@Override
	public SendableMessage parseMessageWithoutPrefix(String messageToParse) {
		return JsonSkSerialiser.getInstance().deserialise(messageToParse);
	}
	
	@Override
	public SendableMessage prefix(SendableMessage message) {
		return messages().all().prefix().concatenate(message);
	}
	
	@Override
	public CentralisedFuture<SendableMessage> getPunishmentMessage(Punishment punishment) {
		return formatWithPunishment(messages().additions().forType(punishment.getType()).layout(), punishment);
	}
	
	@Override
	public CentralisedFuture<SendableMessage> formatWithPunishment(SendableMessageManipulator manipulator,
			Punishment punishment) {
		return formatWithPunishment(manipulator, punishment, null);
	}
	
	@Override
	public CentralisedFuture<SendableMessage> formatWithPunishmentAndUnoperator(SendableMessageManipulator manipulator,
			Punishment punishment, Operator unOperator) {
		return formatWithPunishment(manipulator, punishment, Objects.requireNonNull(unOperator, "unOperator"));
	}

	private CentralisedFuture<SendableMessage> formatWithPunishment(SendableMessageManipulator manipulator,
			Punishment punishment, Operator unOperator) {

		Map<FutureReplaceable, CentralisedFuture<String>> futureReplacements = new EnumMap<>(FutureReplaceable.class);
		for (FutureReplaceable futureReplaceable : FutureReplaceable.values()) {

			if (unOperator == null && futureReplaceable == FutureReplaceable.UNOPERATOR) {
				continue;
			}
			if (manipulator.contains(futureReplaceable.getVariable())) {
				CentralisedFuture<String> replacement = getFutureReplacement(futureReplaceable, punishment, unOperator);
				futureReplacements.put(futureReplaceable, replacement);
			}
		}
		return futuresFactory.supplyAsync(() -> getSimpleReplacements(punishment, unOperator))
				.thenCompose((simpleReplacements) -> {
			return formatWithPunishment0(manipulator, simpleReplacements, futureReplacements);
		});
	}
	
	private enum SimpleReplaceable {
		ID,
		TYPE,
		VICTIM_ID,
		OPERATOR_ID,
		UNOPERATOR_ID,
		REASON,
		SCOPE,
		DURATION,
		START_DATE,
		TIME_PASSED,
		END_DATE,
		TIME_REMAINING;
		
		String getVariable() {
			return "%" + name() + "%";
		}
		
	}
	
	private enum FutureReplaceable {
		VICTIM,
		OPERATOR,
		UNOPERATOR;
		
		String getVariable() {
			return "%" + name() + "%";
		}
	}
	
	private Map<SimpleReplaceable, String> getSimpleReplacements(Punishment punishment, Operator unOperator) {

		final long now = time.currentTime();
		final long start = punishment.getStartDateSeconds();

		final long timePassed = now - start;

		final String durationFormatted;
		final String relativeEndFormatted;

		if (punishment.isPermanent()) {
			// Permanent punishment
			MessagesConfig.Formatting.PermanentDisplay display = messages().formatting().permanentDisplay();
			durationFormatted = display.duration();
			relativeEndFormatted = display.relative();

		} else {
			final long end = punishment.getEndDateSeconds();
			assert end != 0 : end;
			// Temporary punishment
			long duration = end - start;
			durationFormatted = formatRelative(duration);

			// Using a margin of initiation prevents the "29 days, 23 hours, 59 minutes" issue
			if (timePassed < MARGIN_OF_INITIATION) {
				relativeEndFormatted = durationFormatted;

			} else {
				long timeRemaining = end - now;
				relativeEndFormatted = formatRelative(timeRemaining);
			}
		}

		Map<SimpleReplaceable, String> simpleReplacements = new EnumMap<>(SimpleReplaceable.class);
		simpleReplacements.put(SimpleReplaceable.ID, Integer.toString(punishment.getID()));
		simpleReplacements.put(SimpleReplaceable.TYPE, formatPunishmentType(punishment.getType()));
		simpleReplacements.put(SimpleReplaceable.VICTIM_ID, formatVictimId(punishment.getVictim()));
		simpleReplacements.put(SimpleReplaceable.OPERATOR_ID, formatOperatorId(punishment.getOperator()));
		if (unOperator != null)
			simpleReplacements.put(SimpleReplaceable.UNOPERATOR_ID, formatOperatorId(unOperator));
		simpleReplacements.put(SimpleReplaceable.REASON, punishment.getReason());
		simpleReplacements.put(SimpleReplaceable.SCOPE, formatScope(punishment.getScope()));
		simpleReplacements.put(SimpleReplaceable.DURATION, durationFormatted);
		simpleReplacements.put(SimpleReplaceable.START_DATE, formatAbsoluteDate(punishment.getStartDate()));
		simpleReplacements.put(SimpleReplaceable.TIME_PASSED, formatRelative(timePassed));
		simpleReplacements.put(SimpleReplaceable.END_DATE, formatAbsoluteDate(punishment.getEndDate()));
		simpleReplacements.put(SimpleReplaceable.TIME_REMAINING, relativeEndFormatted);

		return simpleReplacements;
	}
	
	private CentralisedFuture<SendableMessage> formatWithPunishment0(SendableMessageManipulator manipulator,
			Map<SimpleReplaceable, String> simpleReplacements,
			Map<FutureReplaceable, CentralisedFuture<String>> futureReplacements) {

		return futuresFactory.allOf(futureReplacements.values()).thenApply((ignore) -> {

			class Replacer implements UnaryOperator<String> {
				@Override
				public String apply(String text) {
					for (Map.Entry<SimpleReplaceable, String> simpleReplacement : simpleReplacements.entrySet()) {
						text = text.replace(
								simpleReplacement.getKey().getVariable(),
								simpleReplacement.getValue());
					}
					for (Map.Entry<FutureReplaceable, CentralisedFuture<String>> futureReplacement : futureReplacements.entrySet()) {
						text = text.replace(
								futureReplacement.getKey().getVariable(),
								futureReplacement.getValue().join());
					}
					return text;
				}
			}
			return manipulator.replaceText(new Replacer());
		});
	}
	
	private CentralisedFuture<String> getFutureReplacement(FutureReplaceable futureReplaceable, Punishment punishment,
			Operator unOperator) {
		switch (futureReplaceable) {
		case VICTIM:
			return formatVictim(punishment.getVictim());
		case OPERATOR:
			return formatOperator(punishment.getOperator());
		case UNOPERATOR:
			return formatOperator(unOperator);
		default:
			throw new IllegalArgumentException("Unknown replaceable " + futureReplaceable);
		}
	}
	
	private String formatVictimId(Victim victim) {
		switch (victim.getType()) {
		case PLAYER:
			return UUIDUtil.toShortString(((PlayerVictim) victim).getUUID());
		case ADDRESS:
			return formatAddressVictim((AddressVictim) victim);
		default:
			throw MiscUtil.unknownVictimType(victim.getType());
		}
	}
	
	private static final String NAME_UNKNOWN_ERROR = "-UnknownError-";

	private CentralisedFuture<String> formatVictim(Victim victim) {
		switch (victim.getType()) {
		case PLAYER:
			/*
			 * This should be a complete future every time we call this ourselves, because of UUIDMaster's fastCache.
			 * However, for API calls, the UUID/name might not be added to the cache.
			 */
			return uuidManager.lookupName(((PlayerVictim) victim).getUUID())
					.thenApply((optName) -> optName.orElse(NAME_UNKNOWN_ERROR));
		case ADDRESS:
			return futuresFactory.completedFuture(formatAddressVictim((AddressVictim) victim));
		default:
			throw MiscUtil.unknownVictimType(victim.getType());
		}
	}
	
	private String formatOperatorId(Operator operator) {
		switch (operator.getType()) {
		case CONSOLE:
			return messages().formatting().consoleDisplay();
		case PLAYER:
			return UUIDUtil.toShortString(((PlayerOperator) operator).getUUID());
		default:
			throw MiscUtil.unknownOperatorType(operator.getType());
		}
	}
	
	private CentralisedFuture<String> formatOperator(Operator operator) {
		switch (operator.getType()) {
		case CONSOLE:
			return futuresFactory.completedFuture(messages().formatting().consoleDisplay());
		case PLAYER:
			/*
			 * Similarly in #formatVictim, this should be a complete future every time we call this ourselves,
			 * because of UUIDMaster's fastCache.
			 */
			return uuidManager.lookupName(((PlayerOperator) operator).getUUID())
					.thenApply((optName) -> optName.orElse(NAME_UNKNOWN_ERROR));
		default:
			throw MiscUtil.unknownOperatorType(operator.getType());
		}
	}
	
	private String formatAddressVictim(AddressVictim addressVictim) {
		return addressVictim.getAddress().toString();
	}

	/** Visible for testing */
	String formatRelative(long diff) {
		if (diff < 0) {
			return formatRelative(-diff);
		}
		MessagesConfig.Misc.Time timeConfig = messages().misc().time();

		List<Map.Entry<ChronoUnit, String>> fragments = new ArrayList<>(timeConfig.fragments().entrySet());
		fragments.sort(Map.Entry.<ChronoUnit, String>comparingByKey().reversed());
		List<String> segments = new ArrayList<>(fragments.size());

		for (Map.Entry<ChronoUnit, String> fragment : fragments) {
			long unitLength = fragment.getKey().getDuration().toSeconds();
			if (diff >= unitLength) {
				long amount = (diff / unitLength);
				diff -= (amount * unitLength);
				segments.add(fragment.getValue().replace("%VALUE%", Long.toString(amount)));
			}
		}
		if (segments.isEmpty()) {
			return timeConfig.fallbackSeconds().replace("%VALUE%", Long.toString(diff));
		}
		if (segments.size() == 1) {
			return segments.get(0);
		}

		String delimiter = (timeConfig.useComma()) ? ", " : " ";
		StringJoiner joiner = new StringJoiner(delimiter);
		for (int n = 0; n < segments.size(); n++) {
			String segment = segments.get(n);
			boolean lastElement = n == segments.size() - 1;
			joiner.add((lastElement) ? timeConfig.and() + segment : segment);
		}
		return joiner.toString();
	}
	
	@Override
	public String formatPunishmentType(PunishmentType type) {
		String formatted = messages().formatting().punishmentTypeDisplay().get(type);
		return (formatted == null) ? type.toString() : formatted;
	}

	@Override
	public ZoneId getTimezone() {
		return configs.getMainConfig().dateFormatting().zoneId();
	}
	
	@Override
	public DateTimeFormatter getDateTimeFormatter() {
		return configs.getMainConfig().dateFormatting().formatAndPattern().getFormatter();
	}
	
	@Override
	public String formatAbsoluteDate(Instant date) {
		if (date.equals(Instant.MAX)) { // implicit null check
			return messages().formatting().permanentDisplay().absolute();
		}
		return getDateTimeFormatter().format(date.atZone(getTimezone()));
	}

	@Override
	public String formatDuration(Duration duration) {
		if (duration.equals(Duration.ZERO)) { // implicit null check
			return messages().formatting().permanentDisplay().duration();
		}
		return formatRelative(duration.toSeconds());
	}
	
	@Override
	public String formatScope(ServerScope scope) {
		Objects.requireNonNull(scope, "scope");
		String globalScopeDisplay = messages().formatting().globalScopeDisplay();
		return scopeManager.getServer(scope, globalScopeDisplay);
	}
	
}
