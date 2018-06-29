## 基于HttpClient 4.5.2
1. 执行GET请求
    ```
    CloseableHttpClient httpClient = HttpClients.custom()
                    .build();
    CloseableHttpResponse response = httpClient.execute(new HttpGet("https://www.baidu.com"));
    System.out.println(EntityUtils.toString(response.getEntity()));
    ```
2. 执行POST请求
    1. 提交form表单参数
        ```
        CloseableHttpClient httpClient = HttpClients.custom()
                .build();
        HttpPost httpPost = new HttpPost("https://www.explame.com");
        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        //表单参数
        formParams.add(new BasicNameValuePair("name1", "value1"));
        formParams.add(new BasicNameValuePair("name2", "value2"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, "utf-8");
        httpPost.setEntity(entity);
        CloseableHttpResponse response = httpClient.execute(httpPost);
        System.out.println(EntityUtils.toString(response.getEntity()));
        ```
    2. 提交payload参数
        ```
        CloseableHttpClient httpClient = HttpClients.custom()
                    .build();
        HttpPost httpPost = new HttpPost("https://www.explame.com");
        StringEntity entity = new StringEntity("{\"id\": \"1\"}");
        httpPost.setEntity(entity);
        CloseableHttpResponse response = httpClient.execute(httpPost);
        System.out.println(EntityUtils.toString(response.getEntity()));
        ```
    3. post上传文件
        ```
        CloseableHttpClient httpClient = HttpClients.custom()
                .build();
        HttpPost httpPost = new HttpPost("https://www.example.com");
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        //要上传的文件
        multipartEntityBuilder.addBinaryBody("file", new File("temp.txt"));
        httpPost.setEntity(multipartEntityBuilder.build());
        CloseableHttpResponse response = httpClient.execute(httpPost);
        System.out.println(EntityUtils.toString(response.getEntity()));
        ```
    4. post提交multipart/form-data类型参数
        ```
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
        ```
3. 设置User-Agent
    ```
        CloseableHttpClient httpClient = HttpClients.custom()
                .setUserAgent("Mozilla/5.0")
                .build();
        CloseableHttpResponse response = httpClient.execute(new HttpGet("https://www.baidu.com"));
        System.out.println(EntityUtils.toString(response.getEntity()));
    ```
4. 设置重试处理器
    当请求超时, 会自动重试，最多3次
    ```
    HttpRequestRetryHandler retryHandler = (exception, executionCount, context) -> {
        if (executionCount >= 3) {
            return false;
        }
        if (exception instanceof InterruptedIOException) {
            return true;
        }
        if (exception instanceof UnknownHostException) {
            return true;
        }
        if (exception instanceof ConnectTimeoutException) {
            return true;
        }
        if (exception instanceof SSLException) {
            return true;
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
    ```
    
5. 重定向策略
    1. HttpClient默认情况
        会对302、307的GET和HEAD请求以及所有的303状态码做重定向处理
    2. 关闭自动重定向
        ```
        CloseableHttpClient httpClient = HttpClients.custom()
                 //关闭httpclient重定向
                .disableRedirectHandling()
                .build();
        ```
    3. POST支持302状态码重定向
        ```
        CloseableHttpClient httpClient = HttpClients.custom()
            //post 302支持重定向
            .setRedirectStrategy(new LaxRedirectStrategy())
            .build();
        CloseableHttpResponse response = httpClient.execute(new HttpPost("https://www.explame.com"));
        System.out.println(EntityUtils.toString(response.getEntity()));
        ```
6. 定制cookie
    * 方式一：通过addHeader方式设置（不推荐这种方式）
        ```
            CloseableHttpClient httpClient = HttpClients.custom()
                    .build();
            HttpGet httpGet = new HttpGet("http://www.example.com");
            httpGet.addHeader("Cookie", "name=value");
            httpClient.execute(httpGet);
        ```
        由于HttpClient默认会维护cookie状态。如果这个请求response中有Set-Cookie头，那下次请求的时候httpclient默认会把这个Cookie带上。并且会新建一行header。如果再遇到
        ```httpGet.addHeader("Cookie", "name=value");```
        那么下次请求则会有两行name为Cookie的header。
    * 方式二：通过CookieStore的方式，以浏览器中的cookie为例（推荐）
        ```
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
        ```
        这种方式把定制的cookie交给httpclient维护。
7. cookie管理
    * 方式一：初始化HttpClient时，传入一个自己CookieStore对象
        ```
        CookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();
        httpClient.execute(new HttpGet("https://www.baidu.com"));
        //请求一次后,清理cookie再发起一次新的请求
        cookieStore.clear();
        httpClient.execute(new HttpGet("https://www.baidu.com"));
        ```
    * 方式二：每次执行请求的时候传入自己的HttpContext对象
        ```
        //注:HttpClientContext不是线程安全的，不要多个线程维护一个HttpClientContext
        HttpClientContext httpContext = HttpClientContext.create();
        CloseableHttpClient httpClient = HttpClients.custom()
                .build();
        httpClient.execute(new HttpGet("https://www.baidu.com"), httpContext);
        //请求一次后,清理cookie再发起一次新的请求
        httpContext.getCookieStore().clear();
        httpClient.execute(new HttpGet("https://www.baidu.com"));
        ```
8. http代理的配置
    ```
    CloseableHttpClient httpClient = HttpClients.custom()
            //设置代理
            .setRoutePlanner(new DefaultProxyRoutePlanner(new HttpHost("localhost", 8888)))
            .build();
    CloseableHttpResponse response = httpClient.execute(new HttpGet("http://www.example.com"));
    System.out.println(EntityUtils.toString(response.getEntity()));
    ```
9. SSL配置
    ```
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
    ```
10. socket代理配置
    ```
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
                        , (X509Certificate[] chain, String authType) -> true).build();
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
    ```
11. 下载文件
    ```
    CloseableHttpClient httpClient = HttpClients.custom().build();
    CloseableHttpResponse response = httpClient.execute(new HttpGet("https://www.example.com"));
    InputStream is = response.getEntity().getContent();
    Files.copy(is, new File("temp.png").toPath(), StandardCopyOption.REPLACE_EXISTING);

    ```
## 最后
* 完整Demo源码下载地址:https://github.com/wycm/HttpClientDemo