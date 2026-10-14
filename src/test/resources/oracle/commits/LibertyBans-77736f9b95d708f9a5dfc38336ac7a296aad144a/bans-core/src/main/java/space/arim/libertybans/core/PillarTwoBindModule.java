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
package space.arim.libertybans.core;

import jakarta.inject.Singleton;

import space.arim.libertybans.core.commands.usage.StandardUsageGlossary;
import space.arim.libertybans.core.commands.usage.UsageGlossary;
import space.arim.omnibus.util.concurrent.EnhancedExecutor;

import space.arim.api.env.PlatformHandle;

import space.arim.libertybans.core.commands.extra.ArgumentParser;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.commands.CommandsCore;
import space.arim.libertybans.core.commands.extra.StandardArgumentParser;
import space.arim.libertybans.core.config.Formatter;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.database.DatabaseManager;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.service.AsynchronicityManager;
import space.arim.libertybans.core.service.FuturePoster;
import space.arim.libertybans.core.service.StandardAsynchronicityManager;
import space.arim.libertybans.core.uuid.CachingUUIDManager;
import space.arim.libertybans.core.uuid.NameValidator;
import space.arim.libertybans.core.uuid.StandardNameValidator;
import space.arim.libertybans.core.uuid.UUIDManager;

public class PillarTwoBindModule {

	public AsynchronicityManager asyncManager(StandardAsynchronicityManager standardAsyncManager) {
		return standardAsyncManager;
	}

	public FuturePoster futurePoster(AsynchronicityManager asyncManager) {
		return asyncManager;
	}

	@Singleton
	public EnhancedExecutor enhancedExecutor(PlatformHandle envHandle) {
		return envHandle.createEnhancedExecutor();
	}

	public InternalDatabase database(DatabaseManager databaseManager) {
		return databaseManager.getInternal();
	}

	public UUIDManager uuidManager(CachingUUIDManager uuidManager) {
		return uuidManager;
	}

	public InternalFormatter formatter(Formatter formatter) {
		return formatter;
	}

	public Commands commands(CommandsCore commands) {
		return commands;
	}

	public ArgumentParser argumentParser(StandardArgumentParser argumentParser) {
		return argumentParser;
	}

	public NameValidator nameValidator(StandardNameValidator nameValidator) {
		return nameValidator;
	}

	public UsageGlossary usage(StandardUsageGlossary usage) {
		return usage;
	}
}
