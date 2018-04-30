package org.agrsw.mavenreleaser;

import org.springframework.http.client.*;
import org.apache.http.*;
import org.springframework.http.*;
import java.net.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.auth.*;
import org.apache.http.auth.*;
import org.apache.http.protocol.*;
import org.apache.http.client.*;

public class HttpComponentsClientHttpRequestFactoryBasicAuth extends HttpComponentsClientHttpRequestFactory
{
    HttpHost host;
    
    public HttpComponentsClientHttpRequestFactoryBasicAuth(final HttpHost host) {
        this.host = host;
    }
    
    protected HttpContext createHttpContext(final HttpMethod httpMethod, final URI uri) {
        return this.createHttpContext();
    }
    
    private HttpContext createHttpContext() {
        final AuthCache authCache = (AuthCache)new BasicAuthCache();
        final BasicScheme basicAuth = new BasicScheme();
        authCache.put(this.host, (AuthScheme)basicAuth);
        final BasicHttpContext localcontext = new BasicHttpContext();
        localcontext.setAttribute("http.auth.auth-cache", (Object)authCache);
        return (HttpContext)localcontext;
    }
}
