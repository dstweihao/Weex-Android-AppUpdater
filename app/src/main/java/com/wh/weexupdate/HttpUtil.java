package com.wh.weexupdate;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/*
 * 创建者 韦豪
 * 创建时间 2018/7/31 9:29
 */
public class HttpUtil {
    public static Response get(String url) throws IOException {
        //01. 定义okhttp
        OkHttpClient okHttpClient_get = new OkHttpClient();
        //02.请求体
        Request request = new Request.Builder()
                .get()//get请求方式
                .url(url)//网址
                .build();

        //03.执行okhttp
        Response response = okHttpClient_get.newCall(request).execute();
        return response;
    }
}
