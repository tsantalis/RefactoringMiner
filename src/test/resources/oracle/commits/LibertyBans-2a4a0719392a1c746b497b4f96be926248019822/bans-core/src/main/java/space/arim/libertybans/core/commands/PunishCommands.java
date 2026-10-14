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
package space.arim.libertybans.core.commands;

import java.time.Duration;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.chat.SendableMessage;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.core.config.AdditionsSection;
import space.arim.libertybans.core.config.AdditionsSection.ExclusivePunishmentAddition;
import space.arim.libertybans.core.config.AdditionsSection.PunishmentAdditionWithDurationPerm;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.config.MainConfig;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.event.PostPunishEventImpl;
import space.arim.libertybans.core.event.PunishEventImpl;

abstract class PunishCommands extends AbstractSubCommandGroup implements PunishUnpunishCommands {

	private final PunishmentDrafter drafter;
	private final InternalFormatter formatter;
	private final EnvEnforcer envEnforcer;
	
	PunishCommands(Dependencies dependencies, Stream<String> matches,
			PunishmentDrafter drafter, InternalFormatter formatter, EnvEnforcer envEnforcer) {
		super(dependencies, matches);
		this.drafter = drafter;
		this.formatter = formatter;
		this.envEnforcer = envEnforcer;
	}

	@Override
	public final CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		return new Execution(sender, command, parseType(arg.toUpperCase(Locale.ROOT)));
	}

	@Override
	public final Collection<String> suggest(CmdSender sender, String arg, int argIndex) {
		switch (argIndex) {
		case 0:
			return getMatches();
		case 1:
			return sender.getOtherPlayersOnSameServer();
		default:
			break;
		}
		return Set.of();
	}
	
	private class Execution extends TypeSpecificExecution {

		private final AdditionsSection.PunishmentAddition section;
		
		Execution(CmdSender sender, CommandPackage command, PunishmentType type) {
			super(sender, command, type);
			section = messages().additions().forType(type);
		}

		@Override
		public void execute() {
			if (!sender().hasPermission("libertybans." + type() + ".command")) {
				sender().sendMessage(section.permissionCommand()); 
				return;
			}
			String additionalPermission = getAdditionalPermission(type());
			if (!additionalPermission.isEmpty() && !sender().hasPermission(additionalPermission)) {
				sender().sendMessage(section.permissionIpAddress());
				return;
			}
			if (!command().hasNext()) {
				sender().sendMessage(section.usage());
				return;
			}
			execute0();
		}

		private void execute0() {
			String targetArg = command().next();
			CentralisedFuture<?> future = parseVictim(sender(), targetArg).thenCompose((victim) -> {
				if (victim == null) {
					return completedFuture(null);
				}
				return performEnact(victim, targetArg);

			}).thenCompose((punishment) -> {
				if (punishment == null) {
					return completedFuture(null);
				}
				return enforceAndSendSuccess(punishment);
			});
			postFuture(future);
		}

		private CompletionStage<Punishment> performEnact(Victim victim, String targetArg) {
			/*
			 * Parse duration. Duration.ZERO represents permanent duration
			 */
			final Duration duration;
			if (type() != PunishmentType.KICK && command().hasNext()) {
				String time = command().peek();
				duration = new DurationParser(time, messages().formatting().permanentArguments()).parse();
				if (!duration.isZero()) {
					command().next();
				}
			} else {
				duration = Duration.ZERO;
			}

			/*
			 * Duration permissions
			 */
			Duration greatestPermission;
			if (type() != PunishmentType.KICK // kicks are always permanent
					&& config().durationPermissions().enable() // Duration permissions enabled
					&& !(greatestPermission = getGreatestPermittedDuration()).isZero() // sender does not have permanent permission
					&& (duration.isZero() || greatestPermission.compareTo(duration) < 0)) { // sender does not have enough permissions

				String durationFormatted = formatter.formatDuration(duration);
				PunishmentAdditionWithDurationPerm sectionWithDurationPerm = ((PunishmentAdditionWithDurationPerm) section);
				sender().sendMessage(sectionWithDurationPerm.permissionDuration().replaceText("%DURATION%", durationFormatted));
				return completedFuture(null);
			}
			String reason;
			MainConfig.Reasons reasonsConfig;
			if (command().hasNext()) {
				reason = command().allRemaining();
			} else if ((reasonsConfig = config().reasons()).permitBlank()) {
				reason = "";
			} else {
				reason = reasonsConfig.defaultReason();
			}
			DraftPunishment draftPunishment = drafter.draftBuilder()
					.type(type()).victim(victim).operator(sender().getOperator())
					.reason(reason).duration(duration)
					.build();

			return fireWithTimeout(new PunishEventImpl(draftPunishment)).thenCompose((event) -> {
				if (event.isCancelled()) {
					return completedFuture(null);
				}
				return draftPunishment.enactPunishmentWithoutEnforcement().thenApply((optPunishment) -> {
					if (optPunishment.isEmpty()) {
						sendConflict(targetArg);
						return null;
					}
					return optPunishment.get();
				});
			});
		}
		
		// Duration permissions
		
		/**
		 * Gets the sender's greatest duration permission, or zero for permanent
		 * @return the greatest duration permission or zero for permanent
		 */
		private Duration getGreatestPermittedDuration() {
			Duration greatestPermission = Duration.ofNanos(-1L);
			for (String durationPerm : config().durationPermissions().permissionsToCheck()) {
				if (!sender().hasPermission(durationPerm)) {
					continue;
				}
				Duration thisDurationPerm = new DurationParser(durationPerm).parse();
				if (thisDurationPerm.isZero()) {
					return Duration.ZERO;
				}
				if (thisDurationPerm.compareTo(greatestPermission) > 0) {
					greatestPermission = thisDurationPerm;
				}
			}
			return greatestPermission;
		}

		// Outcomes

		private void sendConflict(String targetArg) {
			SendableMessage message;
			if (type().isSingular()) {
				message = ((ExclusivePunishmentAddition) section).conflicting().replaceText("%TARGET%", targetArg);
			} else {
				message = messages().misc().unknownError();
			}
			sender().sendMessage(message);
		}

		private CentralisedFuture<?> enforceAndSendSuccess(Punishment punishment) {

			CentralisedFuture<?> enforcement = punishment.enforcePunishment().toCompletableFuture();
			CentralisedFuture<SendableMessage> futureMessage = formatter.formatWithPunishment(
					section.successMessage(), punishment);
			CentralisedFuture<SendableMessage> futureNotify = formatter.formatWithPunishment(
					section.successNotification(), punishment);

			var completion = futuresFactory().allOf(enforcement, futureMessage, futureNotify).thenRun(() -> {
				sender().sendMessage(futureMessage.join());

				envEnforcer.sendToThoseWithPermission(
						"libertybans." + type() + ".notify",
						formatter.prefix(futureNotify.join()));
			});
			postFuture(fireWithTimeout(new PostPunishEventImpl(punishment)));
			return completion;
		}
		
	}

}
