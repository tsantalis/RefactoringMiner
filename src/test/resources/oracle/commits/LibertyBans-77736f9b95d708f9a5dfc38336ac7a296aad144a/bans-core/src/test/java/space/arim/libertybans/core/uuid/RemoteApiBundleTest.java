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

package space.arim.libertybans.core.uuid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.api.util.web.RemoteApiResult;
import space.arim.api.util.web.RemoteNameHistoryApi;
import space.arim.api.util.web.UUIDUtil;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RemoteApiBundleTest {

	private final UUID uuid = UUIDUtil.fromShortString("ed5f12cd600745d9a4b9940524ddaecf");
	private final String name = "A248";

	private <T> CompletableFuture<RemoteApiResult<T>> completedResult(T value) {
		return CompletableFuture.completedFuture(new RemoteApiResult<>(
				value, RemoteApiResult.ResultType.FOUND, null));
	}

	private <T> CompletableFuture<RemoteApiResult<T>> emptyResult() {
		return CompletableFuture.completedFuture(new RemoteApiResult<>(
				null, RemoteApiResult.ResultType.NOT_FOUND, null));
	}

	@Test
	public void empty() {
		RemoteApiBundle remoteApiBundle = new RemoteApiBundle(List.of());
		assertNull(remoteApiBundle.lookup((remoteApi) -> remoteApi.lookupName(uuid)).join());
	}

	@Test
	public void oneResolver() {
		RemoteNameHistoryApi remoteNameHistoryApi = mock(RemoteNameHistoryApi.class);
		when(remoteNameHistoryApi.lookupUUID(name)).thenReturn(completedResult(uuid));
		when(remoteNameHistoryApi.lookupName(uuid)).thenReturn(completedResult(name));
		RemoteApiBundle remoteApiBundle = new RemoteApiBundle(List.of(remoteNameHistoryApi));

		assertEquals(uuid, remoteApiBundle.lookup((remoteApi) -> remoteApi.lookupUUID(name)).join());
		assertEquals(name, remoteApiBundle.lookup((remoteApi) -> remoteApi.lookupName(uuid)).join());

		verify(remoteNameHistoryApi).lookupUUID(name);
		verify(remoteNameHistoryApi).lookupName(uuid);
	}

	@Test
	public void multipleResolvers() {
		// One of the objectives here is to ensure remote APIs are not sent a burst of requests.
		// Specifically, if the first remote API finds an answer, don't query the rest.
		RemoteNameHistoryApi inconsistentRemoteApi = mock(RemoteNameHistoryApi.class);
		when(inconsistentRemoteApi.lookupUUID(name)).thenReturn(completedResult(uuid), emptyResult());
		when(inconsistentRemoteApi.lookupName(uuid)).thenReturn(completedResult(name), emptyResult());
		RemoteNameHistoryApi consistentRemoteApi = mock(RemoteNameHistoryApi.class);
		when(consistentRemoteApi.lookupUUID(name)).thenReturn(completedResult(uuid), completedResult(uuid));
		when(consistentRemoteApi.lookupName(uuid)).thenReturn(completedResult(name), completedResult(name));
		RemoteApiBundle remoteApiBundle = new RemoteApiBundle(List.of(inconsistentRemoteApi, consistentRemoteApi));

		assertEquals(uuid, remoteApiBundle.lookup((remoteApi) -> remoteApi.lookupUUID(name)).join());
		assertEquals(name, remoteApiBundle.lookup((remoteApi) -> remoteApi.lookupName(uuid)).join());
		assertEquals(uuid, remoteApiBundle.lookup((remoteApi) -> remoteApi.lookupUUID(name)).join());
		assertEquals(name, remoteApiBundle.lookup((remoteApi) -> remoteApi.lookupName(uuid)).join());

		verify(inconsistentRemoteApi, times(2)).lookupUUID(name);
		verify(inconsistentRemoteApi, times(2)).lookupName(uuid);
		verify(consistentRemoteApi).lookupUUID(name);
		verify(consistentRemoteApi).lookupName(uuid);
	}
}
