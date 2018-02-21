package com.vatcore.graphdb.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vatcore.graphdb.Driver;
import com.vatcore.graphdb.component.Document;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;

public class HttpUtil {

    public static <T> String post(String url, T data) throws IOException {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                new ObjectMapper().writeValueAsString(data)
        );
        Request request = new Request.Builder()
                .url(Driver.url + url + "?db=" + Driver.db)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

}
