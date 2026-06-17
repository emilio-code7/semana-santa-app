package com.repertorio.apigateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.GET, "/api/hermandades").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/hermandades/{hermandadId}").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/hermandades/{hermandadId}/procesiones").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/hermandades/{hermandadId}/procesiones/{procesionId}").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/hermandades/{hermandadId}/procesiones/{procesionId}/current-marcha").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/marchas/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/hermandades/{hermandadId}/marchas/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/procesiones/live").permitAll()
                        .pathMatchers(HttpMethod.GET, "/actuator/**").permitAll()
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .build();
    }
}
