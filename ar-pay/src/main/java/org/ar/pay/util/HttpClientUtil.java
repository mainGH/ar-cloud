package org.ar.pay.util;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;


@Slf4j
public class HttpClientUtil {

    public static HttpResponse doPost(String url, Map<String,Object> map, String encoding){
        CloseableHttpClient httpClient = null;
        HttpPost httpPost = null;
        HttpResponse response = null;
        try{
            httpClient = HttpClients.createDefault();
            httpPost = new HttpPost(url);
            //
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            Iterator iterator = map.entrySet().iterator();
            while(iterator.hasNext()){
                Map.Entry<String,Object> elem = (Map.Entry<String, Object>) iterator.next();
                list.add(new BasicNameValuePair(elem.getKey(),String.valueOf((String)elem.getValue())));
            }
            if(list.size() > 0){
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(list,encoding);
                httpPost.setEntity(entity);
            }
             response = httpClient.execute(httpPost);

        }catch(Exception ex){
            ex.printStackTrace();
        }
        return response;
    }

    public static HttpResponse postJson(String url,String content){
        HttpClient httpClient = null;
        HttpPost postMethod = null;
        HttpResponse response = null;
        //RebackTokenEntity rebackTokenEntity = null;

        RequestConfig requestConfig = RequestConfig.custom().
                setConnectionRequestTimeout(10000).
                setConnectTimeout(10000).
                setSocketTimeout(10000).
                build();

        try {
            httpClient = HttpClients.createDefault();
            postMethod = new HttpPost(url);//传入URL地址
            postMethod.setConfig(requestConfig);
            //设置请求头 指定为json
            postMethod.addHeader("Content-type", "application/json;charset=UTF-8");
            //传入请求参数 Class为我传入的对象参数
            postMethod.setEntity(new StringEntity(content, Charset.forName("UTF-8")));
            response = httpClient.execute(postMethod);//获取响应

//            int statusCode = response.getStatusLine().getStatusCode();
//            if (statusCode != HttpStatus.SC_OK) {
//                System.out.println("HTTP请求未成功！HTTP Status Code:" + response.getStatusLine());
//            }
//            HttpEntity httpEntity = response.getEntity();
//            String reponseContent = EntityUtils.toString(httpEntity,"UTF-8");
//            EntityUtils.consume(httpEntity);//释放资源
//            //用Gson将对象转化为实体类
//            Gson gson = new Gson();
//            Class= gson.fromJson(reponseContent, Class.class);
//
//            System.out.println("响应内容：" + reponseContent);
           return   response;
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            /*关闭连接资源*/
            try {
                if (response == null){
                    response.getEntity().getContent().close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;

    }







//    public static HttpResponse postJson(String url, String content) throws Exception {
//
//        URI uri = new URIBuilder(url).setCharset(Charset.forName("UTF-8")).build();
//        CloseableHttpClient httpclient = null;
//        HttpEntity entity=null;
//        HttpResponse response = null;
//
//        HttpPost httppost = new HttpPost(uri);
//        RequestConfig requestConfig =
//                RequestConfig.custom().setSocketTimeout(30000).setConnectTimeout(30000).build();// 设置请求和传输超时时间
//        httppost.setConfig(requestConfig);
//        StringEntity stringEntity = new StringEntity(content, ContentType.APPLICATION_JSON);
//        httppost.setEntity(stringEntity);
//        try {
//
//
//                httpclient = HttpClients.createDefault();
//
//             response = httpclient.execute(httppost);
////            int statusCode = response.getStatusLine().getStatusCode();
////            if (statusCode == 200) {
//                 entity = response.getEntity();
////                if (entity != null) {
////                    return EntityUtils.toString(entity);
//////                }
////            } else {
////                throw new Exception(String.format("the response status code is %d", statusCode));
////            }
//           // return response;
//        } catch (IOException e) {
//
//            log.error(String.format("【%s】[RB_ASSET_ROUTER_SERVICE_ERROR]人保资产路由，发送请求失败。", e.getMessage()), e);
//            throw new Exception(String.format("发送请求失败:%s", e.getMessage()), e);
//        } finally {
//            try {
//                httpclient.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        return response;
//
//    }


    public static String postJson2Str(String url, String content) throws Exception {

        URI uri = new URIBuilder(url).setCharset(Charset.forName("UTF-8")).build();
        CloseableHttpClient httpclient = null;
        HttpEntity entity=null;
        HttpResponse response = null;

        HttpPost httppost = new HttpPost(uri);
        RequestConfig requestConfig =
                RequestConfig.custom().setSocketTimeout(30000).setConnectTimeout(30000).build();// 设置请求和传输超时时间
        httppost.setConfig(requestConfig);
        StringEntity stringEntity = new StringEntity(content, ContentType.APPLICATION_JSON);
        httppost.setEntity(stringEntity);
        try {


            httpclient = HttpClients.createDefault();

            response = httpclient.execute(httppost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
            entity = response.getEntity();
                if (entity != null) {
                    return EntityUtils.toString(entity);
                }
            } else{
                return null;
            }

        } catch (IOException e) {

            log.error(String.format("【%s】[RB_ASSET_ROUTER_SERVICE_ERROR]人保资产路由，发送请求失败。", e.getMessage()), e);
            throw new Exception(String.format("发送请求失败:%s", e.getMessage()), e);
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;

    }
}
