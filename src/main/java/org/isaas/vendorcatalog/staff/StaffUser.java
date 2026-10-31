package org.isaas.vendorcatalog.staff;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Marketplace staff. Read-only here: staff are added by isaas-database's
 * seed or bootstrap script; this backend only binds their Auth0 identity.
 */
@Entity
@Table(schema = "platform", name = "staff_users")
public class StaffUser {

	@Id
	private UUID id;

	@Column(name = "auth_provider_id")
	private String authProviderId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String email;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false)
	private StaffRole role;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false)
	private StaffStatus status;

	protected StaffUser() {
	}

	public UUID getId() {
		return id;
	}

	public String getAuthProviderId() {
		return authProviderId;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public StaffRole getRole() {
		return role;
	}

	public boolean isActive() {
		return status == StaffStatus.active;
	}

}
