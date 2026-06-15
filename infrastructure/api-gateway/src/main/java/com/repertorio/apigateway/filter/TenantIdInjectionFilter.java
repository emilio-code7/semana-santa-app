package com.repertorio.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class TenantIdInjectionFilter implements GlobalFilter, Ordered {

    private static final String HERMANDADES_PREFIX = "/api/hermandades/";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith(HERMANDADES_PREFIX)) {
            String remaining = path.substring(HERMANDADES_PREFIX.length());
            int slashIndex = remaining.indexOf('/');
            if (slashIndex > 0) {
                String hermandadId = remaining.substring(0, slashIndex);
                log.debug("Injecting X-Tenant-Id: {}", hermandadId);

                HttpHeaders mutableHeaders = new HttpHeaders();
                mutableHeaders.putAll(exchange.getRequest().getHeaders());
                mutableHeaders.set("X-Tenant-Id", hermandadId);

                ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                    @Override
                    public HttpHeaders getHeaders() {
                        return HttpHeaders.readOnlyHttpHeaders(mutableHeaders);
                    }
                };

                return chain.filter(exchange.mutate().request(decoratedRequest).build());
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
