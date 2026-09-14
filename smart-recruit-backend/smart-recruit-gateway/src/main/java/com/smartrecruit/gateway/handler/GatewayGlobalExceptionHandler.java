package com.smartrecruit.gateway.handler;

import com.alibaba.fastjson2.JSON;
import com.smartrecruit.common.dto.ApiResponse;
import io.netty.channel.ConnectTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * 网关全局异常处理器。
 *
 * <p>统一处理网关层和后端服务的异常。所有响应均返回 HTTP 200，
 * 通过响应体中的业务错误码区分异常类型。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Slf4j
@Order(-1)
@Component
public class GatewayGlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.setRawStatusCode(HttpStatus.OK.value());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<Void> result;

        if (ex instanceof ResponseStatusException rse) {
            result = ApiResponse.error(50002, rse.getReason());
        } else if (ex instanceof ConnectTimeoutException || ex instanceof TimeoutException) {
            result = ApiResponse.error(50002, "后端服务响应超时，请稍后重试");
        } else if (ex instanceof ConnectException) {
            result = ApiResponse.error(50003, "后端服务不可用，请检查服务状态");
        } else {
            log.error("网关异常: ", ex);
            result = ApiResponse.error(50001, "服务暂时不可用，请稍后重试");
        }

        byte[] responseBytes = JSON.toJSONBytes(result);
        DataBuffer buffer = response.bufferFactory().wrap(responseBytes);
        return response.writeWith(Mono.just(buffer));
    }
}
