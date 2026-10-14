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

package space.arim.libertybans.core.punish;

import space.arim.jdbcaesar.QuerySource;
import space.arim.libertybans.api.NetworkAddress;

import java.util.UUID;

public class Association {

	private final UUID uuid;
	private final QuerySource<?> querySource;

	public Association(UUID uuid, QuerySource<?> querySource) {
		this.uuid = uuid;
		this.querySource = querySource;
	}

	public void associateCurrentName(String name, long currentTime) {
		querySource.query(
				"INSERT INTO `libertybans_names` (`uuid`, `name`, `updated`) VALUES (?, ?, ?) "
						+ "ON DUPLICATE KEY UPDATE `updated` = ?")
				.params(uuid, name, currentTime, currentTime)
				.voidResult().execute();
	}

	public void associatePastName(String name, long pastTime) {
		querySource.query(
				"INSERT IGNORE INTO `libertybans_names` (`uuid`, `name`, `updated`) VALUES (?, ?, ?)")
				.params(uuid, name, pastTime)
				.voidResult().execute();
	}

	public void associateCurrentAddress(NetworkAddress address, long currentTime) {
		querySource.query(
				"INSERT INTO `libertybans_addresses` (`uuid`, `address`, `updated`) VALUES (?, ?, ?) "
						+ "ON DUPLICATE KEY UPDATE `updated` = ?")
				.params(uuid, address, currentTime, currentTime)
				.voidResult().execute();
	}

	public void associatePastAddress(NetworkAddress address, long pastTime) {
		querySource.query(
				"INSERT IGNORE INTO `libertybans_addresses` (`uuid`, `address`, `updated`) VALUES (?, ?, ?)")
				.params(uuid, address, pastTime)
				.voidResult().execute();
	}
}
