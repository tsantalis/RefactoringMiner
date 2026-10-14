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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public interface SchemaRowMapper<T> {

	String selectStatement();

	/**
	 * Maps this row to an object representation, or skips this row
	 *
	 * @param resultSet the result set
	 * @return the relational object, or an empty optional to skip this row
	 * @throws SQLException from the result set
	 */
	Optional<T> mapRow(ResultSet resultSet) throws SQLException;

}
