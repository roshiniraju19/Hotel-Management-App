package externaldatabaseconnector.pojo;

public class QueryParameter {
    private final String name;
    private final String dataType;
    private String value;

    public QueryParameter(String name, String dataType, String value) {
        this.name = name;
        this.dataType = dataType;
        this.value = value;
    }

    public String getName() {
        return name;
    }
    public String getDataType() {
        return dataType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
