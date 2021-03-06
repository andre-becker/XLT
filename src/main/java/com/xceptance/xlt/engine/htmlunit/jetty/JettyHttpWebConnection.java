package com.xceptance.xlt.engine.htmlunit.jetty;

import java.io.IOException;
import java.net.CookieStore;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.conscrypt.OpenSSLProvider;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.ProxyConfiguration.Proxy;
import org.eclipse.jetty.client.Socks4Proxy;
import org.eclipse.jetty.client.api.Authentication;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebResponseData;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.xceptance.common.lang.ReflectionUtils;
import com.xceptance.common.util.StringMatcher;
import com.xceptance.xlt.api.util.XltException;
import com.xceptance.xlt.api.util.XltProperties;
import com.xceptance.xlt.engine.dns.XltDnsResolver;
import com.xceptance.xlt.engine.util.TimerUtils;

/**
 * An alternative {@link WebConnection} implementation that is backed by Jetty's {@link HttpClient}. In contrast to
 * Apache's {@link org.apache.http.client.HttpClient}, the Jetty client supports HTTP/2 already.
 * <p>
 * Note that the client can currently speak either HTTP/1.1 or HTTP/2, but not both. Hence two client instances are used
 * internally, one for HTTP/1.1 and another one for HTTP/2. For now the user has to configure the URL patterns for which
 * the HTTP/2 client will be used.
 */
public class JettyHttpWebConnection implements WebConnection
{
    private static final String OPEN_SSL_PROVIDER_NAME;

    static
    {
        // register the OpenSSL security provider ("Conscrypt")
        final Provider openSslProvider = new OpenSSLProvider();
        Security.addProvider(openSslProvider);

        OPEN_SSL_PROVIDER_NAME = openSslProvider.getName();
    }

    /**
     * The web client owning this web connection.
     */
    private final WebClient webClient;

    /**
     * The options of the owning web client.
     */
    private final WebClientOptions webClientOptions;

    /**
     * The URL filter that determines whether or not a certain URL is to be handled by the HTTP/2 client.
     */
    private final StringMatcher http2UrlFilter;

    /**
     * The HTTP/1.1 client.
     */
    private final HttpClient http1Client;

    /**
     * The HTTP/2 client.
     */
    private final HttpClient http2Client;

    /**
     * Constructor.
     *
     * @param webClient
     *            the owning web client
     * @param useHttp2
     *            whether to enable HTTP/2 support
     */
    public JettyHttpWebConnection(final WebClient webClient, final boolean useHttp2)
    {
        // TODO: remove when done
        // useHttp2 = false;
        // System.setProperty("javax.net.debug", "ssl");

        this.webClient = webClient;
        webClientOptions = webClient.getOptions();

        // create the URL filter
        final String includedUrls = XltProperties.getInstance().getProperty("com.xceptance.xlt.http.http2.filter.include", "");
        final String excludedUrls = XltProperties.getInstance().getProperty("com.xceptance.xlt.http.http2.filter.exclude", "");
        http2UrlFilter = new StringMatcher(includedUrls, excludedUrls);

        // create the components shared between both HTTP/1.1 and HTTP/2 clients
        final CookieStore cookieStore = new HtmlUnitBackedCookieStore(webClient.getCookieManager());
        // authentication store?
        // SSL context factory?

        // create the HTTP/1.1 client
        try
        {
            http1Client = createHttpClient(false, cookieStore);
        }
        catch (final Exception e)
        {
            throw new XltException("Failed to start Jetty HTTP/1.1 Client", e);
        }

        // create the HTTP/2 client if enabled
        if (useHttp2)
        {
            try
            {
                http2Client = createHttpClient(true, cookieStore);
            }
            catch (final Exception e)
            {
                throw new XltException("Failed to start Jetty HTTP/2 Client", e);
            }
        }
        else
        {
            // simply point to the HTTP/1.1 client
            http2Client = http1Client;
        }
    }

