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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.punish.Association;
import space.arim.libertybans.core.punish.Enaction;
import space.arim.omnibus.util.ThisClass;

class ImportSink {

	private final BatchOperationExecutor batchExecutor;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	ImportSink(BatchOperationExecutor batchExecutor) {
		this.batchExecutor = batchExecutor;
	}

	void addActivePunishment(Enaction enaction) {
		addPunishment(enaction, true);
	}

	void addHistoricalPunishment(Enaction enaction) {
		addPunishment(enaction, false);
	}

	private void addPunishment(Enaction enaction, boolean active) {
		batchExecutor.runOperation((querySource, connection) -> {
			if (active) {
				Punishment enacted = enaction.enactActive(querySource, connection::rollback);
				if (enacted == null) {
					logger.warn("Unable to import punishment with details {}. " +
							"It is likely there is a conflicting active punishment, for example " +
							"two bans for the same user. The conflicting punishment will be added to the " +
							"user's history, but it will not be enforced actively.", enaction.orderDetails());
				}
			} else {
				enaction.enactHistorical(querySource);
			}
		});
	}

	void addNameAddressRecord(NameAddressRecord nameAddressRecord) {
		batchExecutor.runOperation((querySource, connection) -> {
			Association association = new Association(nameAddressRecord.uuid(), querySource);
			long timeRecorded = nameAddressRecord.timeRecorded();
			nameAddressRecord.name().ifPresent((name) -> association.associatePastName(name, timeRecorded));
			nameAddressRecord.address().ifPresent((address) -> association.associatePastAddress(address, timeRecorded));
		});
	}

}
