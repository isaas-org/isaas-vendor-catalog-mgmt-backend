package org.isaas.vendorcatalog.accesstoken;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.isaas.vendorcatalog.api.ApiException;
import org.isaas.vendorcatalog.identity.CurrentUser;
import org.isaas.vendorcatalog.security.AccessTokens;
import org.isaas.vendorcatalog.vendor.VendorUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Personal access tokens for agent apps. Only a signed-in member manages
 * tokens: a token can't create, list or revoke tokens.
 */
@Service
public class AccessTokenService {

	private final VendorAccessTokenRepository tokens;

	private final Clock clock;

	public AccessTokenService(VendorAccessTokenRepository tokens, Clock clock) {
		this.tokens = tokens;
		this.clock = clock;
	}

	/** The token value is in the response only here; afterwards only its hash exists. */
	@Transactional
	public CreatedAccessToken create(CurrentUser currentUser, String name, int expiresInDays) {
		currentUser.requireLogin();
		VendorUser member = currentUser.requireMember();
		String token = AccessTokens.generate();
		// Postgres keeps microseconds; truncate so the 90-day CHECK sees the same instants.
		Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
		VendorAccessToken stored = tokens.saveAndFlush(new VendorAccessToken(member.getVendorId(), member.getId(),
				name, token.substring(0, AccessTokens.DISPLAY_PREFIX_LENGTH), AccessTokens.hash(token), now,
				now.plus(Duration.ofDays(expiresInDays))));
		return new CreatedAccessToken(stored.getId(), stored.getName(), token, stored.getTokenPrefix(),
				stored.getCreatedAt(), stored.getExpiresAt());
	}

	@Transactional(readOnly = true)
	public List<AccessTokenView> list(CurrentUser currentUser) {
		currentUser.requireLogin();
		return tokens.findByVendorUserIdOrderByCreatedAtDesc(currentUser.requireMember().getId())
			.stream()
			.map(AccessTokenView::from)
			.toList();
	}

	@Transactional
	public void revoke(CurrentUser currentUser, UUID tokenId) {
		currentUser.requireLogin();
		VendorAccessToken token = tokens.findByIdAndVendorUserId(tokenId, currentUser.requireMember().getId())
			.orElseThrow(() -> ApiException.notFound("Access token not found."));
		token.revoke(clock.instant());
		tokens.saveAndFlush(token);
	}

	public record CreatedAccessToken(UUID id, String name, String token, String tokenPrefix, Instant createdAt,
			Instant expiresAt) {
	}

	public record AccessTokenView(UUID id, String name, String tokenPrefix, Instant createdAt, Instant expiresAt,
			Instant lastUsedAt, Instant revokedAt) {

		static AccessTokenView from(VendorAccessToken token) {
			return new AccessTokenView(token.getId(), token.getName(), token.getTokenPrefix(), token.getCreatedAt(),
					token.getExpiresAt(), token.getLastUsedAt(), token.getRevokedAt());
		}

	}

}
