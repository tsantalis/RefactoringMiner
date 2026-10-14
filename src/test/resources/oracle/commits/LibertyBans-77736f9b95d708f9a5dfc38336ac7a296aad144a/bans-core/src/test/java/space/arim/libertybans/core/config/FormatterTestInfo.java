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

import space.arim.libertybans.api.PunishmentType;

final class FormatterTestInfo {

	private final PunishmentType type;
	private final DisplayableVictim victim;
	private final DisplayableOperator operator;
	private final String serverScope;
	private final String reason;

	FormatterTestInfo(PunishmentType type,
					  DisplayableVictim victim, DisplayableOperator operator,
					  String serverScope, String reason) {
		this.type = type;
		this.victim = victim;
		this.operator = operator;
		this.serverScope = serverScope;
		this.reason = reason;
	}

	public PunishmentType type() {
		return type;
	}

	public DisplayableVictim victim() {
		return victim;
	}

	public DisplayableOperator operator() {
		return operator;
	}

	public String serverScope() {
		return serverScope;
	}

	public String reason() {
		return reason;
	}

	public String formatVariables(String layout) {
		return layout
				.replace("%TYPE%", type.toString())
				.replace("%VICTIM%", victim.display())
				.replace("%VICTIM_ID%", victim.displayId())
				.replace("%OPERATOR%", operator.display())
				.replace("%OPERATOR_ID%", operator.displayId())
				.replace("%REASON%", reason);
	}

	@Override
	public String toString() {
		return "FormatterTestInfo{" +
				"type=" + type +
				", victim=" + victim +
				", operator=" + operator +
				", serverScope='" + serverScope + '\'' +
				", reason='" + reason + '\'' +
				'}';
	}
}
