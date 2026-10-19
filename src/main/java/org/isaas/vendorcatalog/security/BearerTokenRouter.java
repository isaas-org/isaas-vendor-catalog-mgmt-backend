package org.isaas.vendorcatalog.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Sends {@code Authorization: Bearer isaas_...} to the access token check and
 * every other bearer token to Auth0 JWT validation. Both end at the same
 * current-user lookup, so authorization code isn't duplicated.
 */
public class BearerTokenRouter implements AuthenticationManagerResolver<HttpServletRequest> {

	private static final String BEARER = "Bearer ";

	private final AuthenticationManager jwtManager;

	private final AuthenticationManager accessTokenManager;

	public BearerTokenRouter(AuthenticationManager jwtManager, AuthenticationManager accessTokenManager) {
		this.jwtManager = jwtManager;
		this.accessTokenManager = accessTokenManager;
	}

	@Override
	public AuthenticationManager resolve(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		boolean accessToken = header != null && header.regionMatches(true, 0, BEARER, 0, BEARER.length())
				&& AccessTokens.looksLikeAccessToken(header.substring(BEARER.length()).trim());
		return accessToken ? accessTokenManager : jwtManager;
	}

}
