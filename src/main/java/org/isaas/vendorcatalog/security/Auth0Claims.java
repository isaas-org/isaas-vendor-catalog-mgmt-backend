package org.isaas.vendorcatalog.security;

/**
 * Custom claims added to access tokens by the "Add iSaaS claims" post-login
 * Action (isaas-auth0-config). Access tokens don't carry email by default.
 */
public final class Auth0Claims {

	public static final String EMAIL = "https://isaas/email";

	public static final String EMAIL_VERIFIED = "https://isaas/email_verified";

	private Auth0Claims() {
	}

}
