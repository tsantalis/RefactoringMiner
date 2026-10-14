/* 
 * LibertyBans-env-bungee
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-bungee is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-bungee is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-bungee. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.bungee;

import jakarta.inject.Singleton;

import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;

import space.arim.api.env.BungeePlatformHandle;
import space.arim.api.env.PlatformHandle;

import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.env.Environment;

import net.md_5.bungee.api.plugin.Plugin;

public class BungeeBindModule {

	@Singleton
	public Omnibus omnibus() {
		return OmnibusProvider.getOmnibus();
	}

	@Singleton
	public PlatformHandle handle(Plugin plugin) {
		return new BungeePlatformHandle(plugin);
	}

	public Environment environment(BungeeEnv env) {
		return env;
	}

	public EnvEnforcer enforcer(BungeeEnforcer enforcer) {
		return enforcer;
	}

	public EnvUserResolver resolver(BungeeUserResolver resolver) {
		return resolver;
	}

	public AddressReporter reporter(StandardAddressReporter reporter) {
		return reporter;
	}

}
