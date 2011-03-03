package com.nirima.json;

import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.processors.JsonValueProcessor;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: magnayn
 * Date: 30/12/2010
 * Time: 19:34
 * To change this template use File | Settings | File Templates.
 */
public class JsonMapProcessor implements JsonValueProcessor {
    public Object processArrayValue(Object value, JsonConfig jsonConfig) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object processObjectValue(String key, Object value, JsonConfig jsonConfig) {
        JSONObject formData = new JSONObject();

        formData.putAll((Map)value, jsonConfig);
        return formData;
    }
}
