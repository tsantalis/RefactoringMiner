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

package space.arim.libertybans.core.importing;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class DatabaseStream {

	private final ConnectionSource connectionSource;
	private final int retrievalSize;

	DatabaseStream(ConnectionSource connectionSource, int retrievalSize) {
		this.connectionSource = Objects.requireNonNull(connectionSource);
		this.retrievalSize = retrievalSize;
	}

	<T> Stream<T> streamRows(SchemaRowMapper<T> schemaRowMapper) {
		Connection connection;
		try {
			connection = connectionSource.openConnection();
		} catch (SQLException ex) {
			throw new ImportException("Unable to open importing connection", ex);
		}
		ResultSetIterator<T> iterator;
		try {
			iterator = new ResultSetIterator<>(schemaRowMapper, connection, retrievalSize);
		} catch (SQLException ex) {
			try {
				connection.close();
			} catch (SQLException suppressed) { ex.addSuppressed(suppressed); }
			throw new ImportException("Unable to select imported punishments", ex);
		}
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator,
				Spliterator.DISTINCT + Spliterator.IMMUTABLE), false)
				.onClose(() -> {
					try {
						iterator.close();
					} catch (SQLException ex) {
						throw new ImportException("Failure closing import iterator", ex);
					}
				})
				.onClose(() -> {
					try {
						connection.close();
					} catch (SQLException ex) {
						throw new ImportException("Failure closing import connection", ex);
					}
				});
	}


}
