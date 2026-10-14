/* 
 * LibertyBans-env-velocity
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-velocity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-velocity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-velocity. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.velocity;

import jakarta.inject.Singleton;

import space.arim.libertybans.core.importing.PlatformImportSource;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;

import space.arim.api.env.PlatformHandle;
import space.arim.api.env.VelocityPlatformHandle;

import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.env.Environment;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;

public class VelocityBindModule {

	@Singleton
	public Omnibus omnibus() {
		return OmnibusProvider.getOmnibus();
	}

	@Singleton
	public PlatformHandle handle(PluginContainer plugin, ProxyServer server) {
		return new VelocityPlatformHandle(plugin, server);
	}

	public Environment environment(VelocityEnv env) {
		return env;
	}

	public EnvEnforcer enforcer(VelocityEnforcer enforcer) {
		return enforcer;
	}

	public EnvUserResolver resolver(VelocityUserResolver resolver) {
		return resolver;
	}

	public PlatformImportSource platformImportSource() {
		throw new UnsupportedOperationException("PlatformImportSource not available");
	}

}
