package org.isaas.vendorcatalog.security;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Format of vendor personal access tokens: {@code isaas_vpat_} followed by 43
 * base62 characters (32 random bytes). The prefix tells them apart from Auth0
 * JWTs on the same Authorization header and makes a leaked token easy to spot.
 */
public final class AccessTokens {

	public static final String PREFIX = "isaas_vpat_";

	/** Characters kept in token_prefix so a user can tell tokens apart. */
	public static final int DISPLAY_PREFIX_LENGTH = PREFIX.length() + 4;

	private static final int RANDOM_BYTES = 32;

	private static final int ENCODED_LENGTH = 43;

	private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

	private static final SecureRandom RANDOM = new SecureRandom();

	private AccessTokens() {
	}

	public static boolean looksLikeAccessToken(String bearerToken) {
		return bearerToken != null && bearerToken.startsWith("isaas_");
	}

	public static String generate() {
		byte[] bytes = new byte[RANDOM_BYTES];
		RANDOM.nextBytes(bytes);
		return PREFIX + base62(bytes);
	}

	/**
	 * SHA-256 of the whole token. 32 random bytes can't be guessed, so a fast
	 * hash is enough and lets the token be looked up by its hash directly.
	 */
	public static byte[] hash(String token) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is always available", ex);
		}
	}

	private static String base62(byte[] bytes) {
		BigInteger value = new BigInteger(1, bytes);
		BigInteger base = BigInteger.valueOf(ALPHABET.length());
		StringBuilder encoded = new StringBuilder();
		while (value.signum() > 0) {
			BigInteger[] quotientAndRemainder = value.divideAndRemainder(base);
			encoded.append(ALPHABET.charAt(quotientAndRemainder[1].intValue()));
			value = quotientAndRemainder[0];
		}
		while (encoded.length() < ENCODED_LENGTH) {
			encoded.append(ALPHABET.charAt(0));
		}
		return encoded.reverse().toString();
	}

}
