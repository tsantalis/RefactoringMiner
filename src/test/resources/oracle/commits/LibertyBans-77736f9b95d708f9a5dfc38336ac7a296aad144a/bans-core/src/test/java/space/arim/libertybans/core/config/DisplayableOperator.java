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

import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;

import java.util.Objects;
import java.util.UUID;

final class DisplayableOperator {

	private final Operator operator;
	private final String name;

	static final DisplayableOperator A248 = player(UUID.randomUUID(),
			"A248");
	static final DisplayableOperator CONSOLE = new DisplayableOperator(ConsoleOperator.INSTANCE, null);

	private DisplayableOperator(Operator operator, String name) {
		this.operator = Objects.requireNonNull(operator);
		this.name = name;
	}

	public String display() {
		if (this == A248) {
			return "A248";
		}
		assert this == CONSOLE;
		return "Console";
	}

	public String displayId() {
		if (this == A248) {
			return ((PlayerOperator) operator).getUUID().toString().replace("-", "");
		}
		assert this == CONSOLE;
		return "Console";
	}

	private static DisplayableOperator player(UUID uuid, String name) {
		return new DisplayableOperator(PlayerOperator.of(uuid), name);
	}

	public Operator operator() {
		return operator;
	}

	public String name() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DisplayableOperator that = (DisplayableOperator) o;
		return operator.equals(that.operator) && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		int result = operator.hashCode();
		result = 31 * result + (name != null ? name.hashCode() : 0);
		return result;
	}

}
