package com.smartrecruit.gateway.filter;

import com.smartrecruit.common.util.DateUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 统一响应格式过滤器。
 *
 * <p>拦截后端服务的 JSON 响应，如果响应体不是标准 {@code {code, message, data, timestamp}} 格式，
 * 则自动包装为标准格式，确保前端获得一致的响应结构。
 *
 * <p>对于已经是标准格式的响应或非 JSON 响应，直接透传不做处理。
 *
 * @author xdh
 * @since 1.0.0
 */
@Slf4j
@Component
public class UnifiedResponseFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {

            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    String contentType = getDelegate().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);

                    if (contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE)) {
                        Flux<? extends DataBuffer> fluxBody = Flux.from(body);

                        return super.writeWith(fluxBody.buffer().flatMap(dataBuffers -> {
                            String responseData;
                            try {
                                int totalSize = 0;
                                for (DataBuffer dataBuffer : dataBuffers) {
                                    totalSize += dataBuffer.readableByteCount();
                                }
                                byte[] allBytes = new byte[totalSize];
                                int offset = 0;
                                for (DataBuffer dataBuffer : dataBuffers) {
                                    int size = dataBuffer.readableByteCount();
                                    dataBuffer.read(allBytes, offset, size);
                                    offset += size;
                                }
                                responseData = new String(allBytes, StandardCharsets.UTF_8);
                            } catch (Exception e) {
                                log.error("读取响应字节流异常：{}", e.getMessage(), e);
                                responseData = "";
                            }

                            dataBuffers.forEach(DataBufferUtils::release);
                            log.debug("网关转发响应: URI={}, Status={}, Response={}",
                                    exchange.getRequest().getURI(),
                                    getStatusCode(),
                                    responseData);

                            String wrappedResponse = wrapResponse(responseData);
                            byte[] uppedContent = wrappedResponse.getBytes(StandardCharsets.UTF_8);

                            getDelegate().getHeaders().setContentType(
                                    new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
                            getDelegate().getHeaders().setContentLength(uppedContent.length);

                            return Mono.just(bufferFactory.wrap(uppedContent));
                        }));
                    }
                }
                return super.writeWith(body);
            }

            @Override
            public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                return writeWith(Flux.from(body).flatMapSequential(p -> p));
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    /**
     * 包装响应数据为标准格式。
     *
     * <p>如果响应数据已包含 code 和 message 字段，则认为是标准格式，不做二次包装。
     *
     * @param responseData 原始响应数据
     * @return 包装后的响应数据
     */
    private String wrapResponse(String responseData) {
        try {
            Object json = JSON.parse(responseData);
            if (json instanceof JSONObject obj) {
                if (obj.containsKey("code") && obj.containsKey("message")) {
                    return responseData;
                }
            }
        } catch (Exception ignored) {
            // JSON 解析失败，需要包装
        }

        JSONObject result = new JSONObject();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", JSON.parse(responseData));
        result.put("timestamp", DateUtils.currentEpochMillis());

        // WriteLongAsString 防止 FastJSON2 将超大 Long（如 Snowflake ID）
        // 序列化为 JavaScript 无法精确表示的数字
        return result.toJSONString(JSONWriter.Feature.WriteLongAsString);
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
