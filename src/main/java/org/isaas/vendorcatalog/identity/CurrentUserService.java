package org.isaas.vendorcatalog.identity;

import org.isaas.vendorcatalog.security.AccessTokenAuthentication;
import org.isaas.vendorcatalog.security.Auth0Claims;
import org.isaas.vendorcatalog.staff.StaffUser;
import org.isaas.vendorcatalog.staff.StaffUserRepository;
import org.isaas.vendorcatalog.vendor.VendorUser;
import org.isaas.vendorcatalog.vendor.VendorUserRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the caller from the authenticated request.
 *
 * For an Auth0 login: find the vendor_users and staff_users rows bound to the
 * token's subject. If there is none and the token says the email is verified,
 * bind the subject to an invited or seeded row with that email first. A row
 * is never created here, so signing up with an email grants nothing unless an
 * admin, a seed or an approved join request already put that email on a row.
 *
 * SSO auto-provisioning is not implemented yet: no enterprise connection
 * exists, and the access token doesn't carry the connection.
 */
@Service
public class CurrentUserService {

	private final VendorUserRepository vendorUsers;

	private final StaffUserRepository staffUsers;

	public CurrentUserService(VendorUserRepository vendorUsers, StaffUserRepository staffUsers) {
		this.vendorUsers = vendorUsers;
		this.staffUsers = staffUsers;
	}

	@Transactional
	public CurrentUser resolve(Authentication authentication) {
		if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
			return fromJwt(jwtAuthentication.getToken());
		}
		if (authentication instanceof AccessTokenAuthentication accessToken) {
			return fromAccessToken(accessToken);
		}
		throw new AuthenticationCredentialsNotFoundException("No supported authentication on the request");
	}

	private CurrentUser fromJwt(Jwt jwt) {
		String subject = jwt.getSubject();
		String email = jwt.getClaimAsString(Auth0Claims.EMAIL);
		boolean emailVerified = Boolean.TRUE.equals(jwt.getClaimAsBoolean(Auth0Claims.EMAIL_VERIFIED));
		boolean canBind = emailVerified && email != null;

		VendorUser member = vendorUsers.findByAuthProviderId(subject).orElse(null);
		if (member == null && canBind && vendorUsers.bindAuthProviderId(subject, email) > 0) {
			member = vendorUsers.findByAuthProviderId(subject).orElse(null);
		}
		StaffUser staff = staffUsers.findByAuthProviderId(subject).orElse(null);
		if (staff == null && canBind && staffUsers.bindAuthProviderId(subject, email) > 0) {
			staff = staffUsers.findByAuthProviderId(subject).orElse(null);
		}
		return new CurrentUser(subject, email, emailVerified, CurrentUser.Via.auth0, member, staff);
	}

	private CurrentUser fromAccessToken(AccessTokenAuthentication accessToken) {
		VendorUser member = vendorUsers.findById(accessToken.getVendorUserId())
			.orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Access token owner no longer exists"));
		// An agent acts only as the vendor member; staff rights never travel with a token.
		return new CurrentUser(member.getAuthProviderId(), member.getEmail(), true, CurrentUser.Via.access_token,
				member, null);
	}

}
