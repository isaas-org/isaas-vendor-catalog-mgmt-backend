package org.isaas.vendorcatalog.security;

import java.time.Clock;
import java.time.Instant;

import org.isaas.vendorcatalog.accesstoken.VendorAccessToken;
import org.isaas.vendorcatalog.accesstoken.VendorAccessTokenRepository;
import org.isaas.vendorcatalog.vendor.VendorUserRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authenticates {@code isaas_vpat_} bearer tokens. A token works only while it
 * is unrevoked, unexpired and its owner is an active member, so disabling a
 * user stops their agents on the next request.
 */
@Component
public class AccessTokenAuthenticationProvider implements AuthenticationProvider {

	private final VendorAccessTokenRepository tokens;

	private final VendorUserRepository users;

	private final Clock clock;

	public AccessTokenAuthenticationProvider(VendorAccessTokenRepository tokens, VendorUserRepository users,
			Clock clock) {
		this.tokens = tokens;
		this.users = users;
		this.clock = clock;
	}

	@Override
	@Transactional
	public Authentication authenticate(Authentication authentication) {
		String token = ((BearerTokenAuthenticationToken) authentication).getToken();
		Instant now = clock.instant();
		VendorAccessToken stored = tokens.findByTokenHash(AccessTokens.hash(token))
			.filter((candidate) -> candidate.isUsableAt(now))
			.orElseThrow(() -> new InvalidBearerTokenException("Access token is unknown, revoked or expired"));
		boolean ownerActive = users.findById(stored.getVendorUserId()).map((user) -> user.isActive()).orElse(false);
		if (!ownerActive) {
			throw new InvalidBearerTokenException("Access token owner is disabled");
		}
		tokens.touch(stored.getId());
		return new AccessTokenAuthentication(stored.getVendorUserId(), stored.getId());
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
	}

}
