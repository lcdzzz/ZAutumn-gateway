package indi.lcdzzz.gateway.apicall;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.*;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

/**
 * 请求伪装，用于将任意请求封装为HttpRequest
 */
public class ZAutumnServerHttpRequestMask extends AbstractServerHttpRequest {
    private final String httpMethod;
    private final MultiValueMap<String, HttpCookie> cookies;
    @Nullable
    private final InetSocketAddress localAddress;
    @Nullable
    private final InetSocketAddress remoteAddress;
    @Nullable
    private final SslInfo sslInfo;
    private final Flux<DataBuffer> body;

    private ZAutumnServerHttpRequestMask(String httpMethod, URI uri, @Nullable String contextPath, HttpHeaders headers, MultiValueMap<String, HttpCookie> cookies, @Nullable InetSocketAddress localAddress, @Nullable InetSocketAddress remoteAddress, @Nullable SslInfo sslInfo, Publisher<? extends DataBuffer> body) {
        super(uri, contextPath, headers);
        Assert.isTrue(StringUtils.hasText(httpMethod), "HTTP method is required.");
        this.httpMethod = httpMethod;
        this.cookies = cookies;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.sslInfo = sslInfo;
        this.body = Flux.from(body);
    }

    public static BaseBuilder<?> get(String urlTemplate, Object... uriVars) {
        return method(HttpMethod.GET, urlTemplate, uriVars);
    }

    public static BaseBuilder<?> head(String urlTemplate, Object... uriVars) {
        return method(HttpMethod.HEAD, urlTemplate, uriVars);
    }

    public static BodyBuilder post(String urlTemplate, Object... uriVars) {
        return method(HttpMethod.POST, urlTemplate, uriVars);
    }

    public static BodyBuilder put(String urlTemplate, Object... uriVars) {
        return method(HttpMethod.PUT, urlTemplate, uriVars);
    }

    public static BodyBuilder patch(String urlTemplate, Object... uriVars) {
        return method(HttpMethod.PATCH, urlTemplate, uriVars);
    }

    public static BaseBuilder<?> delete(String urlTemplate, Object... uriVars) {
        return method(HttpMethod.DELETE, urlTemplate, uriVars);
    }

    public static BaseBuilder<?> options(String urlTemplate, Object... uriVars) {
        return method(HttpMethod.OPTIONS, urlTemplate, uriVars);
    }

    public static BodyBuilder method(HttpMethod method, URI url) {
        Assert.notNull(method, "HTTP method is required. For a custom HTTP method, please provide a String HTTP method value.");
        return new DefaultBodyBuilder(method.name(), url);
    }

    public static BodyBuilder method(HttpMethod method, String uri, Object... vars) {
        return method(method, toUri(uri, vars));
    }

    public static BodyBuilder method(String httpMethod, String uri, Object... vars) {
        return new DefaultBodyBuilder(httpMethod, toUri(uri, vars));
    }

    @Nullable
    public HttpMethod getMethod() {
        return HttpMethod.resolve(this.httpMethod);
    }

    public String getMethodValue() {
        return this.httpMethod;
    }

    @Nullable
    public InetSocketAddress getLocalAddress() {
        return this.localAddress;
    }

    @Nullable
    public InetSocketAddress getRemoteAddress() {
        return this.remoteAddress;
    }

    @Nullable
    protected SslInfo initSslInfo() {
        return this.sslInfo;
    }

    public Flux<DataBuffer> getBody() {
        return this.body;
    }

    protected MultiValueMap<String, HttpCookie> initCookies() {
        return this.cookies;
    }

    public <T> T getNativeRequest() {
        return (T) this;
    }

    private static URI toUri(String uri, Object[] vars) {
        return UriComponentsBuilder.fromUriString(uri).buildAndExpand(vars).encode().toUri();
    }

    public interface BaseBuilder<B extends BaseBuilder<B>> {
        B contextPath(String contextPath);

        B queryParam(String name, Object... values);

        B queryParams(MultiValueMap<String, String> params);

        B remoteAddress(InetSocketAddress remoteAddress);

        B localAddress(InetSocketAddress localAddress);

        void sslInfo(SslInfo sslInfo);

        B cookie(HttpCookie... cookie);

        B cookies(MultiValueMap<String, HttpCookie> cookies);

        B header(String headerName, String... headerValues);

        B headers(MultiValueMap<String, String> headers);

        B accept(MediaType... acceptableMediaTypes);

        B acceptCharset(Charset... acceptableCharsets);

        B acceptLanguageAsLocales(Locale... acceptableLocales);

        B ifModifiedSince(long ifModifiedSince);

        B ifUnmodifiedSince(long ifUnmodifiedSince);

        B ifNoneMatch(String... ifNoneMatches);

        B range(HttpRange... ranges);

        ZAutumnServerHttpRequestMask build();
    }

    public interface BodyBuilder extends BaseBuilder<BodyBuilder> {
        BodyBuilder contentLength(long contentLength);

        BodyBuilder contentType(MediaType contentType);

