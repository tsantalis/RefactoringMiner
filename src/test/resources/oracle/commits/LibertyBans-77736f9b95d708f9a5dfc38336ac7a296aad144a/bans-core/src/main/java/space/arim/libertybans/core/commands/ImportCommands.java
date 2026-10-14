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

package space.arim.libertybans.core.commands;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.importing.AdvancedBanImportSource;
import space.arim.libertybans.core.importing.ImportExecutor;
import space.arim.libertybans.core.importing.ImportSource;
import space.arim.libertybans.core.importing.LiteBansImportSource;
import space.arim.libertybans.core.importing.PlatformImportSource;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class ImportCommands extends AbstractSubCommandGroup {

	private final ImportExecutor executor;
	private final Map<PluginSourceType, Provider<? extends ImportSource>> importSourceProviders;
	private final AtomicBoolean isImporting = new AtomicBoolean();

	@Inject
	public ImportCommands(Dependencies dependencies, ImportExecutor executor,
						  Provider<AdvancedBanImportSource> advancedBanImportSourceProvider,
						  Provider<LiteBansImportSource> liteBansImportSourceProvider,
						  Provider<PlatformImportSource> platformImportSourceProvider) {
		super(dependencies, "import");
		this.executor = executor;
		importSourceProviders = Map.of(
				PluginSourceType.ADVANCEDBAN, advancedBanImportSourceProvider,
				PluginSourceType.LITEBANS, liteBansImportSourceProvider,
				PluginSourceType.VANILLA, platformImportSourceProvider);
	}

	private enum PluginSourceType {
		ADVANCEDBAN,
		LITEBANS,
		VANILLA
	}

	@Override
	public Collection<String> suggest(CmdSender sender, String arg, int argIndex) {
		switch (argIndex) {
		case 0:
			return getMatches();
		case 1:
			return List.of("advancedban", "litebans");
		default:
			break;
		}
		return List.of();
	}

	@Override
	public CommandExecution execute(CmdSender sender, CommandPackage command, String arg) {
		return new Execution(sender, command);
	}

	private class Execution extends AbstractCommandExecution {

		Execution(CmdSender sender, CommandPackage command) {
			super(sender, command);
		}

		@Override
		public void execute() {
			if (!sender().hasPermission("libertybans.admin.import")) {
				sender().sendMessage(messages().admin().noPermission());
				return;
			}
			if (!command().hasNext()) {
				sender().sendMessage(importMessages().usage());
				return;
			}
			String pluginSource = command().next().toUpperCase(Locale.ROOT);
			PluginSourceType sourceType;
			try {
				sourceType = PluginSourceType.valueOf(pluginSource);
			} catch (IllegalArgumentException ex) {
				sender().sendMessage(importMessages().usage());
				return;
			}
			if (!isImporting.compareAndSet(false, true)) {
				sender().sendMessage(importMessages().inProgress());
				return;
			}
			ImportSource importSource = importSourceProviders.get(sourceType).get();
			var future = executor.performImport(importSource).thenAccept((success) -> {
				if (success) {
					sender().sendMessage(importMessages().complete());
				} else {
					sender().sendMessage(importMessages().failure());
				}
			});
			future.whenComplete((ignore, ex) -> isImporting.set(false));
			postFuture(future);
		}

		private MessagesConfig.Admin.Importing importMessages() {
			return messages().admin().importing();
		}

	}

}
