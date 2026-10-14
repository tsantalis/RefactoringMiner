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

package space.arim.libertybans.core.config;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import space.arim.libertybans.api.PunishmentType;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class FormatterTestArgumentsProvider implements ArgumentsProvider {

	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
		String[] servers = new String[] {"global", "server one", "server 2"};
		DisplayableVictim[] victims = new DisplayableVictim[] {
				DisplayableVictim.ObWolf, DisplayableVictim.Address_198_27_31_42};
		DisplayableOperator[] operators = new DisplayableOperator[] {
				DisplayableOperator.A248, DisplayableOperator.CONSOLE};

		Set<FormatterTestInfo> tests = new HashSet<>();
		for (PunishmentType type : PunishmentType.values()) {
			for (String server : servers) {
				for (DisplayableVictim victim : victims) {
					for (DisplayableOperator operator : operators) {
						tests.add(new FormatterTestInfo(type, victim, operator, server, "you are punished"));
					}
				}
			}
		}
		return tests.stream().map(Arguments::of);
	}

}