        ZAutumnServerHttpRequestMask body(Publisher<? extends DataBuffer> body);

        ZAutumnServerHttpRequestMask body(String body);
    }

    private static class DefaultBodyBuilder implements BodyBuilder {
        private final String methodValue;
        private final URI url;
        private final UriComponentsBuilder queryParamsBuilder = UriComponentsBuilder.newInstance();
        private final HttpHeaders headers = new HttpHeaders();
        private final MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap();
        @Nullable
        private String contextPath;
        @Nullable
        private InetSocketAddress remoteAddress;
        @Nullable
        private InetSocketAddress localAddress;
        @Nullable
        private SslInfo sslInfo;

        DefaultBodyBuilder(String method, URI url) {
            this.methodValue = method;
            this.url = url;
        }

        public BodyBuilder contextPath(String contextPath) {
            this.contextPath = contextPath;
            return this;
        }

        public BodyBuilder queryParam(String name, Object... values) {
            this.queryParamsBuilder.queryParam(name, values);
            return this;
        }

        public BodyBuilder queryParams(MultiValueMap<String, String> params) {
            this.queryParamsBuilder.queryParams(params);
            return this;
        }

        public BodyBuilder remoteAddress(InetSocketAddress remoteAddress) {
            this.remoteAddress = remoteAddress;
            return this;
        }

        public BodyBuilder localAddress(InetSocketAddress localAddress) {
            this.localAddress = localAddress;
            return this;
        }

        public void sslInfo(SslInfo sslInfo) {
            this.sslInfo = sslInfo;
        }

        public BodyBuilder cookie(HttpCookie... cookies) {
            Arrays.stream(cookies).forEach((cookie) -> this.cookies.add(cookie.getName(), cookie));
            return this;
        }

        public BodyBuilder cookies(MultiValueMap<String, HttpCookie> cookies) {
            this.cookies.putAll(cookies);
            return this;
        }

        public BodyBuilder header(String headerName, String... headerValues) {
            for (String headerValue : headerValues) {
                this.headers.add(headerName, headerValue);
            }

            return this;
        }

        public BodyBuilder headers(MultiValueMap<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public BodyBuilder accept(MediaType... acceptableMediaTypes) {
            this.headers.setAccept(Arrays.asList(acceptableMediaTypes));
            return this;
        }

        public BodyBuilder acceptCharset(Charset... acceptableCharsets) {
            this.headers.setAcceptCharset(Arrays.asList(acceptableCharsets));
            return this;
        }

        public BodyBuilder acceptLanguageAsLocales(Locale... acceptableLocales) {
            this.headers.setAcceptLanguageAsLocales(Arrays.asList(acceptableLocales));
            return this;
        }

        public BodyBuilder contentLength(long contentLength) {
            this.headers.setContentLength(contentLength);
            return this;
        }

        public BodyBuilder contentType(MediaType contentType) {
            this.headers.setContentType(contentType);
            return this;
        }

        public BodyBuilder ifModifiedSince(long ifModifiedSince) {
            this.headers.setIfModifiedSince(ifModifiedSince);
            return this;
        }

        public BodyBuilder ifUnmodifiedSince(long ifUnmodifiedSince) {
            this.headers.setIfUnmodifiedSince(ifUnmodifiedSince);
            return this;
        }

        public BodyBuilder ifNoneMatch(String... ifNoneMatches) {
            this.headers.setIfNoneMatch(Arrays.asList(ifNoneMatches));
            return this;
        }

        public BodyBuilder range(HttpRange... ranges) {
            this.headers.setRange(Arrays.asList(ranges));
            return this;
        }

        public ZAutumnServerHttpRequestMask build() {
            return this.body(Flux.empty());
        }

        public ZAutumnServerHttpRequestMask body(String body) {
            byte[] bytes = body.getBytes(this.getCharset());
            DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
            return this.body(Flux.just(buffer));
        }

        public ZAutumnServerHttpRequestMask body(Publisher<? extends DataBuffer> body) {
            this.applyCookiesIfNecessary();
            return new ZAutumnServerHttpRequestMask(this.methodValue, this.getUrlToUse(), this.contextPath, this.headers, this.cookies, this.localAddress, this.remoteAddress, this.sslInfo, body);
        }

        private Charset getCharset() {
            return Optional.ofNullable(this.headers.getContentType()).map(MimeType::getCharset).orElse(StandardCharsets.UTF_8);
        }

        private void applyCookiesIfNecessary() {
            if (this.headers.get("Cookie") == null) {
                this.cookies.values().stream().flatMap(Collection::stream).forEach((cookie) -> this.headers.add("Cookie", cookie.toString()));
            }

        }

        private URI getUrlToUse() {
            MultiValueMap<String, String> params = this.queryParamsBuilder.buildAndExpand(new Object[0]).encode().getQueryParams();
            return !params.isEmpty() ? UriComponentsBuilder.fromUri(this.url).queryParams(params).build(true).toUri() : this.url;
        }
    }
}