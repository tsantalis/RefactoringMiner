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
package space.arim.libertybans.core.scope;

import space.arim.libertybans.api.scope.ServerScope;

public final class ScopeImpl implements ServerScope {

	private final String server;
	
	public static final ServerScope GLOBAL = new ScopeImpl("");

	private ScopeImpl(String server) {
		if (server.length() > 32) {
			throw new IllegalArgumentException("Server must be 32 or less chars");
		}
		this.server = server;
	}

	/**
	 * Creates a scope applying to a specific server
	 *
	 * @param server the server name. cannot be empty
	 * @return the scope
	 */
	public static ScopeImpl create(String server) {
		if (server.isEmpty()) {
			throw new IllegalArgumentException("Server cannot be empty");
		}
		return new ScopeImpl(server);
	}

	String server() {
		return server;
	}

	@Override
	public boolean appliesTo(String server) {
		return this.server.isEmpty() || this.server.equals(server);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + server.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof ScopeImpl)) {
			return false;
		}
		ScopeImpl other = (ScopeImpl) object;
		return server.equals(other.server);
	}

	@Override
	public String toString() {
		return server.isEmpty() ? "global" : "server:" + server;
	}
	
}
