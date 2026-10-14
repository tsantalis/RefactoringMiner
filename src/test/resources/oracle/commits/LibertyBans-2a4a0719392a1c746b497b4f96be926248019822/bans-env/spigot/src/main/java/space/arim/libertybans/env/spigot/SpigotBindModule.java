/* 
 * LibertyBans-env-spigot
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-spigot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-spigot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-spigot. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.spigot;

import jakarta.inject.Singleton;

import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;

import space.arim.api.env.BukkitPlatformHandle;
import space.arim.api.env.PlatformHandle;

import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.env.Environment;

import org.bukkit.plugin.java.JavaPlugin;

public class SpigotBindModule {

	@Singleton
	public Omnibus omnibus() {
		return OmnibusProvider.getOmnibus();
	}

	@Singleton
	public PlatformHandle handle(JavaPlugin plugin) {
		return new BukkitPlatformHandle(plugin);
	}

	public Environment environment(SpigotEnv env) {
		return env;
	}

	public EnvEnforcer enforcer(SpigotEnforcer enforcer) {
		return enforcer;
	}

	public EnvUserResolver resolver(SpigotUserResolver resolver) {
		return resolver;
	}

	@Singleton
	public CommandMapHelper commandMapHelper(SimpleCommandMapHelper scmh) {
		return new CachingCommandMapHelper(scmh);
	}

}
