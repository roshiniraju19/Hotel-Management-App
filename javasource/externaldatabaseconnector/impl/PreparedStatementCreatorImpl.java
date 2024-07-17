package externaldatabaseconnector.impl;

import com.google.gson.reflect.TypeToken;
import externaldatabaseconnector.interfaces.PreparedStatementCreator;
import externaldatabaseconnector.pojo.QueryParameter;
import externaldatabaseconnector.utils.JsonUtil;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class PreparedStatementCreatorImpl implements PreparedStatementCreator {
	private static final String GUID_FOR_HARDCODED_NULL_STRING = "903be4ca-ed8b-ddcb-a339-741c4088484a";
	private static final Map<String, Integer> SQL_TYPE_MAP = Map.of(
			"INTEGER", Types.BIGINT,
			"STRING", Types.VARCHAR,
			"BOOLEAN", Types.BOOLEAN,
			"DECIMAL", Types.DECIMAL,
			"DATETIME", Types.TIMESTAMP
	);

	@Override
	public PreparedStatement create(String query, Connection connection) throws SQLException {
		return connection.prepareStatement(query);
	}

	//Using JSON Object for named Query parameters
	public PreparedStatement create(String sql, String queryParametersJSON, Connection connection) throws SQLException {
		PreparedStatement preparedStatement = connection.prepareStatement(sql);

		Type queryParameterMapType = new TypeToken<Map<String, QueryParameter>>() {}.getType();
		Map<String, QueryParameter> queryParameterMap = JsonUtil.fromJson(queryParametersJSON, queryParameterMapType);

		addPreparedStatementParameters(preparedStatement, new ArrayList<>(queryParameterMap.values()));
		return preparedStatement;
	}

	private void addPreparedStatementParameters(PreparedStatement preparedStatement, List<QueryParameter> queryParametersList) throws SQLException, IllegalArgumentException {

		for (int i = 0; i < queryParametersList.size(); i++) {
			QueryParameter queryParameter = queryParametersList.get(i);
			String paramObjectValue = queryParameter.getValue();

			// if the paramObjectValue equals to null, then set the preparedStatement to null for current queryParameter
			if(paramObjectValue.equals("null")){
				preparedStatement.setNull(i + 1, SQL_TYPE_MAP.get(queryParameter.getDataType()));
				continue;
			}

			// check if the paramObjectValue equals GUID_FOR_HARDCODED_NULL_STRING, then make the value as "null"
			CheckForHardCodedNullString(paramObjectValue, queryParameter);

			addParameter(preparedStatement, i, queryParameter);
		}
	}

	private void CheckForHardCodedNullString(String paramObjectValue, QueryParameter paramObject){
		if(paramObjectValue.equals(GUID_FOR_HARDCODED_NULL_STRING))
			paramObject.setValue("null");
	}

	private void addParameter(PreparedStatement preparedStatement,
							  int i,
							  QueryParameter paramObject) throws SQLException {
		String paramObjectValue = paramObject.getValue();
		try {
			switch (paramObject.getDataType()) {
				case "INTEGER":
					preparedStatement.setLong(i + 1, Long.parseLong(paramObjectValue));
					break;
				case "STRING":
					preparedStatement.setString(i + 1, paramObjectValue);
					break;
				case "BOOLEAN":
					preparedStatement.setBoolean(i + 1, Boolean.parseBoolean(paramObjectValue));
					break;
				case "DECIMAL":
					BigDecimal bigDecimal = new BigDecimal(paramObjectValue);
					preparedStatement.setBigDecimal(i + 1, bigDecimal);
					break;
				case "DATETIME":
					long epochTime = Long.parseLong(paramObjectValue);
					java.util.Date date = new java.util.Date(epochTime);
					preparedStatement.setTimestamp(i + 1, new Timestamp(date.getTime()));
					break;
				default:
					throw new IllegalArgumentException(String.format("The parameter type '%s' is invalid, for the parameter named '%s'.", paramObject.getDataType(), paramObject.getName()));
			}
		} catch (Exception exception) {

			//If exception is caught with type of IllegalArgumentException, do not alter message throw it as it is.
			if(exception.getClass().equals(IllegalArgumentException.class)){
				throw exception;
			}

			String message = String.format("An error has occurred while handling the parameter with name: '%s', \nCause : %s",
					paramObject.getName(), exception.toString());
			throw new SQLException(message, exception);
		}
	}
}

