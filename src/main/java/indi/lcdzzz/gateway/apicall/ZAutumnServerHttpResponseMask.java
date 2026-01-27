package indi.lcdzzz.gateway.apicall;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.AbstractServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 响应伪装，用于将响应封装为HttpResponse
 */
public class ZAutumnServerHttpResponseMask extends AbstractServerHttpResponse {
    private Flux<DataBuffer> body;
    private Function<Flux<DataBuffer>, Mono<Void>> writeHandler;

    public ZAutumnServerHttpResponseMask() {
        this(DefaultDataBufferFactory.sharedInstance);
    }

    public ZAutumnServerHttpResponseMask(DataBufferFactory dataBufferFactory) {
        super(dataBufferFactory);
        this.body = Flux.error(new IllegalStateException("No content was written nor was setComplete() called on this response."));
        this.writeHandler = (body) -> {
            Sinks.Empty<Void> completion = Sinks.unsafe().empty();
            this.body = body.cache();
            this.body.subscribe((aVoid) -> {
            }, completion::tryEmitError, completion::tryEmitEmpty);
            return completion.asMono();
        };
    }

    public void setWriteHandler(Function<Flux<DataBuffer>, Mono<Void>> writeHandler) {
        Assert.notNull(writeHandler, "'writeHandler' is required");
        this.body = Flux.error(new IllegalStateException("Not available with custom write handler."));
        this.writeHandler = writeHandler;
    }

    public <T> T getNativeResponse() {
        return (T) this;
    }

    protected void applyStatusCode() {
    }

    protected void applyHeaders() {
    }

    protected void applyCookies() {
        for (List<ResponseCookie> cookies : this.getCookies().values()) {
            for (ResponseCookie cookie : cookies) {
                this.getHeaders().add("Set-Cookie", cookie.toString());
            }
        }

    }

    protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
        return this.writeHandler.apply(Flux.from(body));
    }

    protected Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> body) {
        return this.writeHandler.apply(Flux.from(body).concatMap(Flux::from));
    }

    public Mono<Void> setComplete() {
        return this.doCommit(() -> Mono.defer(() -> (Mono) this.writeHandler.apply(Flux.empty())));
    }

    public Flux<DataBuffer> getBody() {
        return this.body;
    }

    public Mono<String> getBodyAsString() {
        Charset charset = Optional.ofNullable(this.getHeaders().getContentType()).map(MimeType::getCharset).orElse(StandardCharsets.UTF_8);
        return DataBufferUtils.join(this.getBody()).map((buffer) -> {
            String s = buffer.toString(charset);
            DataBufferUtils.release(buffer);
            return s;
        }).defaultIfEmpty("");
    }
}