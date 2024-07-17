package externaldatabaseconnector.impl;

import externaldatabaseconnector.interfaces.ConnectionManager;

public final class ConnectionManagerSingleton {
	private static ConnectionManager connectionManager;

	public static synchronized ConnectionManager getInstance() {
		if (connectionManager == null)
			connectionManager = new JdbcConnectionManager();

		return connectionManager;
	}
}
