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
package space.arim.libertybans.core.uuid;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;

import space.arim.api.util.web.HttpAshconApi;
import space.arim.api.util.web.HttpMcHeadsApi;
import space.arim.api.util.web.HttpMojangApi;
import space.arim.api.util.web.RemoteApiResult;
import space.arim.api.util.web.RemoteNameHistoryApi;
import space.arim.api.util.web.RemoteNameUUIDApi;

import space.arim.dazzleconf.error.BadValueException;
import space.arim.dazzleconf.serialiser.Decomposer;
import space.arim.dazzleconf.serialiser.FlexibleType;
import space.arim.dazzleconf.serialiser.ValueSerialiser;

public class RemoteApiBundle {
	
	private final List<RemoteNameHistoryApi> remotes;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	RemoteApiBundle(List<RemoteNameHistoryApi> remotes) {
		this.remotes = List.copyOf(remotes);
	}

	private <T> T unboxResult(RemoteNameHistoryApi remoteApi, RemoteApiResult<T> remoteApiResult) {
		switch (remoteApiResult.getResultType()) {
		case FOUND:
		case NOT_FOUND:
			break;
		case RATE_LIMITED:
		case ERROR:
		case UNKNOWN:
		default:
			Exception ex = remoteApiResult.getException();
			if (ex == null) {
				logger.warn("Request for name to remote web API {} failed", remoteApi);
			} else {
				logger.warn("Request for name to remote web API {} failed", remoteApi, ex);
			}
			return null;
		}
		return remoteApiResult.getValue();
	}
	
	<T> CompletableFuture<T> lookup(Function<RemoteNameUUIDApi, CompletableFuture<RemoteApiResult<T>>> intermediateResultFunction) {
		CompletableFuture<T> future = null;
		for (RemoteNameHistoryApi remoteApi : remotes) {

			if (future == null) {
				future = intermediateResultFunction.apply(remoteApi)
						.thenApply((remoteApiResult) -> unboxResult(remoteApi, remoteApiResult));
			} else {
				future = future.thenCompose((result) -> {
					if (result != null) {
						return CompletableFuture.completedFuture(result);
					}
					return intermediateResultFunction.apply(remoteApi)
							.thenApply((remoteApiResult) -> unboxResult(remoteApi, remoteApiResult));
				});
			}
		}
		if (future == null) {
			return CompletableFuture.completedFuture(null);
		}
		return future;
	}
	
	private enum RemoteType {
		ASHCON(HttpAshconApi::new),
		MCHEADS(HttpMcHeadsApi::new),
		MOJANG(HttpMojangApi::new);
		
		final Function<HttpClient, RemoteNameHistoryApi> creator;
		
		private RemoteType(Function<HttpClient, RemoteNameHistoryApi> creator) {
			this.creator = creator;
		}
		
		static RemoteType fromInstance(RemoteNameHistoryApi remote) {
			if (remote instanceof HttpAshconApi) {
				return ASHCON;
			}
			if (remote instanceof HttpMcHeadsApi) {
				return MCHEADS;
			}
			if (remote instanceof HttpMojangApi) {
				return MOJANG;
			}
			throw new IllegalArgumentException("Unknown implementing instance of " + remote);
		}
	}

	public static class SerialiserImpl implements ValueSerialiser<RemoteApiBundle> {

		@Override
		public Class<RemoteApiBundle> getTargetClass() {
			return RemoteApiBundle.class;
		}

		@Override
		public RemoteApiBundle deserialise(FlexibleType flexibleType) throws BadValueException {
			HttpClient httpClient = HttpClient.newHttpClient();
			List<RemoteNameHistoryApi> remotes = flexibleType.getList((flexibleElement) -> {
				RemoteType remoteType = flexibleElement.getObject(RemoteType.class);
				return remoteType.creator.apply(httpClient);
			});
			return new RemoteApiBundle(remotes);
		}

		@Override
		public Object serialise(RemoteApiBundle value, Decomposer decomposer) {
			List<String> result = new ArrayList<>();
			for (RemoteNameHistoryApi remote : value.remotes) {
				result.add(RemoteType.fromInstance(remote).name());
			}
			return result;
		}
		
	}
	
}
