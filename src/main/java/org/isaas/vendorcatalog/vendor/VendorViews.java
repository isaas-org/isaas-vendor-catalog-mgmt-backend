package org.isaas.vendorcatalog.vendor;

import java.time.Instant;
import java.util.UUID;

/** Response bodies for the vendor membership endpoints. */
public final class VendorViews {

	private VendorViews() {
	}

	public record VendorView(UUID id, String companyName, String slug, String websiteUrl, String supportEmail,
			VendorStatus status, VerificationStatus verificationStatus, Instant verifiedAt) {

		public static VendorView from(Vendor vendor) {
			return new VendorView(vendor.getId(), vendor.getCompanyName(), vendor.getSlug(), vendor.getWebsiteUrl(),
					vendor.getSupportEmail(), vendor.getStatus(), vendor.getVerificationStatus(),
					vendor.getVerifiedAt());
		}

	}

	/** What an unaffiliated user sees when looking for a vendor to join. */
	public record VendorSummary(UUID id, String companyName, String slug) {

		public static VendorSummary from(Vendor vendor) {
			return new VendorSummary(vendor.getId(), vendor.getCompanyName(), vendor.getSlug());
		}

	}

	/** signedIn is false for an invited member who hasn't logged in yet. */
	public record MemberView(UUID id, String name, String email, VendorUserRole role, VendorUserStatus status,
			boolean signedIn, Instant createdAt) {

		public static MemberView from(VendorUser user) {
			return new MemberView(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus(),
					user.getAuthProviderId() != null, user.getCreatedAt());
		}

	}

	public record JoinRequestView(UUID id, UUID vendorId, String name, String email, VendorUserRole requestedRole,
			JoinRequestStatus status, JoinGrantPath grantedVia, UUID reviewedByUserId, Instant reviewedAt,
			String reviewNotes, Instant createdAt) {

		public static JoinRequestView from(VendorJoinRequest request) {
			return new JoinRequestView(request.getId(), request.getVendorId(), request.getName(), request.getEmail(),
					request.getRequestedRole(), request.getStatus(), request.getGrantedVia(),
					request.getReviewedByUserId(), request.getReviewedAt(), request.getReviewNotes(),
					request.getCreatedAt());
		}

	}

	public record DocumentView(UUID id, ArtifactType artifactType, String fileUrl, ArtifactStatus status,
			UUID submittedByUserId, UUID reviewedByStaffId, Instant reviewedAt, String reviewNotes,
			Instant createdAt) {

		public static DocumentView from(VerificationArtifact artifact) {
			return new DocumentView(artifact.getId(), artifact.getArtifactType(), artifact.getFileUrl(),
					artifact.getStatus(), artifact.getSubmittedByUserId(), artifact.getReviewedByStaffId(),
					artifact.getReviewedAt(), artifact.getReviewNotes(), artifact.getCreatedAt());
		}

	}

}
