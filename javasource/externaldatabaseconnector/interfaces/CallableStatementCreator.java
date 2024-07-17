package externaldatabaseconnector.interfaces;

import externaldatabaseconnector.impl.DatabaseConnectorException;
import externaldatabaseconnector.impl.callablestatement.StatementWrapper;

import java.sql.Connection;
import java.sql.SQLException;

public interface CallableStatementCreator {
    StatementWrapper create(String sql,String queryParameters, Connection connection) throws SQLException, DatabaseConnectorException;
}