    /**
     * Creates a new {@link HttpClient} object and preconfigures it with settings that cannot be changed during runtime.
     * For all other settings, see {@link #reconfigureHttpClient(HttpClient)}.
     *
     * @param useHttp2
     *            whether to enable the HTTP/2 transport
     * @param cookieStore
     *            the cookie store to use
     * @return the HTTP client
     * @throws Exception
     *             if anything goes wrong
     */
    private HttpClient createHttpClient(final boolean useHttp2, final CookieStore cookieStore) throws Exception
    {
        // create http client
        final HttpClientTransport transport = useHttp2 ? new HttpClientTransportOverHTTP2(new HTTP2Client())
                                                       : new HttpClientTransportOverHTTP();

        final SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setProvider(OPEN_SSL_PROVIDER_NAME);

        final HttpClient httpClient = new HttpClient(transport, sslContextFactory);
        httpClient.setCookieStore(cookieStore);

        httpClient.setSocketAddressResolver(new XltDnsResolverAdapterForJetty(new XltDnsResolver()));

        httpClient.setTCPNoDelay(true);
        httpClient.setFollowRedirects(false);

        // TODO: do this in reconfigureHttpClient()
        reconfigureSslContextFactory(httpClient);
        reconfigureProxy(httpClient);
        reconfigureAuthentication(httpClient);

        // start http client
        httpClient.start();

        // ~~~ the following can be done after start() only ~~~

        // clear any default content decoders -> HtmlUnit will take care of decoding compressed responses
        httpClient.getContentDecoderFactories().clear();
        // TODO: apply xlt settings here
        httpClient.getContentDecoderFactories().add(NoOpContentDecoderFactory.DEFLATE);
        httpClient.getContentDecoderFactories().add(NoOpContentDecoderFactory.GZIP);

        return httpClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException
    {
        try
        {
            try
            {
                http1Client.stop();
            }
            finally
            {
                http2Client.stop();
            }
        }
        catch (final Exception e)
        {
            throw new IOException("Failed to close HTTP/1.1 and/or HTTP/2 client", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebResponse getResponse(final WebRequest webRequest) throws IOException
    {
        // determine which HTTP client to use (1.1 or 2.0)
        final HttpClient httpClient = getHttpClient(webRequest);

        // pick up and apply any changes in the Web client configuration
        try
        {
            reconfigureHttpClient(httpClient);
        }
        catch (final Throwable t)
        {
            throw new XltException("Failed to reconfigure HttpClient", t);
        }

        // create and execute the request
        try
        {
            final Request request = makeRequest(webRequest, httpClient);

            final long startTime = TimerUtils.getTime();
            final ContentResponse response = request.send();
            final long loadTime = TimerUtils.getTime() - startTime;

            return makeWebResponse(response, webRequest, loadTime);
        }
        catch (Throwable t)
        {
            // first unwrap any ExecutionException
            if (t instanceof ExecutionException)
            {
                t = t.getCause();
            }

            // now do the right thing
            if (t instanceof Error)
            {
                throw (Error) t;
            }
            else if (t instanceof RuntimeException)
            {
                throw (RuntimeException) t;
            }
            else if (t instanceof IOException)
            {
                throw (IOException) t;
            }
            else
            {
                // wrap any other exception as runtime exception
                throw new XltException("Failed to execute Web request", t);
            }
        }
    }

    /**
     * Returns the HTTP client (either 1.1 or 2) responsible for the given web request.
     */
    private HttpClient getHttpClient(final WebRequest webRequest)
    {
        final String url = webRequest.getUrl().toString();

        final boolean isHttps = StringUtils.startsWithIgnoreCase(url, "https");
        final boolean serverSpeaksHttp2 = http2UrlFilter.isAccepted(url);

        return isHttps && serverSpeaksHttp2 ? http2Client : http1Client;
    }

    private void reconfigureHttpClient(final HttpClient httpClient) throws URISyntaxException
    {
        reconfigureNetworking(httpClient);

        // TODO
        // reconfigureSslContextFactory(httpClient);
        // reconfigureProxy(httpClient);
        // reconfigureAuthentication(httpClient);
    }

    private void reconfigureNetworking(final HttpClient httpClient)
    {
        // setting properties unconditionally in the hope that the changes are cheap

        httpClient.setBindAddress(new InetSocketAddress(webClientOptions.getLocalAddress(), 0));
        httpClient.setConnectTimeout(webClientOptions.getTimeout());
        // TODO: read timeout?? maybe it can be set only at the request ...
    }

    private synchronized void reconfigureProxy(final HttpClient httpClient) throws URISyntaxException
    {
        final List<Proxy> proxies = httpClient.getProxyConfiguration().getProxies();
        final AuthenticationStore authStore = httpClient.getAuthenticationStore();

        // remove old config
        // proxies.clear();
        // authStore.clearAuthentications();

        // set new config
        final ProxyConfig proxyConfiguration = webClientOptions.getProxyConfig();
        final String proxyHost = proxyConfiguration.getProxyHost();

        if (proxyHost != null)
        {
            final int proxyPort = proxyConfiguration.getProxyPort();
            final boolean isSocksProxy = proxyConfiguration.isSocksProxy();
            final Set<String> bypassHosts = ReflectionUtils.<Map<String, Pattern>>readInstanceField(proxyConfiguration, "proxyBypassHosts_")
                                                           .keySet();

            final Proxy proxy = isSocksProxy ? new Socks4Proxy(proxyHost, proxyPort) : new HttpProxy(proxyHost, proxyPort);
            proxy.getExcludedAddresses().addAll(bypassHosts);
            proxies.add(proxy);

            // set up proxy authentication
            final Credentials creds = webClient.getCredentialsProvider().getCredentials(new AuthScope(proxyHost, proxyPort));
            if (creds != null)
            {
                final String userName = creds.getUserPrincipal().getName();
                final String password = creds.getPassword();

                final URI httpUri = new URI(String.format("http://%s:%d/", proxyHost, proxyPort));
                final Authentication proxyAuthHttp = new BasicAuthentication(httpUri, Authentication.ANY_REALM, userName, password);
                authStore.addAuthentication(proxyAuthHttp);

                final URI httpsUri = new URI(String.format("https://%s:%d/", proxyHost, proxyPort));
                final Authentication proxyAuthHttps = new BasicAuthentication(httpsUri, Authentication.ANY_REALM, userName, password);
                authStore.addAuthentication(proxyAuthHttps);
            }
        }
    }

    private synchronized void reconfigureAuthentication(final HttpClient httpClient)
    {
        final AuthenticationStore authStore = httpClient.getAuthenticationStore();

        // remove old config
        // authStore.clearAuthentications();

        // TODO. make it work for any scope
        final Credentials creds = webClient.getCredentialsProvider().getCredentials(AuthScope.ANY);
        if (creds != null)
        {
            final Authentication auth = new AnyUriAnyRealmBasicAuthentication(creds.getUserPrincipal().getName(), creds.getPassword());
            authStore.addAuthentication(auth);
        }
    }

    private void reconfigureSslContextFactory(final HttpClient httpClient)
    {
        // TODO: maybe not sufficient, at least not for some properties (e.g. trustAll). May require stop/start.

        final SslContextFactory sslContextFactory = httpClient.getSslContextFactory();

        final boolean trustAll = webClientOptions.isUseInsecureSSL();

        if (trustAll)
        {
            // insecure
            sslContextFactory.setIncludeProtocols(webClientOptions.getSSLInsecureProtocol());
        }
        else
        {
            // secure
            final String[] protocols = webClientOptions.getSSLClientProtocols();
            if (ArrayUtils.isNotEmpty(protocols))
            {
                sslContextFactory.setIncludeProtocols(protocols);
            }

            // TODO
            // webClientOptions.getSSLClientCertificatePassword();
            // webClientOptions.getSSLClientCertificateStore();
            // webClientOptions.getSSLTrustStore();
        }

        // both insecure and secure
        sslContextFactory.setTrustAll(trustAll);

        final String[] cipherSuites = webClientOptions.getSSLClientCipherSuites();
        if (ArrayUtils.isNotEmpty(cipherSuites))
        {
            sslContextFactory.setIncludeCipherSuites(cipherSuites);
        }
    }

    /**
     * Creates a Jetty {@link Request} object that corresponds to the passed HtmlUnit {@link WebRequest} object.
     *
     * @param webRequest
     *            the HtmlUnit web request
     * @param httpClient
     *            the http client to use
     * @return the corresponding Jetty request
     */
    private Request makeRequest(final WebRequest webRequest, final HttpClient httpClient) throws URISyntaxException
    {
        // build the request
        final Request request = httpClient.newRequest(webRequest.getUrl().toString());

        request.method(webRequest.getHttpMethod().name());
        request.timeout(httpClient.getConnectTimeout(), TimeUnit.MILLISECONDS);

        // set parameters/body
        final String body = webRequest.getRequestBody();
        if (body == null)
        {
            webRequest.getRequestParameters().forEach(p -> request.param(p.getName(), p.getValue()));
        }
        else
        {
            request.content(new StringContentProvider(body));
        }

        // copy headers from HU request to Jetty request
        webRequest.getAdditionalHeaders().forEach(request::header);

        // register handler to collect the final request headers
        request.onRequestCommit(r -> webRequest.setAdditionalHeaders(toMap(r.getHeaders())));

        return request;
    }

    /**
     * Creates an HtmlUnit {@link WebResponse} object that corresponds to the passed Jetty {@link ContentResponse}
     * object.
     *
     * @param response
     *            the Jetty response
     * @param webRequest
     *            the original HtmlUnit web request
     * @param loadTime
     *            the time taken to execute the request [ms]
     * @return the corresponding HtmlUnit web response
     */
    private static WebResponse makeWebResponse(final ContentResponse response, final WebRequest webRequest, final long loadTime)
    {
        final List<NameValuePair> headers = toNamevaluePairs(response.getHeaders());

        final WebResponseData webResponseData = new WebResponseData(response.getContent(), response.getStatus(), response.getReason(),
                                                                    headers);

        final WebResponse webResponse = new WebResponse(webResponseData, webRequest, loadTime);

        return webResponse;
    }

    /**
     * Converts a Jetty {@link HttpFields} object to a corresponding map.
     */
    private static Map<String, String> toMap(final HttpFields httpFields)
    {
        final Map<String, String> headers = new LinkedHashMap<>();

        httpFields.forEach(f -> headers.put(f.getName(), f.getValue()));

        return headers;
    }

    /**
     * Converts a Jetty {@link HttpFields} object to the corresponding list of name/value pairs.
     */
    private static List<NameValuePair> toNamevaluePairs(final HttpFields httpFields)
    {
        final List<NameValuePair> headers = new ArrayList<>();

        httpFields.forEach(f -> headers.add(new NameValuePair(f.getName(), f.getValue())));

        return headers;
    }
}
