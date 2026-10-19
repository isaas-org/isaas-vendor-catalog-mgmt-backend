package org.isaas.vendorcatalog.vendor;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A member of a vendor. The row is the only authority on which vendor a
 * person belongs to and with what role. auth_provider_id (the Auth0 subject)
 * is NULL until the person's first login binds it by verified email.
 */
@Entity
@Table(schema = "vendors", name = "vendor_users")
public class VendorUser {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(name = "vendor_id", nullable = false)
	private UUID vendorId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String email;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false)
	private VendorUserRole role;

	@Column(name = "auth_provider_id")
	private String authProviderId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false)
	private VendorUserStatus status = VendorUserStatus.active;

	@Generated
	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	protected VendorUser() {
	}

	public VendorUser(UUID vendorId, String name, String email, VendorUserRole role, String authProviderId) {
		this.vendorId = vendorId;
		this.name = name;
		this.email = email;
		this.role = role;
		this.authProviderId = authProviderId;
	}

	public UUID getId() {
		return id;
	}

	public UUID getVendorId() {
		return vendorId;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public VendorUserRole getRole() {
		return role;
	}

	public void setRole(VendorUserRole role) {
		this.role = role;
	}

	public String getAuthProviderId() {
		return authProviderId;
	}

	public VendorUserStatus getStatus() {
		return status;
	}

	public void setStatus(VendorUserStatus status) {
		this.status = status;
	}

	public boolean isActive() {
		return status == VendorUserStatus.active;
	}

	public boolean isActiveAdmin() {
		return isActive() && role == VendorUserRole.admin;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
