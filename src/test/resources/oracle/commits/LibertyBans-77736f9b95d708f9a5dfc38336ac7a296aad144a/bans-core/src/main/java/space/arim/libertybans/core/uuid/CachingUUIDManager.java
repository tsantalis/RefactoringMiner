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
package space.arim.libertybans.core.uuid;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.api.util.web.RemoteApiResult;
import space.arim.api.util.web.RemoteNameUUIDApi;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.env.EnvUserResolver;

@Singleton
public final class CachingUUIDManager implements UUIDManager {

	private final Configs configs;
	private final FactoryOfTheFuture futuresFactory;
	private final NameValidator nameValidator;
	private final EnvUserResolver envResolver;
	private final QueryingImpl queryingImpl;

	private final Cache<String, UUID> nameToUuidCache;
	private final Cache<UUID, String> uuidToNameCache;

	@Inject
	public CachingUUIDManager(Configs configs, FactoryOfTheFuture futuresFactory, Provider<InternalDatabase> dbProvider,
			NameValidator nameValidator, EnvUserResolver envResolver) {
		this(configs, futuresFactory, nameValidator, envResolver, new QueryingImpl(dbProvider));
	}

	CachingUUIDManager(Configs configs, FactoryOfTheFuture futuresFactory, NameValidator nameValidator,
					   EnvUserResolver envResolver, QueryingImpl queryingImpl) {
		this.configs = configs;
		this.futuresFactory = futuresFactory;
		this.nameValidator = nameValidator;
		this.envResolver = envResolver;
		this.queryingImpl = queryingImpl;

		Duration expiration = Duration.ofSeconds(20L);
		nameToUuidCache = Caffeine.newBuilder().expireAfterAccess(expiration).build();
		uuidToNameCache = Caffeine.newBuilder().expireAfterAccess(expiration).build();
	}
	
	@Override
	public void addCache(UUID uuid, String name) {
		nameToUuidCache.put(name.toLowerCase(Locale.ROOT), uuid);
		uuidToNameCache.put(uuid, name);
	}
	
	private <T> CentralisedFuture<T> completedFuture(T value) {
		return futuresFactory.completedFuture(value);
	}

	/*
	 * UUID resolution works as follows:
	 * 
	 * 1. Check caches
	 * 2. Check online players
	 * 3. Check own database
	 * 4. If online server, check Mojang API and third party web APIs where configured.
	 */
	
	@Override
	public CentralisedFuture<Optional<UUID>> lookupUUID(String name) {
		if (!nameValidator.validateNameArgument(name)) {
			return completedFuture(Optional.empty());
		}
		UUID cachedResolve = nameToUuidCache.getIfPresent(name.toLowerCase(Locale.ROOT));
		if (cachedResolve != null) {
			return completedFuture(Optional.of(cachedResolve));
		}
		return lookupUUIDUncached(name).thenApply((optExternalUuid) -> {
			if (optExternalUuid.isPresent()) {
				addCache(optExternalUuid.get(), name);
			}
			return optExternalUuid;
		});
	}

	private CentralisedFuture<Optional<UUID>> lookupUUIDUncached(String name) {
		Optional<UUID> envResolve = envResolver.lookupUUID(name);
		if (envResolve.isPresent()) {
			return completedFuture(envResolve);
		}
		return queryingImpl.resolve(name).thenCompose((queriedUuid) -> {
			if (queriedUuid != null) {
				return completedFuture(Optional.of(queriedUuid));
			}
			return webLookup((remoteApi) -> remoteApi.lookupUUID(name));
		});
	}

	@Override
	public CentralisedFuture<Optional<String>> lookupName(UUID uuid) {
		String cachedResolve = uuidToNameCache.getIfPresent(uuid);
		if (cachedResolve != null) {
			return completedFuture(Optional.of(cachedResolve));
		}
		return lookupNameUncached(uuid).thenApply((optExternalName) -> {
			if (optExternalName.isPresent()) {
				addCache(uuid, optExternalName.get());
			}
			return optExternalName;
		});
	}

	private CentralisedFuture<Optional<String>> lookupNameUncached(UUID uuid) {
		Optional<String> envResolve = envResolver.lookupName(uuid);
		if (envResolve.isPresent()) {
			return completedFuture(envResolve);
		}
		return queryingImpl.resolve(uuid).thenCompose((queriedName) -> {
			if (queriedName != null) {
				return completedFuture(Optional.of(queriedName));
			}
			return webLookup((remoteApi) -> remoteApi.lookupName(uuid));
		});
	}
	
	private <T> CompletableFuture<Optional<T>> webLookup(Function<RemoteNameUUIDApi, CompletableFuture<RemoteApiResult<T>>> resultFunction) {
		UUIDResolutionConfig uuidResolution = configs.getMainConfig().uuidResolution();
		if (uuidResolution.serverType() != ServerType.ONLINE) {
			return futuresFactory.completedFuture(Optional.empty());
		}
		return uuidResolution.remoteApis().lookup(resultFunction).thenApply(Optional::ofNullable);
	}
	
	// Address lookups
	
	@Override
	public CentralisedFuture<NetworkAddress> lookupAddress(String name) {
		if (!nameValidator.validateNameArgument(name)) {
			return completedFuture(null);
		}
		InetAddress quickResolve = envResolver.getAddressOfOnlinePlayer(name);
		if (quickResolve != null) {
			return completedFuture(NetworkAddress.of(quickResolve));
		}
		return queryingImpl.resolveAddress(name);
	}
	
}
