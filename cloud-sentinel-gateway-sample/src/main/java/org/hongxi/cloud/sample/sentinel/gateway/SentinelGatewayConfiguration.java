package org.hongxi.cloud.sample.sentinel.gateway;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import reactor.core.publisher.Mono;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

/**
 * Created by shenhongxi on 2020/7/12.
 */
@Configuration
public class SentinelGatewayConfiguration {

    @Bean
    public BlockRequestHandler blockRequestHandler() {
        return new BlockRequestHandler() {
            @Override
            public Mono<ServerResponse> handleRequest(ServerWebExchange exchange,
                    Throwable t) {
                return ServerResponse.status(444)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(fromValue("{\"code\":444,\"msg\":\"Sentinel gateway block\"}"));
            }
        };
    }
}
