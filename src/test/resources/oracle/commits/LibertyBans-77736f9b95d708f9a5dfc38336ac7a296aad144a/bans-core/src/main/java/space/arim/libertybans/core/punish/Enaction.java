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

package space.arim.libertybans.core.punish;

import space.arim.jdbcaesar.QuerySource;
import space.arim.jdbcaesar.mapper.UpdateCountMapper;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;

import java.sql.SQLException;
import java.util.Objects;

public class Enaction {

	private final OrderDetails orderDetails;
	private final PunishmentCreator creator;

	public Enaction(OrderDetails orderDetails, PunishmentCreator creator) {
		this.orderDetails = orderDetails;
		this.creator = creator;
	}

	public OrderDetails orderDetails() {
		return orderDetails;
	}

	public interface Rollback {
		void rollback() throws SQLException;
	}

	public Punishment enactActive(QuerySource<?> querySource, Rollback rollback) throws SQLException {
		return enact(querySource, Objects.requireNonNull(rollback), true);
	}

	public Punishment enactHistorical(QuerySource<?> querySource) throws SQLException {
		return enact(querySource, null, false);
	}

	private Punishment enact(QuerySource<?> querySource, Rollback rollback, boolean active)
			throws SQLException {

		final PunishmentType type = orderDetails.type();
		final Victim victim = orderDetails.victim();
		final Operator operator = orderDetails.operator();
		final String reason = orderDetails.reason();
		final ServerScope scope = orderDetails.scope();
		final long start = orderDetails.start();
		final long end = orderDetails.end();

		int id = querySource.query(
				"INSERT INTO `libertybans_punishments` (`type`, `operator`, `reason`, `scope`, `start`, `end`) "
						+ "VALUES (?, ?, ?, ?, ?, ?)")
				.params(
						type, operator, reason,
						scope, start, end)
				.updateGenKeys((updateCount, genKeys) ->  {
					if (!genKeys.next()) {
						throw new IllegalStateException("No punishment ID generated for insertion query");
					}
					return genKeys.getInt("id");
				}).execute();

		if (active && type != PunishmentType.KICK) { // Kicks are pure history

			String enactStatement = " INTO `libertybans_" + type + "s` "
					+ "(`id`, `victim`, `victim_type`) VALUES (?, ?, ?)";
			Object[] enactArgs = new Object[] {id, victim, victim.getType()};

			if (type.isSingular()) {
				int updateCount = querySource.query("INSERT IGNORE" + enactStatement).params(enactArgs)
						.updateCount(UpdateCountMapper.identity()).execute();
				if (updateCount == 0) {
					// There is already a punishment of this type for this victim
					rollback.rollback();
					return null;
				}
			} else {
				querySource.query("INSERT" + enactStatement).params(enactArgs).voidResult().execute();
			}
		}

		querySource.query(
				"INSERT INTO `libertybans_history` (`id`, `victim`, `victim_type`) VALUES (?, ?, ?)")
				.params(id, victim, victim.getType())
				.voidResult().execute();
		return creator.createPunishment(id, type, victim, operator, reason, scope, start, end);
	}

	public static final class OrderDetails {

		private final PunishmentType type;
		private final Victim victim;
		private final Operator operator;
		private final String reason;
		private final ServerScope scope;
		private final long start;
		private final long end;

		public OrderDetails(PunishmentType type, Victim victim, Operator operator,
							String reason, ServerScope scope, long start, long end) {
			this.type = type;
			this.victim = victim;
			this.operator = operator;
			this.reason = reason;
			this.scope = scope;
			this.start = start;
			this.end = end;
		}

		public PunishmentType type() {
			return type;
		}

		public Victim victim() {
			return victim;
		}

		public Operator operator() {
			return operator;
		}

		public String reason() {
			return reason;
		}

		public ServerScope scope() {
			return scope;
		}

		public long start() {
			return start;
		}

		public long end() {
			return end;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			OrderDetails that = (OrderDetails) o;
			return type == that.type
					&& victim.equals(that.victim) && operator.equals(that.operator)
					&& reason.equals(that.reason) && scope.equals(that.scope)
					&& start == that.start && end == that.end;
		}

		@Override
		public int hashCode() {
			int result = type.hashCode();
			result = 31 * result + victim.hashCode();
			result = 31 * result + operator.hashCode();
			result = 31 * result + reason.hashCode();
			result = 31 * result + scope.hashCode();
			result = 31 * result + (int) (start ^ (start >>> 32));
			result = 31 * result + (int) (end ^ (end >>> 32));
			return result;
		}
	}

}
