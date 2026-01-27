package indi.lcdzzz.gateway.apicall;

import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;
import reactor.core.publisher.Mono;

public class ZAutumnServerWebExchange extends DefaultServerWebExchange {
    private ZAutumnServerWebExchange(ZAutumnServerHttpRequestMask request, WebSessionManager sessionManager) {
        super(request, new ZAutumnServerHttpResponseMask(), sessionManager, ServerCodecConfigurer.create(), new AcceptHeaderLocaleContextResolver());
    }

    public static ZAutumnServerWebExchange from(ZAutumnServerHttpRequestMask request) {
        return builder(request).build();
    }

    public static ZAutumnServerWebExchange from(ZAutumnServerHttpRequestMask.BaseBuilder<?> requestBuilder) {
        return builder(requestBuilder).build();
    }

    public static Builder builder(ZAutumnServerHttpRequestMask request) {
        return new Builder(request);
    }

    public static Builder builder(ZAutumnServerHttpRequestMask.BaseBuilder<?> requestBuilder) {
        return new Builder(requestBuilder.build());
    }

    public ZAutumnServerHttpResponseMask getResponse() {
        return (ZAutumnServerHttpResponseMask) super.getResponse();
    }

    public static class Builder {
        private final ZAutumnServerHttpRequestMask request;
        @Nullable
        private WebSessionManager sessionManager;

        public Builder(ZAutumnServerHttpRequestMask request) {
            this.request = request;
        }

        public Builder session(WebSession session) {
            this.sessionManager = (exchange) -> Mono.just(session);
            return this;
        }

        public Builder sessionManager(WebSessionManager sessionManager) {
            this.sessionManager = sessionManager;
            return this;
        }

        public ZAutumnServerWebExchange build() {
            return new ZAutumnServerWebExchange(this.request, this.sessionManager != null ? this.sessionManager : new DefaultWebSessionManager());
        }
    }
}
