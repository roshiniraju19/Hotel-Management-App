package externaldatabaseconnector.impl;

public class DatabaseConnectorException extends Exception {
	public DatabaseConnectorException(String msg) {
		super(msg, null);
	}
	
	public DatabaseConnectorException(String msg, Exception e) {
		super(msg, e);
	}
}
