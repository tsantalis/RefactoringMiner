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

import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import space.arim.omnibus.Omnibus;
import space.arim.omnibus.events.AsyncEvent;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.MainConfig;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.service.FuturePoster;

public abstract class AbstractSubCommandGroup implements SubCommandGroup {

	private final Dependencies dependencies;
	/**
	 * Matching commands, lowercased
	 * 
	 */
	private final Set<String> matches;
	
	private AbstractSubCommandGroup(Dependencies dependencies, Set<String> matches) {
		this.dependencies = dependencies;
		this.matches = Set.copyOf(matches);
	}
	
	AbstractSubCommandGroup(Dependencies dependencies, String...matches) {
		this(dependencies, Set.of(matches));
	}
	
	AbstractSubCommandGroup(Dependencies dependencies, Stream<String> matches) {
		this(dependencies, matches.collect(Collectors.toUnmodifiableSet()));
	}
	
	@Singleton
	public static class Dependencies {

		final Omnibus omnibus;
		final FactoryOfTheFuture futuresFactory;
		final FuturePoster futurePoster;
		final Configs configs;
		final ArgumentParser argumentParser;

		@Inject
		public Dependencies(Omnibus omnibus, FactoryOfTheFuture futuresFactory, FuturePoster futurePoster,
				Configs configs, ArgumentParser argumentParser) {
			this.omnibus = omnibus;
			this.futuresFactory = futuresFactory;
			this.futurePoster = futurePoster;
			this.configs = configs;
			this.argumentParser = argumentParser;
		}
		
	}
	
	@Override
	public boolean matches(String arg) {
		return matches.contains(arg);
	}
	
	Set<String> getMatches() {
		return matches;
	}
	
	/*
	 * 
	 * Helper methods
	 * 
	 */
	
	ArgumentParser argumentParser() {
		return dependencies.argumentParser;
	}
	
	FactoryOfTheFuture futuresFactory() {
		return dependencies.futuresFactory;
	}
	
	<T> CentralisedFuture<T> completedFuture(T value) {
		return futuresFactory().completedFuture(value);
	}
	
	void postFuture(CompletionStage<?> completionStage) {
		dependencies.futurePoster.postFuture(completionStage);
	}
	
	Configs configs() {
		return dependencies.configs;
	}
	
	MainConfig config() {
		return configs().getMainConfig();
	}
	
	MessagesConfig messages() {
		return configs().getMessagesConfig();
	}

	<E extends AsyncEvent> CompletionStage<E> fireWithTimeout(E event) {
		return dependencies.omnibus.getEventBus().fireAsyncEvent(event).orTimeout(10L, TimeUnit.SECONDS);
	}

}
