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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.DelayCalculators;
import space.arim.omnibus.util.concurrent.EnhancedExecutor;
import space.arim.omnibus.util.concurrent.ScheduledTask;

import space.arim.api.util.web.UUIDUtil;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.Victim.VictimType;
import space.arim.libertybans.api.database.PunishmentDatabase;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.bootstrap.plugin.PluginInfo;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.libertybans.core.service.SimpleThreadFactory;

import space.arim.jdbcaesar.JdbCaesar;
import space.arim.jdbcaesar.JdbCaesarBuilder;
import space.arim.jdbcaesar.QuerySource;
import space.arim.jdbcaesar.adapter.EnumNameAdapter;

public class StandardDatabase implements PunishmentDatabase, InternalDatabase {

	private final DatabaseManager manager;
	private final Vendor vendor;
	private final HikariDataSource dataSource;
	private final JdbCaesar jdbCaesar;
	private final ExecutorService executor;
	private final boolean refresherEvent;
	
	private ScheduledTask hyperSqlRefreshTask;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	private static final int MAJOR_REVISION = Integer.parseInt(PluginInfo.DATABASE_REVISION_MAJOR);
	private static final int MINOR_REVISION = Integer.parseInt(PluginInfo.DATABASE_REVISION_MINOR);
	
	private StandardDatabase(DatabaseManager manager, Vendor vendor, HikariDataSource dataSource, JdbCaesar jdbCaesar,
			ExecutorService executor, boolean refresherEvent) {
		this.manager = manager;
		this.vendor = vendor;
		this.dataSource = dataSource;
		this.jdbCaesar = jdbCaesar;
		this.executor = executor;
		this.refresherEvent = refresherEvent;
	}
	
	static StandardDatabase create(DatabaseManager manager, Vendor vendor, HikariDataSource hikariDataSource, int poolSize,
			boolean refresherEvent) {

		JdbCaesar jdbCaesar = new JdbCaesarBuilder()
				.dataSource(hikariDataSource)
				.exceptionHandler((ex) -> {
					logger.error("Error while executing a database query", ex);
				})
				.defaultFetchSize(DatabaseDefaults.FETCH_SIZE)
				.defaultIsolation(DatabaseDefaults.ISOLATION)
				.addAdapters(
						new EnumNameAdapter<>(PunishmentType.class),
						new JdbCaesarHelper.VictimAdapter(),
						new EnumNameAdapter<>(Victim.VictimType.class),
						new JdbCaesarHelper.OperatorAdapter(),
						new JdbCaesarHelper.ScopeAdapter(manager.scopeManager()),
						new JdbCaesarHelper.UUIDBytesAdapter(),
						new JdbCaesarHelper.NetworkAddressAdapter())
				.build();

		ExecutorService executor = Executors.newFixedThreadPool(poolSize, SimpleThreadFactory.create("Database"));
		return new StandardDatabase(manager, vendor, hikariDataSource, jdbCaesar, executor, refresherEvent);
	}
	
	/*
	 * Lifecycle
	 * 
	 * Guarded by the global lock on BaseFoundation lifecycle events
	 */
	
	void startRefreshTaskIfNecessary() {
		if (!refresherEvent) {
			EnhancedExecutor enhancedExecutor = manager.enhancedExecutorProvider().get();
			hyperSqlRefreshTask = enhancedExecutor.scheduleRepeating(
					new RefreshTaskRunnable(manager, this),
					Duration.ofHours(1L), DelayCalculators.fixedDelay());
		}
	}
	
	void cancelRefreshTaskIfNecessary() {
		if (!refresherEvent) {
			hyperSqlRefreshTask.cancel();
		}
	}
	
	void close() {
		dataSource.close();
		executor.shutdown();
	}
	
	void closeCompletely() {
		if (getVendor() == Vendor.HSQLDB) {
			jdbCaesar().query("SHUTDOWN").voidResult().execute();
		}
		close();
	}
	
	/*
	 * API
	 */
	
