
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

package space.arim.libertybans.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FormatterTest {

	private Formatter formatterWithTime(MessagesConfig.Misc.Time timeConf) {
		Configs configs = mock(Configs.class);
		MessagesConfig messagesConfig = mock(MessagesConfig.class);
		MessagesConfig.Misc messagesConfigMisc = mock(MessagesConfig.Misc.class);

		when(configs.getMessagesConfig()).thenReturn(messagesConfig);
		when(messagesConfig.misc()).thenReturn(messagesConfigMisc);
		when(messagesConfigMisc.time()).thenReturn(timeConf);

		return new Formatter(
				mock(FactoryOfTheFuture.class), configs,
				mock(InternalScopeManager.class), mock(UUIDManager.class));
	}

	@Test
	public void formatRelative() {
		MessagesConfig.Misc.Time timeConf = mock(MessagesConfig.Misc.Time.class);
		when(timeConf.fragments()).thenReturn(Map.of(
				ChronoUnit.SECONDS, "%VALUE% seconds",
				ChronoUnit.MINUTES, "%VALUE% minutes",
				ChronoUnit.HOURS, "%VALUE% hours",
				ChronoUnit.DAYS, "%VALUE% days"));
		when(timeConf.useComma()).thenReturn(true);
		when(timeConf.and()).thenReturn("and ");
		
		Formatter formatter = formatterWithTime(timeConf);

		assertEquals("3 minutes, and 5 seconds",
				formatter.formatRelative(3L * 60L + 5L));
		assertEquals("4 hours, 15 minutes, and 58 seconds",
				formatter.formatRelative(4L * 60L * 60L + 15L * 60L + 58L));
		assertEquals("2 days, 4 minutes, and 40 seconds",
				formatter.formatRelative(2L * 24L * 60L * 60L + 4L * 60L + 40L));
	}
}
