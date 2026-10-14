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

import java.nio.file.Path;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import space.arim.omnibus.util.concurrent.EnhancedExecutor;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.libertybans.api.database.PunishmentDatabase;
import space.arim.libertybans.bootstrap.StartupException;
import space.arim.libertybans.core.Part;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.scope.InternalScopeManager;

@Singleton
public class DatabaseManager implements Part {

	private final Path folder;
	private final FactoryOfTheFuture futuresFactory;
	private final Provider<EnhancedExecutor> enhancedExecutorProvider;
	private final Configs configs;
	private final InternalScopeManager scopeManager;
	
	private volatile StandardDatabase database;
	
	@Inject
	public DatabaseManager(@Named("folder") Path folder, FactoryOfTheFuture futuresFactory,
			Provider<EnhancedExecutor> enhancedExecutorProvider, Configs configs, InternalScopeManager scopeManager) {
		this.folder = folder;
		this.futuresFactory = futuresFactory;
		this.enhancedExecutorProvider = enhancedExecutorProvider;
		this.configs = configs;
		this.scopeManager = scopeManager;
	}
	
	Path folder() {
		return folder;
	}
	
	FactoryOfTheFuture futuresFactory() {
		return futuresFactory;
	}
	
	Provider<EnhancedExecutor> enhancedExecutorProvider() {
		return enhancedExecutorProvider;
	}
	
	Configs configs() {
		return configs;
	}
	
	InternalScopeManager scopeManager() {
		return scopeManager;
	}
	
	public InternalDatabase getInternal() {
		return database;
	}
	
	public PunishmentDatabase getExternal() {
		return database.asExternal();
	}
	
	@Override
	public void startup() {
		DatabaseResult dbResult = new DatabaseSettings(this).create();
		StandardDatabase database = dbResult.database;
		if (!dbResult.success) {
			database.closeCompletely();
			throw new StartupException("Database initialisation failed");
		}
		database.startRefreshTaskIfNecessary();
		this.database = database;
	}
	
	@Override
	public void restart() {
		StandardDatabase currentDatabase = this.database;

		DatabaseResult dbResult = new DatabaseSettings(this).create();
		StandardDatabase database = dbResult.database;
		if (!dbResult.success) {
			database.close();
			throw new StartupException("Database restart failed");
		}
		currentDatabase.cancelRefreshTaskIfNecessary();

		if (currentDatabase.getVendor() == database.getVendor()) {
			currentDatabase.close();
		} else {
			currentDatabase.closeCompletely();
		}

		database.startRefreshTaskIfNecessary();
		this.database = database;
	}

	@Override
	public void shutdown() {
		StandardDatabase database = this.database;
		database.cancelRefreshTaskIfNecessary();
		database.closeCompletely();
	}
	
}
