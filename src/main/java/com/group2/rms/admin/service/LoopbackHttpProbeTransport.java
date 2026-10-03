package com.group2.rms.admin.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;

/** HTTP transport for the fixed loopback health route. Never follows redirects. */
@Component
public class LoopbackHttpProbeTransport implements HttpProbeTransport {

    @Override
    public int get(URI uri, String sessionId) throws IOException, InterruptedException {
        if (!"http".equals(uri.getScheme()) || !"127.0.0.1".equals(uri.getHost())
                || uri.getPath() == null
                || !uri.getPath().endsWith("/admin/api-monitoring/internal/health")) {
            throw new IllegalArgumentException("Monitoring target is not allowlisted");
        }
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection(Proxy.NO_PROXY);
        connection.setRequestMethod("GET");
        connection.setUseCaches(false);
        connection.setConnectTimeout(2_000);
        connection.setReadTimeout(5_000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Cookie", "JSESSIONID=" + sessionId);
        try {
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }
}
