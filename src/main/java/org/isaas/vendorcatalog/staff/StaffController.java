package org.isaas.vendorcatalog.staff;

import java.util.List;
import java.util.UUID;

import org.isaas.vendorcatalog.identity.CurrentUser;
import org.isaas.vendorcatalog.staff.StaffReviewService.VendorReview;
import org.isaas.vendorcatalog.vendor.ArtifactStatus;
import org.isaas.vendorcatalog.vendor.VendorStatus;
import org.isaas.vendorcatalog.vendor.VendorViews.DocumentView;
import org.isaas.vendorcatalog.vendor.VendorViews.VendorView;
import org.isaas.vendorcatalog.vendor.VerificationStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Staff endpoints. Used from an API client for now; there is no staff UI yet. */
@RestController
@RequestMapping("/staff")
public class StaffController {

	private final StaffReviewService reviews;

	public StaffController(StaffReviewService reviews) {
		this.reviews = reviews;
	}

	@GetMapping("/vendors")
	public List<VendorView> vendors(CurrentUser currentUser,
			@RequestParam(defaultValue = "pending") VerificationStatus verificationStatus) {
		return reviews.vendorsByVerification(currentUser, verificationStatus);
	}

	@GetMapping("/vendors/{vendorId}")
	public VendorReview vendor(CurrentUser currentUser, @PathVariable UUID vendorId) {
		return reviews.vendor(currentUser, vendorId);
	}

	@PostMapping("/verification-documents/{documentId}/review")
	public DocumentView reviewDocument(CurrentUser currentUser, @PathVariable UUID documentId,
			@Valid @RequestBody DocumentReviewRequest body) {
		return reviews.reviewDocument(currentUser, documentId, body.decision(), body.notes());
	}

	@PostMapping("/vendors/{vendorId}/verification")
	public VendorView decideVerification(CurrentUser currentUser, @PathVariable UUID vendorId,
			@Valid @RequestBody VerificationDecisionRequest body) {
		return reviews.decideVerification(currentUser, vendorId, body.decision());
	}

	@PostMapping("/vendors/{vendorId}/status")
	public VendorView setStatus(CurrentUser currentUser, @PathVariable UUID vendorId,
			@Valid @RequestBody StatusRequest body) {
		return reviews.setStatus(currentUser, vendorId, body.status());
	}

	public record DocumentReviewRequest(@NotNull ArtifactStatus decision, @Size(max = 2000) String notes) {
	}

	public record VerificationDecisionRequest(@NotNull VerificationStatus decision) {
	}

	public record StatusRequest(@NotNull VendorStatus status) {
	}

}
