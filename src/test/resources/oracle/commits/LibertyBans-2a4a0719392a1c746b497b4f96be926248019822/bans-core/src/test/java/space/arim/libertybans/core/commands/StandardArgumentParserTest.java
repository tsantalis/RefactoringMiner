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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

public class StandardArgumentParserTest {

	@Test
	public void testParseFixedIpv4() {
		byte[] address = new byte[] {(byte) 127, (byte) 0, (byte) 255, (byte) 38};
		assertArrayEquals(address, StandardArgumentParser.parseIpv4("127.0.255.38"));
	}
	
	private static byte[] randomIpv4() {
		Random random = ThreadLocalRandom.current();
		byte[] result = new byte[4];
		random.nextBytes(result);
		return result;
	}
	
	@Test
	public void testParseRandomIpv4() {
		byte[] address = randomIpv4();
		List<String> octets = new ArrayList<>(4);
		for (byte octet : address) {
			octets.add(Integer.toString(Byte.toUnsignedInt(octet)));
		}
		assertArrayEquals(address, StandardArgumentParser.parseIpv4(String.join(".", octets)));
	}
	
}