	@Override
	public Connection getConnection() throws SQLException {
		logger.debug("Foreign caller acquiring connection");
		return dataSource.getConnection();
	}
	
	@Override
	public int getMajorRevision() {
		return MAJOR_REVISION;
	}

	@Override
	public int getMinorRevision() {
		return MINOR_REVISION;
	}

	@Override
	public Executor getExecutor() {
		return executor;
	}
	
	/*
	 * Internal methods
	 */
	
	@Override
	public PunishmentDatabase asExternal() {
		return this;
	}
	
	@Override
	public Vendor getVendor() {
		return vendor;
	}
	
	@Override
	public JdbCaesar jdbCaesar() {
		return jdbCaesar;
	}
	
	@Override
	public CentralisedFuture<?> executeAsync(Runnable command) {
		return manager.futuresFactory().runAsync(command, executor);
	}
	
	@Override
	public <T> CentralisedFuture<T> selectAsync(Supplier<T> supplier) {
		return manager.futuresFactory().supplyAsync(supplier, executor);
	}
	
	@Override
	public PunishmentType getTypeFromResult(ResultSet resultSet) throws SQLException {
		return PunishmentType.valueOf(resultSet.getString("type"));
	}
	
	@Override
	public Victim getVictimFromResult(ResultSet resultSet) throws SQLException {
		VictimType victimType = VictimType.valueOf(resultSet.getString("victim_type"));
		byte[] bytes = resultSet.getBytes("victim");
		switch (victimType) {
		case PLAYER:
			return PlayerVictim.of(UUIDUtil.fromByteArray(bytes));
		case ADDRESS:
			return AddressVictim.of(bytes);
		default:
			throw MiscUtil.unknownVictimType(victimType);
		}
	}

	@Override
	public Operator getOperatorFromResult(ResultSet resultSet) throws SQLException {
		return JdbCaesarHelper.getOperatorFromResult(resultSet);
	}
	
	@Override
	public String getReasonFromResult(ResultSet resultSet) throws SQLException {
		return resultSet.getString("reason");
	}

	@Override
	public ServerScope getScopeFromResult(ResultSet resultSet) throws SQLException {
		String server = resultSet.getString("scope");
		if (!server.isEmpty()) {
			return manager.scopeManager().specificScope(server);
		}
		return manager.scopeManager().globalScope();
	}
	
	@Override
	public long getStartFromResult(ResultSet resultSet) throws SQLException {
		return resultSet.getLong("start");
	}
	
	@Override
	public long getEndFromResult(ResultSet resultSet) throws SQLException {
		return resultSet.getLong("end");
	}
	
	@Override
	public void clearExpiredPunishments(QuerySource<?> querySource, PunishmentType type, long currentTime) {
		assert type != PunishmentType.KICK;
		String query;
		if (getVendor().hasDeleteFromJoin()) {
			query = "DELETE `thetype` FROM `libertybans_" + type + "s` `thetype` "
					+ "INNER JOIN `libertybans_punishments` `puns` ON `puns`.`id` = `thetype`.`id` WHERE "
					+ "`puns`.`end` != 0 AND `puns`.`end` < ?";

		} else {
			query = "DELETE FROM `libertybans_" + type + "s` WHERE `id` IN "
					+ "(SELECT `id` FROM `libertybans_punishments` `puns` WHERE `puns`.`end` != 0 AND `puns`.`end` < ?)";
		}
		querySource.query(query).params(currentTime).voidResult().execute();
	}

	@Override
	public void truncateAllTables() {
		String[] tables = new String[] { // Order is significant
				"names", "addresses", "history", "bans", "mutes", "warns", "punishments"};
		for (String table : tables) {
			// TRUNCATE TABLE does not work with foreign key constraints on MySQL
			String queryCommand = (getVendor() == Vendor.HSQLDB) ? "TRUNCATE TABLE" : "DELETE FROM";
			jdbCaesar().query(queryCommand + " `libertybans_" + table + "`").voidResult().execute();
		}
	}

}
