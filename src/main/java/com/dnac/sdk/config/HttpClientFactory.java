// com/dnac/sdk/config/HttpClientFactory.java
package com.dnac.sdk.config;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

public final class HttpClientFactory {
    public static HttpClient create(DnacConfig cfg) throws Exception {
        HttpClient.Builder b = HttpClient.newBuilder().connectTimeout(cfg.connectTimeout());
        if (cfg.insecureTls()) {
            TrustManager[] trustAll = new TrustManager[] {
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] x, String a) {}
                        public void checkServerTrusted(X509Certificate[] x, String a) {}
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new SecureRandom());
            b.sslContext(sc);
            System.setProperty("jdk.internal.httpclient.disableHostnameVerification","true");
        }
        return b.build();
    }
}