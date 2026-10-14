/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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
package space.arim.libertybans.core.commands.extra;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.api.chat.SendableMessage;
import space.arim.api.chat.manipulator.SendableMessageManipulator;
import space.arim.api.chat.serialiser.LegacyCodeSerialiser;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.MessagesConfig;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StandardArgumentParserTest {

	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();
	private final Configs configs;
	private final UUIDManager uuidManager;
	private final CmdSender sender;

	private final ArgumentParser argumentParser;

	private final MessagesConfig messagesConfig;
	private final MessagesConfig.All.NotFound notFound;

	private final SendableMessage notFoundMessage = LegacyCodeSerialiser.getInstance('&').deserialise("&cNot found");

	public StandardArgumentParserTest(@Mock Configs configs, @Mock UUIDManager uuidManager, @Mock CmdSender sender,
									  @Mock MessagesConfig messagesConfig, @Mock MessagesConfig.All.NotFound notFound) {
		this.configs = configs;
		this.uuidManager = uuidManager;
		this.sender = sender;
		this.messagesConfig = messagesConfig;
		this.notFound = notFound;

		argumentParser = new StandardArgumentParser(futuresFactory, configs, uuidManager);
	}

	@BeforeEach
	public void setupMocks() {
		lenient().when(configs.getMessagesConfig()).thenReturn(messagesConfig).getMock();
		MessagesConfig.All all = mock(MessagesConfig.All.class);
		lenient().when(messagesConfig.all()).thenReturn(all);
		lenient().when(all.notFound()).thenReturn(notFound);
	}

	private <T> CentralisedFuture<T> completedFuture(T value) {
		return futuresFactory.completedFuture(value);
	}

	/*
	 * parseVictimByName testing
	 */

	private Victim parseVictimByName(String targetArg) {
		return argumentParser.parseVictimByName(sender, targetArg).join();
	}

	@Test
	public void directPlayerVictim() {
		UUID uuid = UUID.randomUUID();

		assertEquals(PlayerVictim.of(uuid), parseVictimByName(uuid.toString()));
		assertEquals(PlayerVictim.of(uuid), parseVictimByName(uuid.toString().replace("-", "")));

		verify(sender, never()).sendMessage(any());
	}

	@Test
	public void lookupPlayerVictim() {
		UUID uuid = UUID.randomUUID();
		String name = "ObsidianWolf_";
		when(uuidManager.lookupUUID(name)).thenReturn(completedFuture(Optional.of(uuid)));

		assertEquals(PlayerVictim.of(uuid), parseVictimByName(name));

		verify(uuidManager).lookupUUID(name);
		verify(sender, never()).sendMessage(any());
	}

	@Test
	public void unknownPlayerVictimForName() {
		String name = "A248";
		when(uuidManager.lookupUUID(name)).thenReturn(completedFuture(Optional.empty()));
		when(notFound.player()).thenReturn(SendableMessageManipulator.create(notFoundMessage));

		assertNull(parseVictimByName(name));

		verify(uuidManager).lookupUUID(name);
		verify(sender).sendMessage(notFoundMessage);
	}

	/*
	 * parseOperatorByName testing
	 */

	private Operator parseOperatorByName(String targetArg) {
		return argumentParser.parseOperatorByName(sender, targetArg).join();
	}

	private void mockConsoleArguments(Set<String> consoleArguments) {
		MessagesConfig.Formatting formatting = mock(MessagesConfig.Formatting.class);
		when(messagesConfig.formatting()).thenReturn(formatting);
		when(formatting.consoleArguments()).thenReturn(consoleArguments);
	}

	@Test
	public void directPlayerOperator() {
		UUID uuid = UUID.randomUUID();
		mockConsoleArguments(Set.of());

		assertEquals(PlayerOperator.of(uuid), parseOperatorByName(uuid.toString()));
		assertEquals(PlayerOperator.of(uuid), parseOperatorByName(uuid.toString().replace("-", "")));

		verify(sender, never()).sendMessage(any());
	}

	@Test
	public void lookupPlayerOperator() {
		UUID uuid = UUID.randomUUID();
		String name = "ObsidianWolf_";
		mockConsoleArguments(Set.of());
		when(uuidManager.lookupUUID(name)).thenReturn(completedFuture(Optional.of(uuid)));

		assertEquals(PlayerOperator.of(uuid), parseOperatorByName(name));

		verify(uuidManager).lookupUUID(name);
		verify(sender, never()).sendMessage(any());
	}

	@Test
	public void unknownPlayerOperatorForName() {
		String name = "A248";
		mockConsoleArguments(Set.of());
		when(uuidManager.lookupUUID(name)).thenReturn(completedFuture(Optional.empty()));
		when(notFound.player()).thenReturn(SendableMessageManipulator.create(notFoundMessage));

		assertNull(parseOperatorByName(name));

		verify(uuidManager).lookupUUID(name);
		verify(sender).sendMessage(notFoundMessage);
	}

	@Test
	public void directConsoleOperator() {
		mockConsoleArguments(Set.of("console"));

		assertEquals(ConsoleOperator.INSTANCE, parseOperatorByName("console"));
	}

	/*
	 * parseAddressVictim testing
	 */

	private Victim parseAddressVictim(String targetArg) {
		return argumentParser.parseAddressVictim(sender, targetArg).join();
	}

	@Test
	public void parseFixedIpv4() {
		NetworkAddress address = NetworkAddress.of(new byte[] {
				(byte) 127, (byte) 0, (byte) 255, (byte) 38});
		assertEquals(AddressVictim.of(address), parseAddressVictim("127.0.255.38"));
		verify(uuidManager, never()).lookupAddress(any());
		verify(sender, never()).sendMessage(any());
	}
	
	private static NetworkAddress randomIpv4() {
		Random random = ThreadLocalRandom.current();
		byte[] result = new byte[4];
		random.nextBytes(result);
		return NetworkAddress.of(result);
	}
	
	@Test
	public void parseRandomIpv4() {
		NetworkAddress address = randomIpv4();
		List<String> octets = new ArrayList<>(4);
		for (byte octet : address.getRawAddress()) {
			octets.add(Integer.toString(Byte.toUnsignedInt(octet)));
		}
		assertEquals(AddressVictim.of(address), parseAddressVictim(String.join(".", octets)));
		verify(uuidManager, never()).lookupAddress(any());
		verify(sender, never()).sendMessage(any());
	}

	@Test
	public void lookupAddressVictim() {
		String name = "A248";
		NetworkAddress address = randomIpv4();
		when(uuidManager.lookupAddress(name)).thenReturn(completedFuture(address));

		assertEquals(AddressVictim.of(address), parseAddressVictim(name));

		verify(uuidManager).lookupAddress(name);
		verify(sender, never()).sendMessage(any());
	}

	@Test
	public void unknownAddressVictimForName() {
		String name = "A248";
		when(uuidManager.lookupAddress(name)).thenReturn(completedFuture(null));
		when(notFound.playerOrAddress()).thenReturn(SendableMessageManipulator.create(notFoundMessage));

		assertNull(parseAddressVictim(name));

		verify(uuidManager).lookupAddress(name);
		verify(sender).sendMessage(notFoundMessage);
	}
	
}
