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

import java.util.UUID;

import jakarta.inject.Inject;

import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.uuid.UUIDManager;

public class StandardArgumentParser implements ArgumentParser {

	private final FactoryOfTheFuture futuresFactory;
	private final Configs configs;
	private final UUIDManager uuidManager;
	
	@Inject
	public StandardArgumentParser(FactoryOfTheFuture futuresFactory, Configs configs, UUIDManager uuidManager) {
		this.futuresFactory = futuresFactory;
		this.configs = configs;
		this.uuidManager = uuidManager;
	}
	
	private <T> CentralisedFuture<T> completedFuture(T value) {
		return futuresFactory.completedFuture(value);
	}
	
	// UUID from name
	
	private CentralisedFuture<UUID> parseOrLookupUUID(CmdSender sender, String targetArg) {
		switch (targetArg.length()) {
		case 36:
			UUID uuid;
			try {
				uuid = UUID.fromString(targetArg);
			}  catch (IllegalArgumentException ex) {
				sender.sendMessage(configs.getMessagesConfig().all().notFound().uuid().replaceText("%TARGET%", targetArg));
				return completedFuture(null);
			}
			return completedFuture(uuid);
		case 32:
			long mostSigBits;
			long leastSigBits;
			try {
				mostSigBits = Long.parseUnsignedLong(targetArg.substring(0, 16), 16);
				leastSigBits = Long.parseUnsignedLong(targetArg.substring(16, 32), 16);
			} catch (NumberFormatException ex) {
				sender.sendMessage(configs.getMessagesConfig().all().notFound().uuid().replaceText("%TARGET%", targetArg));
				return completedFuture(null);
			}
			return completedFuture(new UUID(mostSigBits, leastSigBits));
		default:
			break;
		}
		return uuidManager.lookupUUID(targetArg).thenApply((uuid) -> {
			if (uuid.isEmpty()) {
				sender.sendMessage(configs.getMessagesConfig().all().notFound().player().replaceText("%TARGET%", targetArg));
				return null;
			}
			return uuid.get();
		});
	}
	
	@Override
	public CentralisedFuture<Victim> parseVictimByName(CmdSender sender, String targetArg) {
		return parseOrLookupUUID(sender, targetArg).thenApply((uuid) -> {
			return (uuid == null) ? null : PlayerVictim.of(uuid);
		});
	}
	
	@Override
	public CentralisedFuture<Operator> parseOperatorByName(CmdSender sender, String operatorArg) {
		if (ContainsCI.containsIgnoreCase(configs.getMessagesConfig().formatting().consoleArguments(), operatorArg)) {
			return completedFuture(ConsoleOperator.INSTANCE);
		}
		return parseOrLookupUUID(sender, operatorArg).thenApply((uuid) -> {
			return (uuid == null) ? null : PlayerOperator.of(uuid);
		});
	}
	
	@Override
	public CentralisedFuture<Victim> parseAddressVictim(CmdSender sender, String targetArg) {
		byte[] parsedAddress = parseIpv4(targetArg);
		if (parsedAddress != null) {
			return completedFuture(AddressVictim.of(NetworkAddress.of(parsedAddress)));
		}
		return uuidManager.lookupAddress(targetArg).thenApply((address) -> {
			if (address == null) {
				sender.sendMessage(configs.getMessagesConfig().all().notFound().playerOrAddress().replaceText("%TARGET%", targetArg));
				return null;
			}
			return AddressVictim.of(address);
		});
	}

	private static byte[] parseIpv4(String targetArg) {
		String[] octetStrings = targetArg.split("\\.");
		if (octetStrings.length != 4) {
			return null;
		}
		byte[] ipv4 = new byte[4];
		for (int n = 0; n < 4; n++) {
			String octetString = octetStrings[n];
			int octet;
			try {
				octet = Integer.parseUnsignedInt(octetString);
			} catch (NumberFormatException ex) {
				return null;
			}
			if (octet < 0 || octet > 255) {
				return null;
			}
			ipv4[n] = (byte) octet;
		}
		return ipv4;
	}
	
}
