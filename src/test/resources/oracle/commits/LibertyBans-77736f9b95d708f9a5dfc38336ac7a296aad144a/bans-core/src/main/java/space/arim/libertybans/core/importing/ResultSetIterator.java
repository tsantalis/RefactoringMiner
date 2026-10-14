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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.omnibus.util.ThisClass;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

class ResultSetIterator<T> implements Iterator<T>, AutoCloseable {

	private final SchemaRowMapper<T> schemaRowMapper;

	private final PreparedStatement prepStmt;
	private final ResultSet resultSet;
	private boolean endOfResult;

	private final int retrievalSize;
	private final List<T> buffer;
	private Iterator<T> bufferIterator;

	// Statistics
	private int totalCount;
	private int skipped;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	ResultSetIterator(SchemaRowMapper<T> schemaRowMapper, Connection connection, int retrievalSize)
			throws SQLException {
		this.schemaRowMapper = schemaRowMapper;

		prepStmt = connection.prepareStatement(schemaRowMapper.selectStatement());
		try {
			prepStmt.setFetchSize(retrievalSize);
			resultSet = prepStmt.executeQuery();
		} catch (SQLException ex) {
			try {
				prepStmt.close();
			} catch (SQLException suppressed) { ex.addSuppressed(suppressed); }
			throw ex;
		}

		this.retrievalSize = retrievalSize;
		buffer = new ArrayList<>(retrievalSize);
	}

	@Override
	public boolean hasNext() {
		if (bufferIterator != null && bufferIterator.hasNext()) {
			return true;
		}
		if (endOfResult) {
			return false;
		}
		buffer.clear();
		try {
			for (int count = retrievalSize; count > 0; count--) {
				if (!resultSet.next()) {
					endOfResult = true;
					logger.trace("Received end of ResultSet");
					break;
				}
				Optional<T> mapped = schemaRowMapper.mapRow(resultSet);
				mapped.ifPresentOrElse(buffer::add, () -> skipped++);
			}
		} catch (SQLException ex) {
			throw new ImportException(ex);
		}
		if (buffer.isEmpty()) {
			logger.trace("buffer is empty; received no rows");
			return false;
		}
		if (totalCount > 0) {
			logger.info("Iterated over " + totalCount + " rows so far. Iterating further...");
		}
		totalCount += buffer.size();
		bufferIterator = buffer.iterator();
		return true;
	}

	@Override
	public T next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		return bufferIterator.next();
	}

	@Override
	public void close() throws SQLException {
		try {
			resultSet.close();
		} catch (SQLException ex) {
			try {
				prepStmt.close();
			} catch (SQLException suppressed) { ex.addSuppressed(suppressed); }
			throw ex;
		}
		prepStmt.close();

		if (skipped > 0) {
			logger.info("Skipping " + skipped + " rows.");
		}
	}
}
