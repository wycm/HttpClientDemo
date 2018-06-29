package com.github.wycm.httpclient;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HttpClientDemo {
    public static void main(String[] args) throws Exception {
        downloadFile();
    }

    /**
     * 执行请求
     * @throws IOException
     */
    public static void executeRequest() throws IOException{
        CloseableHttpClient httpClient = HttpClients.custom()
                .build();
        CloseableHttpResponse response = httpClient.execute(new HttpGet("https://www.baidu.com"));
        System.out.println(EntityUtils.toString(response.getEntity()));
    }
    /**
     * post请求提交form data参数
     * @throws IOException
     */
    public static void submitFormParams() throws IOException{
        CloseableHttpClient httpClient = HttpClients.custom()
                .build();
        HttpPost httpPost = new HttpPost("https://www.example.com");
        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        //表单参数
        formParams.add(new BasicNameValuePair("name1", "value1"));
        formParams.add(new BasicNameValuePair("name2", "value2"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, "utf-8");
        httpPost.setEntity(entity);
        CloseableHttpResponse response = httpClient.execute(httpPost);
        System.out.println(EntityUtils.toString(response.getEntity()));
    }

    /**
     * post请求提交payload参数
     * @throws IOException
     */
    public static void submitPayloadParams() throws IOException{
        CloseableHttpClient httpClient = HttpClients.custom()
                .build();
        HttpPost httpPost = new HttpPost("https://www.example.com");
        StringEntity entity = new StringEntity("{\"id\": \"1\"}");
        httpPost.setEntity(entity);
        CloseableHttpResponse response = httpClient.execute(httpPost);
        System.out.println(EntityUtils.toString(response.getEntity()));
    }
    /**
     * post上传文件
     */
    public static void uploadFile() throws IOException {
        CloseableHttpClient httpClient = HttpClients.custom()
                .build();
        HttpPost httpPost = new HttpPost("https://www.example.com");
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        //要上传的文件
        multipartEntityBuilder.addBinaryBody("file", new File("temp.txt"));
        httpPost.setEntity(multipartEntityBuilder.build());
        CloseableHttpResponse response = httpClient.execute(httpPost);
        System.out.println(EntityUtils.toString(response.getEntity()));
    }

    /**
     * post提交多类型参数
     * 针对<form enctype="multipart/form-data">表单
     */
    public static void postMultipartParams() throws Exception{
        CloseableHttpClient httpClient = HttpClients.custom()
                .build();
        HttpPost httpPost = new HttpPost("https://www.example.com");
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        multipartEntityBuilder.addTextBody("username","wycm");
        multipartEntityBuilder.addTextBody("passowrd","123");
        //文件
        multipartEntityBuilder.addBinaryBody("file", new File("temp.txt"));
        httpPost.setEntity(multipartEntityBuilder.build());
        CloseableHttpResponse response = httpClient.execute(httpPost);
        System.out.println(EntityUtils.toString(response.getEntity()));
    }

    /**
     * 设置User-Agent
     * @throws Exception
     */
    public static void custiomUserAgent() throws Exception{
        CloseableHttpClient httpClient = HttpClients.custom()
                .setUserAgent("Mozilla/5.0")
                .build();
        CloseableHttpResponse response = httpClient.execute(new HttpGet("https://www.baidu.com"));
        System.out.println(EntityUtils.toString(response.getEntity()));
    }

    /**
     * 设置重试处理器，当请求超时, 会自动重试，最多3次
     * @throws Exception
     */
    public static void setRetryHandler() throws Exception{
        HttpRequestRetryHandler retryHandler = (exception, executionCount, context) -> {
            if (executionCount >= 3) {
                return false;
            }
            if (exception instanceof InterruptedIOException) {
                return false;
            }
            if (exception instanceof UnknownHostException) {
                return false;
            }
            if (exception instanceof ConnectTimeoutException) {
                return false;
            }
            if (exception instanceof SSLException) {
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
            if (idempotent) {
                return true;
            }
            return false;
        };
        CloseableHttpClient httpClient = HttpClients.custom()
                .setRetryHandler(retryHandler)
                .build();
        httpClient.execute(new HttpGet("https://www.baidu.com"));
    }
    /**
     * post 302状态码支持重定向
     * @throws IOException
     */
    public static void postRedirect() throws IOException{
        CloseableHttpClient httpClient = HttpClients.custom()
                //post 302支持重定向
                .setRedirectStrategy(new LaxRedirectStrategy())
                .build();
        CloseableHttpResponse response = httpClient.execute(new HttpPost("https://www.example.com"));
        System.out.println(EntityUtils.toString(response.getEntity()));
    }
    /**
     * 关闭重定向策略
     * @throws IOException
     */
    public static void closeRedirect() throws IOException{
        CloseableHttpClient httpClient = HttpClients.custom()
                //关闭httpclient重定向
                .disableRedirectHandling()
                .build();
    }

    /**
     * 定制cookie,方式一
     */
    public static void customCookie1() throws IOException {
        CloseableHttpClient httpClient = HttpClients.custom()
                .build();
        HttpGet httpGet = new HttpGet("http://www.example.com");
        httpGet.addHeader("Cookie", "name=value");
        httpClient.execute(httpGet);
    }
    /**
     * 定制cookie,方式二
     */
    public static void customCookie2() throws IOException {
        //此处直接粘贴浏览器cookie
        final String RAW_COOKIES = "name1=value1; name2=value2";
        final CookieStore cookieStore = new BasicCookieStore();
        for (String rawCookie : RAW_COOKIES.split("; ")){
            String[] s = rawCookie.split("=");
            BasicClientCookie cookie = new BasicClientCookie(s[0], s[1]);
            cookie.setDomain("baidu.com");
            cookie.setPath("/");
            cookie.setSecure(false);
            cookie.setAttribute("domain", "baidu.com");
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, +5);
            cookie.setExpiryDate(calendar.getTime());
            cookieStore.addCookie(cookie);
        }
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();
        httpClient.execute(new HttpGet("https://www.baidu.com"));
    }

    /**
     * cookie管理方式一
     * 初始化HttpClient时，传入一个自己CookieStore对象
     */
    public static void cookieManage1() throws Exception{
        CookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();
        httpClient.execute(new HttpGet("https://www.baidu.com"));
        //请求一次后,清理cookie再发起一次新的请求
        cookieStore.clear();
        httpClient.execute(new HttpGet("https://www.baidu.com"));
    }
    /**
     * cookie管理方式二
     * 每次执行请求的时候传入自己的HttpContext对象
     */
    public static void cookieManage2() throws Exception{
        //注:HttpClientContext不是线程安全的，不要多个线程维护一个HttpClientContext
        HttpClientContext httpContext = HttpClientContext.create();
        CloseableHttpClient httpClient = HttpClients.custom()
                .build();
        httpClient.execute(new HttpGet("https://www.baidu.com"), httpContext);
        //请求一次后,清理cookie再发起一次新的请求
        httpContext.getCookieStore().clear();
        httpClient.execute(new HttpGet("https://www.baidu.com"));
    }
    /**
     * 通过代理访问
     */
    public static void httpProxy() throws IOException {
        CloseableHttpClient httpClient = HttpClients.custom()
                //设置代理
                .setRoutePlanner(new DefaultProxyRoutePlanner(new HttpHost("localhost", 8888)))
                .build();
        CloseableHttpResponse response = httpClient.execute(new HttpGet("http://www.example.com"));
        System.out.println(EntityUtils.toString(response.getEntity()));
    }
    /**
     * ssl配置
     */
    public static void SSLConfig() throws Exception {
        //默认信任
        SSLContext sslContext = SSLContexts.custom()
                        .loadTrustMaterial(KeyStore.getInstance(KeyStore.getDefaultType())
                                , (X509Certificate[] chain, String authType) -> true).build();
        Registry<ConnectionSocketFactory> socketFactoryRegistry =
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", new PlainConnectionSocketFactory())
                        .register("https", new SSLConnectionSocketFactory(sslContext))
                        .build();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(new PoolingHttpClientConnectionManager(socketFactoryRegistry))
                .build();
        httpClient.execute(new HttpGet("https://www.baidu.com"));
    }

    static class SocketProxyPlainConnectionSocketFactory extends PlainConnectionSocketFactory{
        @Override
        public Socket createSocket(final HttpContext context) {
            InetSocketAddress socksAddr = (InetSocketAddress) context.getAttribute("socks.address");
            if (socksAddr != null){
                Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksAddr);
                return new Socket(proxy);
            } else {
                return new Socket();
            }
        }
    }
    static class SocketProxySSLConnectionSocketFactory extends SSLConnectionSocketFactory {
        public SocketProxySSLConnectionSocketFactory(final SSLContext sslContext) {
            super(sslContext, NoopHostnameVerifier.INSTANCE);
        }

        @Override
        public Socket createSocket(final HttpContext context) {
            InetSocketAddress socksAddr = (InetSocketAddress) context.getAttribute("socks.address");
            if (socksAddr != null){
                Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksAddr);
                return new Socket(proxy);
            } else {
                return new Socket();
            }
        }

    }
    /**
     * socket代理配置
     */
    public static void socketProxy() throws Exception {
        //默认信任
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(KeyStore.getInstance(KeyStore.getDefaultType())
                        , (chain, authType) -> true).build();
        Registry<ConnectionSocketFactory> socketFactoryRegistry =
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", new SocketProxyPlainConnectionSocketFactory())
                        .register("https", new SocketProxySSLConnectionSocketFactory(sslContext))
                        .build();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(new PoolingHttpClientConnectionManager(socketFactoryRegistry))
                .build();
        HttpClientContext httpClientContext = HttpClientContext.create();
        httpClientContext.setAttribute("socks.address", new InetSocketAddress("127.0.0.1", 1086));
        CloseableHttpResponse response = httpClient.execute(new HttpGet("https://httpbin.org/ip"), httpClientContext);
        System.out.println(EntityUtils.toString(response.getEntity()));
    }
    /**
     * 下载文件
     */
    public static void downloadFile() throws Exception{
        CloseableHttpClient httpClient = HttpClients.custom().build();
        CloseableHttpResponse response = httpClient.execute(new HttpGet("https://www.example.com"));
        InputStream is = response.getEntity().getContent();
        Files.copy(is, new File("temp.png").toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
