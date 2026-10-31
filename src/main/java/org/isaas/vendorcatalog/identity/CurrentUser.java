package org.isaas.vendorcatalog.identity;

import org.isaas.vendorcatalog.api.ApiException;
import org.isaas.vendorcatalog.staff.StaffRole;
import org.isaas.vendorcatalog.staff.StaffUser;
import org.isaas.vendorcatalog.vendor.VendorUser;
import org.springframework.http.HttpStatus;

/**
 * The caller of a request: their Auth0 identity, their vendor membership (if
 * any) and their staff role (if any). Membership and roles come only from the
 * database, never from token claims. The vendor a request acts on is always
 * the member's own vendor, never one named in the request.
 */
public final class CurrentUser {

	/** How the request was authenticated. */
	public enum Via {
		auth0, access_token
	}

	private final String subject;

	private final String email;

	private final boolean emailVerified;

	private final Via via;

	private final VendorUser member;

	private final StaffUser staff;

	CurrentUser(String subject, String email, boolean emailVerified, Via via, VendorUser member, StaffUser staff) {
		this.subject = subject;
		this.email = email;
		this.emailVerified = emailVerified;
		this.via = via;
		this.member = member;
		this.staff = staff;
	}

	public String subject() {
		return subject;
	}

	public String email() {
		return email;
	}

	public boolean emailVerified() {
		return emailVerified;
	}

	public Via via() {
		return via;
	}

	/** The vendor membership, active or not, or null for an unaffiliated user. */
	public VendorUser member() {
		return member;
	}

	public StaffUser staff() {
		return staff;
	}

	/** An active member of some vendor. */
	public VendorUser requireMember() {
		if (member == null) {
			throw ApiException.forbidden("NOT_A_MEMBER", "You are not a member of a vendor yet.");
		}
		if (!member.isActive()) {
			throw ApiException.forbidden("ACCOUNT_DISABLED", "Your vendor membership is disabled.");
		}
		return member;
	}

	/** An active admin of their vendor. */
	public VendorUser requireAdmin() {
		VendorUser user = requireMember();
		if (!user.isActiveAdmin()) {
			throw ApiException.forbidden("ADMIN_REQUIRED", "Only a vendor admin can do this.");
		}
		return user;
	}

	/** A person signed in through Auth0, not an agent using an access token. */
	public void requireLogin() {
		if (via != Via.auth0) {
			throw ApiException.forbidden("LOGIN_REQUIRED", "This needs a signed-in user, not an access token.");
		}
	}

	/** The verified email of a signed-in user, needed to create or join a vendor. */
	public String requireVerifiedEmail() {
		requireLogin();
		if (email == null || !emailVerified) {
			throw ApiException.forbidden("EMAIL_NOT_VERIFIED", "Verify your email address first.");
		}
		return email;
	}

	/** An active staff member, signed in through Auth0. */
	public StaffUser requireStaff() {
		requireLogin();
		if (staff == null || !staff.isActive()) {
			throw ApiException.forbidden("STAFF_REQUIRED", "Only marketplace staff can do this.");
		}
		return staff;
	}

	public StaffUser requireStaffAdmin() {
		StaffUser user = requireStaff();
		if (user.getRole() != StaffRole.admin) {
			throw new ApiException(HttpStatus.FORBIDDEN, "STAFF_ADMIN_REQUIRED", "Only a staff admin can do this.");
		}
		return user;
	}

}
