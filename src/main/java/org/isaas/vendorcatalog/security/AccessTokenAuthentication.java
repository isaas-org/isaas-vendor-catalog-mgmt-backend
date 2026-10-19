package org.isaas.vendorcatalog.security;

import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.AbstractAuthenticationToken;

/** A request authenticated with a personal access token instead of an Auth0 login. */
public class AccessTokenAuthentication extends AbstractAuthenticationToken {

	private final UUID vendorUserId;

	private final UUID tokenId;

	public AccessTokenAuthentication(UUID vendorUserId, UUID tokenId) {
		super(List.of());
		this.vendorUserId = vendorUserId;
		this.tokenId = tokenId;
		setAuthenticated(true);
	}

	public UUID getVendorUserId() {
		return vendorUserId;
	}

	public UUID getTokenId() {
		return tokenId;
	}

	@Override
	public Object getCredentials() {
		return "";
	}

	@Override
	public Object getPrincipal() {
		return vendorUserId;
	}

}
