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

import java.util.Objects;

import jakarta.inject.Singleton;

import space.arim.libertybans.api.scope.ServerScope;

@Singleton
public class Scoper implements InternalScopeManager {

	public Scoper() {

	}
	
	@Override
	public ServerScope specificScope(String server) {
		Objects.requireNonNull(server, "server");
		return ScopeImpl.create(server);
	}

	@Override
	public ServerScope globalScope() {
		return ScopeImpl.GLOBAL;
	}
	
	@Override
	public ServerScope currentServerScope() {
		return ScopeImpl.GLOBAL; // TODO implement scopes
	}
	
	@Override
	public String getServer(ServerScope scope, String defaultIfGlobal) {
		if (!(scope instanceof ScopeImpl)) {
			throw new IllegalArgumentException("Foreign implementation of Scope: " + scope.getClass());
		}
		String server = ((ScopeImpl) scope).server();
		return server.isEmpty() ? defaultIfGlobal : server;
	}
	
	@Override
	public void checkScope(ServerScope scope) {
		if (scope == null) {
			throw new NullPointerException("scope");
		}
		if (!(scope instanceof ScopeImpl)) {
			throw new IllegalArgumentException("Foreign implementation of scope " + scope.getClass());
		}
	}

}
