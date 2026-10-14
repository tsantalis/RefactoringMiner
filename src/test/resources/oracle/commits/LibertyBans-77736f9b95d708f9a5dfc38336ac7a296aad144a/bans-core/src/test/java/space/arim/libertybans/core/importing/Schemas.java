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

import space.arim.jdbcaesar.QuerySource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

class Schemas {

	private final QuerySource<?> querySource;

	Schemas(QuerySource<?> querySource) {
		this.querySource = querySource;
	}

	private void setup(String schemaFile) {
		URL resource = getClass().getResource("/schemas/" + schemaFile + ".sql");
		String queries;
		try (InputStream inputStream = resource.openStream();
			 BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
			queries = new String(bufferedInputStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		for (String query : queries.split(";")) {
			querySource.query(query).voidResult().execute();
		}
	}

	void setupAdvancedBan() {
		setup("advancedban");
	}

	void setupLiteBans() {
		setup("litebans");
	}
}
