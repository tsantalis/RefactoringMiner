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
package space.arim.libertybans.core.selector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.libertybans.core.punish.PunishmentCreator;

@Singleton
public class SelectionImpl {

	private final FactoryOfTheFuture futuresFactory;
	private final Provider<InternalDatabase> dbProvider;
	private final PunishmentCreator creator;

	@Inject
	public SelectionImpl(FactoryOfTheFuture futuresFactory, Provider<InternalDatabase> dbProvider, PunishmentCreator creator) {
		this.futuresFactory = futuresFactory;
		this.dbProvider = dbProvider;
		this.creator = creator;
	}

	/*
	 * The objective is to dynamically build SQL queries while avoiding
	 * security-poor concatenated SQL.
	 * 
	 * When selecting non-active punishments from the history table,
	 * the SELECT statement must be qualified by the punishment type
	 */

	private static String getColumns(InternalSelectionOrder selection) {
		StringJoiner columns = new StringJoiner(", ");
		columns.add("`id`");
		if (selection.getVictimNullable() == null) {
			columns.add("`victim`");
			columns.add("`victim_type`");
		}
		if (selection.getOperatorNullable() == null) {
			columns.add("`operator`");
		}
		columns.add("`reason`");
		if (selection.getScopeNullable() == null) {
			columns.add("`scope`");
		}
		columns.add("`start`");
		columns.add("`end`");

		return columns.toString();
	}

	private static Map.Entry<String, Object[]> getPredication(InternalSelectionOrder selection) {
		StringJoiner predicates = new StringJoiner(" AND ");
		List<Object> params = new ArrayList<>();

		Victim victim = selection.getVictimNullable();
		if (victim != null) {
			predicates.add("`victim` = ? AND `victim_type` = ?");
			params.add(victim);
			params.add(victim.getType());
		}
		Operator operator = selection.getOperatorNullable();
		if (operator != null) {
			predicates.add("`operator` = ?");
			params.add(operator);
		}
		ServerScope scope = selection.getScopeNullable();
		if (scope != null) {
			predicates.add("`scope` = ?");
			params.add(scope);
		}
		if (selection.selectActiveOnly()) {
			predicates.add("(`end` = 0 OR `end` > ?)");
			params.add(MiscUtil.currentTime());
		} else {
			predicates.add("`type` = ?");
			params.add(selection.getType());
		}
		return Map.entry(predicates.toString(), params.toArray());
	}

	private Punishment fromResultSetAndSelection(InternalDatabase database, ResultSet resultSet,
			InternalSelectionOrder selection) throws SQLException {
		PunishmentType type = selection.getType();
		Victim victim = selection.getVictimNullable();
		Operator operator = selection.getOperatorNullable();
		ServerScope scope = selection.getScopeNullable();
		return creator.createPunishment(
				resultSet.getInt("id"),
				type,
				(victim == null) ? database.getVictimFromResult(resultSet) : victim,
				(operator == null) ? database.getOperatorFromResult(resultSet) : operator,
				database.getReasonFromResult(resultSet),
				(scope == null) ? database.getScopeFromResult(resultSet) : scope,
				database.getStartFromResult(resultSet),
				database.getEndFromResult(resultSet));
	}
	
	private static Map.Entry<String, Object[]> getSelectionQuery(InternalSelectionOrder selection) {
		String columns = getColumns(selection);
		Map.Entry<String, Object[]> predication = getPredication(selection);

		StringBuilder statementBuilder = new StringBuilder("SELECT ");
		statementBuilder.append(columns).append(" FROM `libertybans_");

		PunishmentType type = selection.getType();
		if (selection.selectActiveOnly()) {
			assert type != PunishmentType.KICK : type;
			statementBuilder.append("simple_").append(type.toString()).append('s');
		} else {
			// getPredication will be responsible for narrowing the type selected
			statementBuilder.append("simple_history");
		}
		statementBuilder.append('`');

		String predicates = predication.getKey();
		if (!predicates.isEmpty()) {
			statementBuilder.append(" WHERE ").append(predicates);
		}
		statementBuilder.append(" ORDER BY `start` DESC");
		int maximumToRetrieve = selection.maximumToRetrieve();
		if (maximumToRetrieve != 0) {
			statementBuilder.append(" LIMIT " + maximumToRetrieve);
		}
		return Map.entry(statementBuilder.toString(), predication.getValue());
	}
	
	CentralisedFuture<Punishment> getFirstSpecificPunishment(InternalSelectionOrder selection) {
		if (selection.selectActiveOnly() && selection.getType() == PunishmentType.KICK) {
			// Kicks cannot possibly be active. They are all history
			return futuresFactory.completedFuture(null);
		}
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {
			Map.Entry<String, Object[]> query = getSelectionQuery(selection);
			return database.jdbCaesar().query(
					query.getKey())
					.params(query.getValue())
					.singleResult((resultSet) -> {
						return fromResultSetAndSelection(database, resultSet, selection);
					}).execute();
		});
	}
	
	ReactionStage<List<Punishment>> getSpecificPunishments(InternalSelectionOrder selection) {
		if (selection.selectActiveOnly() && selection.getType() == PunishmentType.KICK) {
			// Kicks cannot possibly be active. They are all history
			return futuresFactory.completedFuture(List.of());
		}
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {

			Map.Entry<String, Object[]> query = getSelectionQuery(selection);
			int skipCount = selection.skipCount();

			List<Punishment> result = database.jdbCaesar().query(
					query.getKey())
					.params(query.getValue())
					.totalResult((resultSet) -> {

						for (int toSkipRemaining = skipCount; toSkipRemaining > 0; toSkipRemaining--) {
							if (!resultSet.next()) {
								return List.<Punishment>of();
							}
						}
						List<Punishment> punishments = new ArrayList<>();
						while (resultSet.next()) {
							punishments.add(fromResultSetAndSelection(database, resultSet, selection));
						}
						return punishments;
					}).execute();

			return List.copyOf(result);
		});
	}

}
