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

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalDatabaseSetup implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

	private final ExtensionContext.Namespace namespace = ExtensionContext.Namespace.create(getClass());

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Hsqldb {

	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface H2 {

	}

	private boolean isHsqldbNotH2(Class<?> testClass) {
		if (testClass.getAnnotation(Hsqldb.class) != null) {
			return true;
		}
		if (testClass.getAnnotation(H2.class) != null) {
			return false;
		}
		throw new ExtensionConfigurationException("Either @Hsqldb or @H2 is required");
	}

	private String getJdbcUrl(Class<?> testClass, int counterValue) {
		return isHsqldbNotH2(testClass) ?
				"jdbc:hsqldb:mem:testdb-" + counterValue
				: "jdbc:h2:mem:testdb-" + counterValue + ";DB_CLOSE_DELAY=-1";
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		AtomicInteger globalCounter = context.getRoot().getStore(namespace)
				.getOrComputeIfAbsent(AtomicInteger.class, (k) -> new AtomicInteger(), AtomicInteger.class);

		String jdbcUrl =  getJdbcUrl(context.getRequiredTestClass(), globalCounter.incrementAndGet());
		ConnectionSource connectionSource = () -> DriverManager.getConnection(jdbcUrl, "SA", "");
		context.getStore(namespace).put(ConnectionSource.class, connectionSource);
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		// Both HSQLDB and H2 use the 'SHUTDOWN' statement
		try (Connection connection = getConnectionSource(context).openConnection();
			 PreparedStatement prepStmt = connection.prepareStatement("SHUTDOWN")) {
			prepStmt.execute();
		}
	}

	private ConnectionSource getConnectionSource(ExtensionContext context) {
		return context.getStore(namespace).get(ConnectionSource.class, ConnectionSource.class);
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		var paramType = parameterContext.getParameter().getType();
		return paramType.equals(ConnectionSource.class);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return getConnectionSource(extensionContext);
	}
}
