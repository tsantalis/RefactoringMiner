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
package space.arim.libertybans.core;

import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.core.punish.Enforcer;
import space.arim.libertybans.core.punish.InternalRevoker;
import space.arim.libertybans.core.punish.PunishmentCreator;
import space.arim.libertybans.core.punish.Revoker;
import space.arim.libertybans.core.punish.SecurePunishmentCreator;
import space.arim.libertybans.core.punish.StandardEnforcer;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.libertybans.core.scope.Scoper;
import space.arim.libertybans.core.selector.CaffeineMuteCache;
import space.arim.libertybans.core.selector.InternalSelector;
import space.arim.libertybans.core.selector.MuteCache;
import space.arim.libertybans.core.selector.SelectorImpl;

public abstract class PillarOneBindModuleMinusConfigs {

	public BaseFoundation foundation(LifecycleGodfather godfather) {
		return godfather;
	}

	public PunishmentCreator creator(SecurePunishmentCreator creator) {
		return creator;
	}

	public InternalRevoker revoker(Revoker revoker) {
		return revoker;
	}

	public Enforcer enforcer(StandardEnforcer enforcer) {
		return enforcer;
	}

	public MuteCache muteCache(CaffeineMuteCache muteCache) {
		return muteCache;
	}

	public InternalSelector selector(SelectorImpl selector) {
		return selector;
	}

	public InternalScopeManager scopeManager(Scoper scopeManager) {
		return scopeManager;
	}

}
