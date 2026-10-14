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

package space.arim.libertybans.core.importing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(LocalDatabaseSetup.class)
@LocalDatabaseSetup.Hsqldb
public class DatabaseStreamTest {

	private ConnectionSource connectionSource;
	private DatabaseStream databaseStream;

	@BeforeEach
	public void setup(ConnectionSource connectionSource) throws SQLException {
		this.connectionSource = connectionSource;
		databaseStream = new DatabaseStream(connectionSource, 2);

		try (Connection connection = connectionSource.openConnection();
			 PreparedStatement createTableStatement = connection.prepareStatement(
					 "CREATE TABLE test_rows (" +
							 "tally INT NOT NULL, " +
							 "textual VARCHAR(256) NOT NULL, " +
							 "flag BOOLEAN NOT NULL)")) {
			createTableStatement.execute();
		}
	}

	private void prepareData(PreparedStatement insertDataStatement, Row row) throws SQLException {
		insertDataStatement.setInt(1, row.tally());
		insertDataStatement.setString(2, row.text());
		insertDataStatement.setBoolean(3, row.flag());
	}

	private void insertAllData(Set<Row> rows) {
		try (Connection connection = connectionSource.openConnection();
			 PreparedStatement insertDataStatement = connection.prepareStatement(
			 		"INSERT INTO test_rows (tally, textual, flag) VALUES (?, ?, ?)")) {
			for (Row row : rows) {
				prepareData(insertDataStatement, row);
				insertDataStatement.addBatch();
			}
			insertDataStatement.executeBatch();
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	private Set<Row> sourceRows() {
		return databaseStream.streamRows(new SchemaRowMapper<Row>() {

			@Override
			public String selectStatement() {
				return "SELECT * FROM test_rows";
			}

			@Override
			public Optional<Row> mapRow(ResultSet resultSet) throws SQLException {
				return Optional.of(new Row(
						resultSet.getInt("tally"), resultSet.getString("textual"), resultSet.getBoolean("flag")));
			}
		}).collect(Collectors.toUnmodifiableSet());
	}

	@Test
	public void empty() {
		assertEquals(Set.of(), sourceRows());
	}

	@Test
	public void fetchSingleRow() throws SQLException {
		Set<Row> expectedRows = Set.of(new Row(-1, "some text", true));
		insertAllData(expectedRows);
		assertEquals(expectedRows, sourceRows());
	}

	@Test
	public void fetchMultipleRowsSingleRetrieval() throws SQLException {
		// Use less rows than retrieval size
		Set<Row> expectedRows = Set.of(
				new Row(-1, "some text", true),
				new Row(2, "second row", false));
		insertAllData(expectedRows);
		assertEquals(expectedRows, sourceRows());
	}

	@Test
	public void fetchMultipleRowsTwoRetrievals() {
		// Use enough rows to trigger 2 or more retrievals
		Set<Row> expectedRows = Set.of(
				new Row(-1, "some text", true),
				new Row(15, "bigger string values", false),
				new Row(50, "this is the last row, but may be ordered any which way", true));
		insertAllData(expectedRows);
		assertEquals(expectedRows, sourceRows());
	}

	@Test
	public void fetchMultipleRowsThreeRetrievals() {
		// Use enough rows to trigger 3 or more retrievals
		Set<Row> expectedRows = Set.of(
				new Row(-1, "some text", true),
				new Row(15, "bigger string values", false),
				new Row(50, "rows may be ordered any which way", true),
				new Row(14, "", true),
				new Row(29, "minus one", true),
				new Row(0, "248", false));
		insertAllData(expectedRows);
		assertEquals(expectedRows, sourceRows());
	}

	private static class Row {
		private final int tally;
		private final String text;
		private final boolean flag;

		Row(int tally, String text, boolean flag) {
			this.tally = tally;
			this.text = Objects.requireNonNull(text);
			this.flag = flag;
		}

		public int tally() {
			return tally;
		}

		public String text() {
			return text;
		}

		public boolean flag() {
			return flag;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Row row = (Row) o;
			return tally == row.tally && flag == row.flag && text.equals(row.text);
		}

		@Override
		public int hashCode() {
			int result = tally;
			result = 31 * result + text.hashCode();
			result = 31 * result + (flag ? 1 : 0);
			return result;
		}
	}
}
