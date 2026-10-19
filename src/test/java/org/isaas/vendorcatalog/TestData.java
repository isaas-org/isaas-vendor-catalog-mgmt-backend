package org.isaas.vendorcatalog;

import java.util.UUID;

import org.isaas.vendorcatalog.security.Auth0Claims;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Rows for integration tests, written straight to the real schema. Tests
 * don't share data: every row gets a unique suffix, so tests can run against
 * one database without cleanup.
 */
@Component
public class TestData {

	private final JdbcTemplate jdbc;

	public TestData(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public static String unique(String prefix) {
		return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
	}

	public static String email(String name) {
		return unique(name) + "@test.test";
	}

	/** An Auth0 login with a verified email. */
	public static RequestPostProcessor login(String subject, String email) {
		return jwt().jwt((token) -> token.subject(subject)
			.claim(Auth0Claims.EMAIL, email)
			.claim(Auth0Claims.EMAIL_VERIFIED, true));
	}

	public static RequestPostProcessor unverifiedLogin(String subject, String email) {
		return jwt().jwt((token) -> token.subject(subject)
			.claim(Auth0Claims.EMAIL, email)
			.claim(Auth0Claims.EMAIL_VERIFIED, false));
	}

	public UUID activeVendor() {
		return vendor("active", "verified");
	}

	public UUID pendingVendor() {
		return vendor("pending", "pending");
	}

	public UUID vendor(String status, String verificationStatus) {
		String slug = unique("vendor");
		return jdbc.queryForObject("""
				INSERT INTO vendors.vendors (company_name, slug, status, verification_status)
				VALUES (?, ?, ?::vendors.vendor_status, ?::vendors.verification_status)
				RETURNING id
				""", UUID.class, "Vendor " + slug, slug, status, verificationStatus);
	}

	public UUID member(UUID vendorId, String email, String role, String authProviderId) {
		return jdbc.queryForObject("""
				INSERT INTO vendors.vendor_users (vendor_id, name, email, role, auth_provider_id)
				VALUES (?, 'Test Member', ?, ?::vendors.vendor_user_role, ?)
				RETURNING id
				""", UUID.class, vendorId, email, role, authProviderId);
	}

	public UUID staff(String email, String authProviderId, String role) {
		return jdbc.queryForObject("""
				INSERT INTO platform.staff_users (name, email, role, auth_provider_id)
				VALUES ('Test Staff', ?, ?::platform.staff_role, ?)
				RETURNING id
				""", UUID.class, email, role, authProviderId);
	}

	public String authProviderIdOf(UUID vendorUserId) {
		return jdbc.queryForObject("SELECT auth_provider_id FROM vendors.vendor_users WHERE id = ?", String.class,
				vendorUserId);
	}

	public void disableMember(UUID vendorUserId) {
		jdbc.update("UPDATE vendors.vendor_users SET status = 'disabled' WHERE id = ?", vendorUserId);
	}

}
