package externaldatabaseconnector.utils;

import com.google.gson.Gson;
import java.lang.reflect.Type;

public class JsonUtil {

    private static final Gson gsonObject = new Gson();

    public static <T> T fromJson(String jsonString, Type resultType) {
        return gsonObject.fromJson(jsonString, resultType);
    }
}
