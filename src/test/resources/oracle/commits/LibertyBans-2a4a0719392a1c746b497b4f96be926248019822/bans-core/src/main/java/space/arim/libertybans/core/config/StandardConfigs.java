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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import space.arim.libertybans.bootstrap.StartupException;

@Singleton
public class StandardConfigs implements Configs {
	
	private final Path folder;
	
	private final ConfigHolder<MainConfig> mainHolder = new ConfigHolder<>(MainConfig.class);
	private final ConfigHolder<MessagesConfig> messagesHolder = new ConfigHolder<>(MessagesConfig.class);
	private final ConfigHolder<SqlConfig> sqlHolder = new ConfigHolder<>(SqlConfig.class);
	
	@Inject
	public StandardConfigs(@Named("folder") Path folder) {
		this.folder = folder;
	}
	
	@Override
	public MainConfig getMainConfig() {
		return mainHolder.getConfigData();
	}
	
	@Override
	public MessagesConfig getMessagesConfig() {
		return messagesHolder.getConfigData();
	}
	
	@Override
	public SqlConfig getSqlConfig() {
		return sqlHolder.getConfigData();
	}
	
	@Override
	public CompletableFuture<Boolean> reloadConfigs() {
		Path langFolder = folder.resolve("lang");
		try {
			Files.createDirectories(langFolder);
		} catch (IOException ex) {
			throw new UncheckedIOException("Unable to create plugin directories", ex);
		}
		// Save default language files
		CompletableFuture<?> futureLangFiles = createLangFiles(langFolder);

		// Reload main config
		CompletableFuture<Boolean> reloadMain = mainHolder.reload(folder.resolve("config.yml"));
		// Reload sql config
		CompletableFuture<Boolean> reloadSql = sqlHolder.reload(folder.resolve("sql.yml"));

		// Reload messages config from specified language file
		CompletableFuture<Boolean> reloadMessages = CompletableFuture.allOf(futureLangFiles, reloadMain)
				.thenCompose((ignore) -> {
			if (!reloadMain.join()) {
				return CompletableFuture.completedFuture(false);
			}
			String langFileOption = mainHolder.getConfigData().langFile();
			return messagesHolder.reload(langFolder.resolve("messages_" + langFileOption + ".yml"));
		});
		return CompletableFuture.allOf(reloadMessages, reloadSql).thenApply((ignore) -> {
			return reloadMain.join() && reloadMessages.join() && reloadSql.join();
		});
	}
	
	private CompletableFuture<?> createLangFiles(Path langFolder) {
		Translation[] translations = Translation.values();
		Set<CompletableFuture<?>> futureLangFiles = new HashSet<>(translations.length);
		for (Translation translation : translations) {

			final String name = "messages_" + translation.name().toLowerCase(Locale.ROOT) + ".yml";
			Path messagesPath = langFolder.resolve(name);
			if (Files.exists(messagesPath)) {
				continue;
			}
			var copyTranslationFuture = CompletableFuture.runAsync(() -> {
				try (InputStream inputStream = getClass().getResource("/lang/" + name).openStream();
						ReadableByteChannel source = Channels.newChannel(inputStream);
						FileChannel dest = FileChannel.open(messagesPath, StandardOpenOption.WRITE,
								StandardOpenOption.CREATE_NEW)) {

					dest.transferFrom(source, 0, Long.MAX_VALUE);
				} catch (IOException ex) {
					throw new UncheckedIOException("Unable to copy language file for language " + name, ex);
				}
			});
			futureLangFiles.add(copyTranslationFuture);
		}
		return CompletableFuture.allOf(futureLangFiles.toArray(CompletableFuture[]::new));
	}
	
	@Override
	public void startup() {
		if (!reloadConfigs().join()) {
			throw new StartupException("Issue while loading configuration");
		}
	}
	
	@Override
	public void restart() {
		if (!reloadConfigs().join()) {
			throw new StartupException("Issue while reloading configuration");
		}
	}
	
	@Override
	public void shutdown() {

	}
	
}
