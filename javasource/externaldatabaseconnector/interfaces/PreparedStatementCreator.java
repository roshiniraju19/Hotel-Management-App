package externaldatabaseconnector.interfaces;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface PreparedStatementCreator {
	PreparedStatement create(String query, Connection connection) throws SQLException;
	PreparedStatement create(String sql,String queryParameters, Connection connection) throws SQLException, IllegalArgumentException;


}
