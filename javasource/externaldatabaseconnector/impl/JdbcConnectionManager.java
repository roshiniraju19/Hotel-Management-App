package externaldatabaseconnector.impl;

import com.mendix.core.Core;
import com.mendix.logging.ILogNode;

import com.zaxxer.hikari.HikariDataSource;
import externaldatabaseconnector.interfaces.ConnectionManager;
import externaldatabaseconnector.pojo.ConnectionDetail;
import externaldatabaseconnector.proxies.constants.Constants;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Simple connection manager backed by HikariCP. It does not support shutdown at
 * this moment that may lead to memory leak in case of hot deployment.
 * <p>
 * TODO: REMOVE TECHNICAL DEBT - extract connection manager into shared
 * component, so that : 1. the same connection manager may be used by actions,
 * data storage layer (ConnectionBus), etc 2. all connections may be properly
 * closed during application shutdown
 */
public class JdbcConnectionManager implements ConnectionManager {
	private final Map<Integer, HikariDataSource> connectionPool = new ConcurrentHashMap<>();
	private final ILogNode logNode;
	private boolean hasDriversInitialized;

	public JdbcConnectionManager(final ILogNode logNode) {
		this.logNode = logNode;
	}

	public JdbcConnectionManager() {
		this(Core.getLogger(Constants.getLogNode()));
	}

	@Override
	public Connection getConnection(ConnectionDetail connectionDetailsObject)
			throws SQLException {
		initializeDrivers();

		final String jdbcUrl = connectionDetailsObject.getConnectionString();
		final String userName = connectionDetailsObject.getUserName();
		final Integer connPoolKey = toConnPoolKey(jdbcUrl, userName);
		final HikariDataSource dataSource = connectionPool.computeIfAbsent(connPoolKey, k -> {
			if (logNode.isTraceEnabled()) {
				logNode.trace(String.format("Creating data source in connection pool for [url=%s, user=%s]", jdbcUrl, userName));
			}
			try {
				return createHikariDataSource(connectionDetailsObject, connPoolKey);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		if (logNode.isTraceEnabled()) {
			logNode.trace(String.format("Getting connection from data source in connection pool for [url=%s, user=%s]",
				jdbcUrl, userName));
		}
		return dataSource.getConnection();
	}

	/**
	 * The JDBC drivers in the userlib folder of a project are not automatically
	 * correctly registered to the DriverManager. The cause is maybe the fact that
	 * the drivers are put into the project.jar on deployment. Hence, we explicitly
	 * register the drivers.
	 */
	private synchronized void initializeDrivers() {
		if (!hasDriversInitialized) {
			ServiceLoader<Driver> loader = ServiceLoader.load(Driver.class);
			List<Driver> drivers = StreamSupport.stream(loader.spliterator(), false).collect(Collectors.toList());

			for (Driver driver : drivers) {
				try {
					DriverManager.registerDriver(driver);
				} catch (SQLException e) {
					throw new RuntimeException("Failed to register JDBC driver: " + driver.getClass().getName(), e);
				}
			}

			if (logNode.isTraceEnabled()) {
				String logMessage = drivers.stream().map(a -> a.getClass().getName())
						.collect(Collectors.joining(", ", "Found JDBC Drivers: ", ""));
				logNode.trace(logMessage);
			}

			hasDriversInitialized = true;
		}
	}

	private Integer toConnPoolKey(final String jdbcUrl, final String userName) {
		return (jdbcUrl + userName).hashCode();
	}

	private HikariDataSource createHikariDataSource(final ConnectionDetail connectionDetailsObject,
			Integer connPoolKey) throws Exception {

		final HikariDataSource dataSource = new HikariDataSource();
		dataSource.setPoolName(String.format("MxDbConnector-HikaryCP-%d", connPoolKey));
		dataSource.setMinimumIdle(0);

		if(connectionDetailsObject.getDatabaseType().equals("Snowflake")){
			Properties prop = new Properties();
			prop.put("application", "Mendix_DbConnector");
			dataSource.setDataSourceProperties(prop);
			dataSource.setDataSource(MxSnowflakeDataSource.getSnowflakeDataSource(connectionDetailsObject));
			return dataSource;
		}

		dataSource.setJdbcUrl(connectionDetailsObject.getConnectionString());
		dataSource.setUsername(connectionDetailsObject.getUserName());
		dataSource.setPassword(connectionDetailsObject.getPassword());
		return dataSource;
	}
}