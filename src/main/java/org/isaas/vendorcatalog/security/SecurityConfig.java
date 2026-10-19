package org.isaas.vendorcatalog.security;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Every endpoint except health and info needs a bearer token: an Auth0 access
 * token for the vendor API (issuer and audience come from
 * spring.security.oauth2.resourceserver.jwt.*) or a vendor personal access
 * token. Stateless, no cookies, so no CSRF protection is needed.
 *
 * What a caller may do is decided per endpoint from their vendor_users row
 * (see CurrentUser), not from token claims.
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder,
			AccessTokenAuthenticationProvider accessTokenProvider) throws Exception {
		BearerTokenRouter router = new BearerTokenRouter(new ProviderManager(new JwtAuthenticationProvider(jwtDecoder)),
				new ProviderManager(accessTokenProvider));
		http.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests((requests) -> requests
				.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
				.anyRequest().authenticated())
			.oauth2ResourceServer((resourceServer) -> resourceServer.authenticationManagerResolver(router));
		return http.build();
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
