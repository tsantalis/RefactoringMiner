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
package space.arim.libertybans.core.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.database.PunishmentDatabase;
import space.arim.libertybans.api.scope.ServerScope;

import space.arim.jdbcaesar.JdbCaesar;
import space.arim.jdbcaesar.QuerySource;

public interface InternalDatabase {
	
	PunishmentDatabase asExternal();

	Vendor getVendor();
	
	JdbCaesar jdbCaesar();
	
	CentralisedFuture<?> executeAsync(Runnable command);
	
	<T> CentralisedFuture<T> selectAsync(Supplier<T> supplier);
	
	PunishmentType getTypeFromResult(ResultSet resultSet) throws SQLException;
	
	Victim getVictimFromResult(ResultSet resultSet) throws SQLException;
	
	Operator getOperatorFromResult(ResultSet resultSet) throws SQLException;
	
	String getReasonFromResult(ResultSet resultSet) throws SQLException;
	
	ServerScope getScopeFromResult(ResultSet resultSet) throws SQLException;
	
	long getStartFromResult(ResultSet resultSet) throws SQLException;
	
	long getEndFromResult(ResultSet resultSet) throws SQLException;
	
	void clearExpiredPunishments(QuerySource<?> querySource, PunishmentType type, long currentTime);

	/**
	 * Designed to be used by testing, to clear all tables after one integration test
	 * 
	 */
	void truncateAllTables();

}
