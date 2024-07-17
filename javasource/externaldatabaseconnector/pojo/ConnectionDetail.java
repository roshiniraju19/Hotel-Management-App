package externaldatabaseconnector.pojo;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class ConnectionDetail {
    @SerializedName("UserName")
    private final String userName;

    @SerializedName("Password")
    private final String password;

    @SerializedName("ConnectionString")
    private final String connectionString;

    @SerializedName("DatabaseType")
    private final String databaseType;

    @SerializedName("AdditionalProperties")
    private final Map<String, String> additionalProperties;

    public ConnectionDetail(String userName, String password, String connectionString, String databaseType, Map<String, String> additionalProperties) {
        this.userName = userName;
        this.password = password;
        this.connectionString = connectionString;
        this.databaseType = databaseType;
        this.additionalProperties = additionalProperties;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }
    public String getConnectionString() {
        return connectionString;
    }
    public String getDatabaseType() {
        return databaseType;
    }
    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }

}
