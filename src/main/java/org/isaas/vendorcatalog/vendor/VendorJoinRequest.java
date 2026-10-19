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
 * How a member after the first got access. The database checks that the
 * path, reviewer and decision agree (vendor_join_requests_path_rules) and
 * that only an active vendor receives peer or SSO requests (trigger
 * vendors.enforce_join_request_gate).
 */
@Entity
@Table(schema = "vendors", name = "vendor_join_requests")
public class VendorJoinRequest {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(name = "vendor_id", nullable = false)
	private UUID vendorId;

	@Column(name = "auth_provider_id")
	private String authProviderId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String email;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "requested_role", nullable = false)
	private VendorUserRole requestedRole;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false)
	private JoinRequestStatus status = JoinRequestStatus.pending;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "granted_via", nullable = false)
	private JoinGrantPath grantedVia;

	@Column(name = "reviewed_by_user_id")
	private UUID reviewedByUserId;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@Column(name = "review_notes")
	private String reviewNotes;

	@Generated
	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	protected VendorJoinRequest() {
	}

	private VendorJoinRequest(UUID vendorId, String authProviderId, String name, String email,
			VendorUserRole requestedRole, JoinGrantPath grantedVia) {
		this.vendorId = vendorId;
		this.authProviderId = authProviderId;
		this.name = name;
		this.email = email;
		this.requestedRole = requestedRole;
		this.grantedVia = grantedVia;
	}

	public static VendorJoinRequest peerRequest(UUID vendorId, String authProviderId, String name, String email,
			VendorUserRole requestedRole) {
		return new VendorJoinRequest(vendorId, authProviderId, name, email, requestedRole, JoinGrantPath.peer_approval);
	}

	/** An invite is approved at once and names the inviting admin. */
	public static VendorJoinRequest invite(UUID vendorId, String name, String email, VendorUserRole role,
			UUID invitedByUserId) {
		VendorJoinRequest request = new VendorJoinRequest(vendorId, null, name, email, role, JoinGrantPath.admin_invite);
		request.decide(JoinRequestStatus.approved, invitedByUserId, null);
		return request;
	}

	public void decide(JoinRequestStatus decision, UUID reviewerId, String notes) {
		this.status = decision;
		this.reviewedByUserId = reviewerId;
		this.reviewedAt = Instant.now();
		this.reviewNotes = notes;
	}

	public UUID getId() {
		return id;
	}

	public UUID getVendorId() {
		return vendorId;
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

	public VendorUserRole getRequestedRole() {
		return requestedRole;
	}

	public JoinRequestStatus getStatus() {
		return status;
	}

	public JoinGrantPath getGrantedVia() {
		return grantedVia;
	}

	public UUID getReviewedByUserId() {
		return reviewedByUserId;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}

	public String getReviewNotes() {
		return reviewNotes;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
