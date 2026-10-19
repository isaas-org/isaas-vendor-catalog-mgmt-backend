package org.isaas.vendorcatalog.vendor;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A vendor company. {@code status} is the operational gate and can only be
 * 'active' while {@code verificationStatus} is 'verified' (CHECK
 * vendors_active_requires_verified). verified_at is stamped by a trigger.
 */
@Entity
@Table(schema = "vendors", name = "vendors")
public class Vendor {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@Column(name = "company_name", nullable = false)
	private String companyName;

	@Column(nullable = false)
	private String slug;

	@Column(name = "website_url")
	private String websiteUrl;

	@Column(name = "support_email")
	private String supportEmail;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(nullable = false)
	private VendorStatus status = VendorStatus.pending;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "verification_status", nullable = false)
	private VerificationStatus verificationStatus = VerificationStatus.pending;

	@Column(name = "verified_by_staff_id")
	private UUID verifiedByStaffId;

	@Generated(event = { EventType.INSERT, EventType.UPDATE })
	@Column(name = "verified_at", insertable = false, updatable = false)
	private Instant verifiedAt;

	@Generated
	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	protected Vendor() {
	}

	public Vendor(String companyName, String slug, String websiteUrl, String supportEmail) {
		this.companyName = companyName;
		this.slug = slug;
		this.websiteUrl = websiteUrl;
		this.supportEmail = supportEmail;
	}

	public UUID getId() {
		return id;
	}

	public String getCompanyName() {
		return companyName;
	}

	public String getSlug() {
		return slug;
	}

	public String getWebsiteUrl() {
		return websiteUrl;
	}

	public String getSupportEmail() {
		return supportEmail;
	}

	public VendorStatus getStatus() {
		return status;
	}

	public void setStatus(VendorStatus status) {
		this.status = status;
	}

	public VerificationStatus getVerificationStatus() {
		return verificationStatus;
	}

	/** Records a staff decision; the trigger keeps verified_at in step. */
	public void decideVerification(VerificationStatus decision, UUID staffId) {
		this.verificationStatus = decision;
		this.verifiedByStaffId = decision == VerificationStatus.verified ? staffId : null;
	}

	public UUID getVerifiedByStaffId() {
		return verifiedByStaffId;
	}

	public Instant getVerifiedAt() {
		return verifiedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
