package externaldatabaseconnector.impl;

import externaldatabaseconnector.impl.callablestatement.StatementWrapper;
import externaldatabaseconnector.interfaces.CallableStatementCreator;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public class CallableStatementCreatorImpl implements CallableStatementCreator {

    @Override
    public StatementWrapper create(String sql,String queryParameters, Connection connection) throws SQLException, DatabaseConnectorException {
        final CallableStatement cStatement = connection.prepareCall(sql);
        return new StatementWrapper(cStatement);
    }
}