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
package space.arim.libertybans.core.punish;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.DraftPunishmentBuilder;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.time.Duration;

@Singleton
public class Enactor implements PunishmentDrafter {

	private final InternalScopeManager scopeManager;
	private final Provider<InternalDatabase> dbProvider;
	private final PunishmentCreator creator;

	@Inject
	public Enactor(InternalScopeManager scopeManager, Provider<InternalDatabase> dbProvider, PunishmentCreator creator) {
		this.scopeManager = scopeManager;
		this.dbProvider = dbProvider;
		this.creator = creator;
	}

	@Override
	public DraftPunishmentBuilder draftBuilder() {
		return new DraftPunishmentBuilderImpl(this);
	}

	InternalScopeManager scopeManager() {
		return scopeManager;
	}

	CentralisedFuture<Punishment> enactPunishment(DraftPunishment draftPunishment) {
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {

			final PunishmentType type = draftPunishment.getType();
			final Duration duration = draftPunishment.getDuration();
			final long start = MiscUtil.currentTime();
			final long end = (duration.isZero()) ? 0L : start + duration.toSeconds();

			Enaction enaction = new Enaction(
					new Enaction.OrderDetails(
							type, draftPunishment.getVictim(), draftPunishment.getOperator(),
							draftPunishment.getReason(), draftPunishment.getScope(), start, end),
					creator);

			return database.jdbCaesar().transaction().body((querySource, controller) -> {

				if (type != PunishmentType.KICK) {
					database.clearExpiredPunishments(querySource, type, start);
				}
				return enaction.enactActive(querySource, controller::rollback);
			}).execute();
		});
	}

}
