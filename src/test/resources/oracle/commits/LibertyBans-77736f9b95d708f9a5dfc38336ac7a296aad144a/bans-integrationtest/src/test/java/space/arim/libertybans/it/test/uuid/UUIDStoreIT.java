/* 
 * LibertyBans-integrationtest
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-integrationtest is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * LibertyBans-integrationtest is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-integrationtest. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.it.test.uuid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.punish.Enforcer;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.util.RandomUtil;
import space.arim.libertybans.it.util.TestingUtil;

@ExtendWith(InjectionInvocationContextProvider.class)
public class UUIDStoreIT {

	private final UUIDManager uuidManager;

	public UUIDStoreIT(UUIDManager uuidManager) {
		this.uuidManager = uuidManager;
	}

	private static String randomName() {
		// Names too small tend to be in use
		return RandomUtil.randomString(12, 16);
	}

	private static NetworkAddress randomAddress() {
		return NetworkAddress.of(RandomUtil.randomAddress());
	}

	private String fullLookupName(UUID uuid) {
		return uuidManager.lookupName(uuid).join().orElse(null);
	}

	private UUID fullLookupUUID(String name) {
		return uuidManager.lookupUUID(name).join().orElse(null);
	}

	private NetworkAddress fullLookupAddress(String name) {
		return uuidManager.lookupAddress(name).join();
	}

	@TestTemplate
	public void testNoStoredUuid() {
		assertNull(fullLookupName(UUID.randomUUID()));
		assertNull(fullLookupUUID(randomName()));
	}

	@TestTemplate
	public void testWebLookupKnownProfile() {
		String name = "A248";
		UUID uuid = UUID.fromString("ed5f12cd-6007-45d9-a4b9-940524ddaecf");
		assertEquals(name, fullLookupName(uuid));
		assertEquals(uuid, fullLookupUUID(name));
	}

	@TestTemplate
	public void testStoreJoiningUuid(Enforcer enforcer) {
		UUID uuid = UUID.randomUUID();
		String name = randomName();
		NetworkAddress address = randomAddress();

		assumeTrue(null == enforcer.executeAndCheckConnection(uuid, name, address).join());

		assertEquals(name, fullLookupName(uuid));
		assertEquals(uuid, fullLookupUUID(name));
		assertEquals(address, fullLookupAddress(name));
	}

	@TestTemplate
	public void testUseLatestName(Enforcer enforcer) {
		UUID uuid = UUID.randomUUID();
		String name = randomName();
		NetworkAddress address = randomAddress();

		assumeTrue(null == enforcer.executeAndCheckConnection(uuid, name, address).join());

		assumeTrue(name.equals(fullLookupName(uuid)));
		assumeTrue(uuid.equals(fullLookupUUID(name)));
		assumeTrue(address.equals(fullLookupAddress(name)));

		String recentName = randomName();
		NetworkAddress recentAddress = randomAddress();

		/*
		 * Without this delay, the test continues too quickly, causing it to fail spuriously.
		 * This is because tracking of which name or address is more recent has only seconds precision.
		 * 
		 * In reality, it won't matter which address is chosen as more recent within the same second
		 */
		TestingUtil.sleepUnchecked(Duration.ofSeconds(1L));

		assumeTrue(null == enforcer.executeAndCheckConnection(uuid, recentName, recentAddress).join());

		assertEquals(recentName, fullLookupName(uuid), "Should use most recent name");
		assertEquals(recentAddress, fullLookupAddress(recentName), "Should use most recent address with most recent name");
		assertEquals(recentAddress, fullLookupAddress(name), "Should use most recent address with past name");

		assertEquals(uuid, fullLookupUUID(recentName));
		assertEquals(uuid, fullLookupUUID(name), "Should still be able to look up by past name");
	}

}
