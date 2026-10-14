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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
public class CachingUUIDManager implements UUIDManager {

	private final Configs configs;
	private final FactoryOfTheFuture futuresFactory;
	private final NameValidator nameValidator;
	private final EnvUserResolver envResolver;

	final Cache<UUID, String> fastCache = Caffeine.newBuilder().expireAfterAccess(20L, TimeUnit.SECONDS).build();
	private final QueryingImpl queryingImpl;

	@Inject
	public CachingUUIDManager(Configs configs, FactoryOfTheFuture futuresFactory, Provider<InternalDatabase> dbProvider,
			NameValidator nameValidator, EnvUserResolver envResolver) {
		this.configs = configs;
		this.futuresFactory = futuresFactory;
		this.nameValidator = nameValidator;
		this.envResolver = envResolver;

		queryingImpl = new QueryingImpl(dbProvider);
	}
	
	@Override
	public void addCache(UUID uuid, String name) {
		fastCache.put(uuid, name);
	}
	
	private <T> CentralisedFuture<T> completedFuture(T value) {
		return futuresFactory.completedFuture(value);
	}

	UUID resolveUuidCached(String name) {
		// Caffeine specifies that operations on the entry set do not refresh the expiration timer
		for (Map.Entry<UUID, String> entry : fastCache.asMap().entrySet()) {
			if (entry.getValue().equalsIgnoreCase(name)) {
				UUID uuid = entry.getKey();
				// Manual cache refresh
				fastCache.getIfPresent(uuid);
				return uuid;
			}
		}
		return null;
	}

	/*
	 * UUID resolution works as follows:
	 * 
	 * 1. Check online players
	 * 2. Check fast cache using own resolver
	 * 3. Check own database using own resolver
	 * 4. Check other resolvers through UUIDVault.
	 * 5. If online server, check Mojang API and third party web APIs where configured.
	 * If offline server, calculate UUID if possible. If mixed server, stop.
	 * 
	 * If 4 or 5 found an answer, add it to the fast cache.
	 * 
	 */
	
	@Override
	public CentralisedFuture<Optional<UUID>> lookupUUID(final String name) {
		if (!nameValidator.validateNameArgument(name)) {
			return completedFuture(Optional.empty());
		}
		Optional<UUID> envResolve = envResolver.lookupUUID(name);
		if (envResolve.isPresent()) {
			return completedFuture(envResolve);
		}
		UUID cachedResolve = resolveUuidCached(name);
		if (cachedResolve != null) {
			return completedFuture(Optional.of(cachedResolve));
		}
		return queryingImpl.resolve(name).thenCompose((queriedUuid) -> {
			if (queriedUuid != null) {
				return completedFuture(Optional.of(queriedUuid));
			}
			return webLookup((remoteApi) -> remoteApi.lookupUUID(name)).thenApply((optExternalUuid) -> {
				if (optExternalUuid.isPresent()) {
					fastCache.put(optExternalUuid.get(), name);
				}
				return optExternalUuid;
			});
		});
	}

	@Override
	public CentralisedFuture<Optional<String>> lookupName(final UUID uuid) {
		Optional<String> envResolve = envResolver.lookupName(uuid);
		if (envResolve.isPresent()) {
			return completedFuture(envResolve);
		}
		String cachedResolve = fastCache.getIfPresent(uuid);
		if (cachedResolve != null) {
			return completedFuture(Optional.of(cachedResolve));
		}
		return queryingImpl.resolve(uuid).thenCompose((queriedName) -> {
			if (queriedName != null) {
				return completedFuture(Optional.of(queriedName));
			}
			return webLookup((remoteApi) -> remoteApi.lookupName(uuid)).thenApply((optExternalName) -> {
				if (optExternalName.isPresent()) {
					fastCache.put(uuid, optExternalName.get());
				}
				return optExternalName;
			});
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
	public CentralisedFuture<NetworkAddress> fullLookupAddress(String name) {
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
