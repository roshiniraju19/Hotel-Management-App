package externaldatabaseconnector.interfaces;

import externaldatabaseconnector.pojo.ConnectionDetail;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionManager {
	Connection getConnection(ConnectionDetail connectionDetailsObject) throws SQLException;
}
