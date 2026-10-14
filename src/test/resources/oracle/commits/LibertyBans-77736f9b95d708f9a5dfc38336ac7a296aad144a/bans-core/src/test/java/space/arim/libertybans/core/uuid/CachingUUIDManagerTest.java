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

package space.arim.libertybans.core.uuid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.api.util.web.UUIDUtil;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.MainConfig;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CachingUUIDManagerTest {

	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();
	private final Configs configs;
	private final NameValidator nameValidator;
	private final EnvUserResolver envUserResolver;
	private final QueryingImpl queryingImpl;

	private final UUIDManager uuidManager;

	private final UUID uuid = UUIDUtil.fromShortString("ed5f12cd600745d9a4b9940524ddaecf");
	private final String name = "A248";
	private final NetworkAddress address = NetworkAddress.of(new byte[4]);

	public CachingUUIDManagerTest(@Mock Configs configs, @Mock NameValidator nameValidator,
								  @Mock EnvUserResolver envUserResolver, @Mock QueryingImpl queryingImpl) {
		this.configs = configs;
		this.nameValidator = nameValidator;
		this.envUserResolver = envUserResolver;
		this.queryingImpl = queryingImpl;

		uuidManager = new CachingUUIDManager(configs, futuresFactory,
				nameValidator, envUserResolver, queryingImpl);
	}

	@BeforeEach
	public void setup() {
		lenient().when(nameValidator.validateNameArgument(name)).thenReturn(true);
	}

	private UUID lookupUUID(String name) {
		return uuidManager.lookupUUID(name).join().orElse(null);
	}

	private String lookupName(UUID uuid) {
		return uuidManager.lookupName(uuid).join().orElse(null);
	}

	private NetworkAddress lookupAddress(String name) {
		return uuidManager.lookupAddress(name).join();
	}

	private <T> CentralisedFuture<T> completedFuture(T value) {
		return futuresFactory.completedFuture(value);
	}

	@Test
	public void resolveUUIDFromEnvironment() {
		when(envUserResolver.lookupUUID(name)).thenReturn(
				Optional.of(uuid), Optional.empty());

		assertEquals(uuid, lookupUUID(name));
		assertEquals(uuid, lookupUUID(name), "UUID should be cached");

		verify(envUserResolver).lookupUUID(name);
	}

	@Test
	public void resolveNameFromEnvironment() {
		when(envUserResolver.lookupName(uuid)).thenReturn(
				Optional.of(name), Optional.empty());

		assertEquals(name, lookupName(uuid));
		assertEquals(name, lookupName(uuid), "Name should be cached");

		verify(envUserResolver).lookupName(uuid);
	}

	@Test
	public void resolveAddressFromEnvironment() {
		when(envUserResolver.getAddressOfOnlinePlayer(name)).thenReturn(address.toInetAddress());

		assertEquals(address, lookupAddress(name));

		verify(envUserResolver).getAddressOfOnlinePlayer(name);
	}

	@Test
	public void resolveUUIDQueried() {
		when(queryingImpl.resolve(name)).thenReturn(completedFuture(uuid), completedFuture(null));

		assertEquals(uuid, lookupUUID(name));
		assertEquals(uuid, lookupUUID(name), "UUID should be cached");

		verify(queryingImpl).resolve(name);
	}

	@Test
	public void resolveNameQueried() {
		when(queryingImpl.resolve(uuid)).thenReturn(completedFuture(name), completedFuture(null));

		assertEquals(name, lookupName(uuid));
		assertEquals(name, lookupName(uuid), "Name should be cached");

		verify(queryingImpl).resolve(uuid);
	}

	@Test
	public void resolveAddressQueried() {
		when(queryingImpl.resolveAddress(name)).thenReturn(completedFuture(address));

		assertEquals(address, lookupAddress(name));

		verify(queryingImpl).resolveAddress(name);
	}

	private RemoteApiBundle mockRemoteApiBundle() {
		MainConfig mainConfig = mock(MainConfig.class);
		UUIDResolutionConfig uuidResolution = mock(UUIDResolutionConfig.class);
		RemoteApiBundle remoteApiBundle = mock(RemoteApiBundle.class);
		when(configs.getMainConfig()).thenReturn(mainConfig);
		when(mainConfig.uuidResolution()).thenReturn(uuidResolution);
		when(uuidResolution.serverType()).thenReturn(ServerType.ONLINE);
		when(uuidResolution.remoteApis()).thenReturn(remoteApiBundle);
		return remoteApiBundle;
	}

	@Test
	public void resolveUUIDWebLookup() {
		when(queryingImpl.resolve(name)).thenReturn(completedFuture(null));

		RemoteApiBundle remoteApiBundle = mockRemoteApiBundle();
		when(remoteApiBundle.lookup(any())).thenReturn(
				completedFuture(uuid), completedFuture(null));

		assertEquals(uuid, lookupUUID(name));
		assertEquals(uuid, lookupUUID(name), "UUID should be cached");
	}

	@Test
	public void resolveNameWebLookup() {
		when(queryingImpl.resolve(uuid)).thenReturn(completedFuture(null));

		RemoteApiBundle remoteApiBundle = mockRemoteApiBundle();
		when(remoteApiBundle.lookup(any())).thenReturn(
				completedFuture(name), completedFuture(null));

		assertEquals(name, lookupName(uuid));
		assertEquals(name, lookupName(uuid), "Name should be cached");
	}

	@Test
	public void resolveUUIDExplicitlyCached() {
		uuidManager.addCache(uuid, name);

		assertEquals(uuid, lookupUUID(name));
	}

	@Test
	public void resolveNameExplicitlyCached() {
		uuidManager.addCache(uuid, name);

		assertEquals(name, lookupName(uuid));
	}

	@Test
	public void badName() {
		String badName = "lol_haha_dead";
		when(nameValidator.validateNameArgument(badName)).thenReturn(false);

		assertNull(lookupUUID(badName));
		assertNull(lookupAddress(badName));

		verify(nameValidator, times(2)).validateNameArgument(badName);
	}

}
