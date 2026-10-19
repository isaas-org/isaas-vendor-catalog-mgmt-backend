package org.isaas.vendorcatalog.accesstoken;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A personal access token an agent app uses to act as one vendor user. Only
 * the SHA-256 hash of the token is stored; the database limits the lifetime
 * to 90 days (vendor_access_tokens_max_lifetime).
 */
@Entity
@Table(schema = "vendors", name = "vendor_access_tokens")
public class VendorAccessToken {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(name = "vendor_id", nullable = false)
	private UUID vendorId;

	@Column(name = "vendor_user_id", nullable = false)
	private UUID vendorUserId;

	@Column(nullable = false)
	private String name;

	@Column(name = "token_prefix", nullable = false)
	private String tokenPrefix;

	@Column(name = "token_hash", nullable = false)
	private byte[] tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "last_used_at", insertable = false, updatable = false)
	private Instant lastUsedAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected VendorAccessToken() {
	}

	public VendorAccessToken(UUID vendorId, UUID vendorUserId, String name, String tokenPrefix, byte[] tokenHash,
			Instant createdAt, Instant expiresAt) {
		this.vendorId = vendorId;
		this.vendorUserId = vendorUserId;
		this.name = name;
		this.tokenPrefix = tokenPrefix;
		this.tokenHash = tokenHash;
		this.createdAt = createdAt;
		this.expiresAt = expiresAt;
	}

	public boolean isUsableAt(Instant now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

	public void revoke(Instant now) {
		if (revokedAt == null) {
			revokedAt = now;
		}
	}

	public UUID getId() {
		return id;
	}

	public UUID getVendorId() {
		return vendorId;
	}

	public UUID getVendorUserId() {
		return vendorUserId;
	}

	public String getName() {
		return name;
	}

	public String getTokenPrefix() {
		return tokenPrefix;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getLastUsedAt() {
		return lastUsedAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
