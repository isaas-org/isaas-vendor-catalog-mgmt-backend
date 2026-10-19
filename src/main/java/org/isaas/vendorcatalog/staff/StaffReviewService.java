package org.isaas.vendorcatalog.staff;

import java.util.List;
import java.util.UUID;

import org.isaas.vendorcatalog.api.ApiException;
import org.isaas.vendorcatalog.identity.CurrentUser;
import org.isaas.vendorcatalog.vendor.ArtifactStatus;
import org.isaas.vendorcatalog.vendor.Vendor;
import org.isaas.vendorcatalog.vendor.VendorRepository;
import org.isaas.vendorcatalog.vendor.VendorStatus;
import org.isaas.vendorcatalog.vendor.VendorViews.DocumentView;
import org.isaas.vendorcatalog.vendor.VendorViews.VendorView;
import org.isaas.vendorcatalog.vendor.VerificationArtifact;
import org.isaas.vendorcatalog.vendor.VerificationArtifactRepository;
import org.isaas.vendorcatalog.vendor.VerificationStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Staff review of vendor KYB: check each document, decide verification, then
 * activate. Verification doesn't activate a vendor by itself, and the
 * database refuses an active vendor that isn't verified.
 */
@Service
public class StaffReviewService {

	private final VendorRepository vendors;

	private final VerificationArtifactRepository documents;

	public StaffReviewService(VendorRepository vendors, VerificationArtifactRepository documents) {
		this.vendors = vendors;
		this.documents = documents;
	}

	@Transactional(readOnly = true)
	public List<VendorView> vendorsByVerification(CurrentUser currentUser, VerificationStatus status) {
		currentUser.requireStaff();
		return vendors.findByVerificationStatusOrderByCreatedAt(status).stream().map(VendorView::from).toList();
	}

	@Transactional(readOnly = true)
	public VendorReview vendor(CurrentUser currentUser, UUID vendorId) {
		currentUser.requireStaff();
		Vendor vendor = findVendor(vendorId);
		List<DocumentView> vendorDocuments = documents.findByVendorIdOrderByCreatedAt(vendorId)
			.stream()
			.map(DocumentView::from)
			.toList();
		return new VendorReview(VendorView.from(vendor), vendorDocuments);
	}

	@Transactional
	public DocumentView reviewDocument(CurrentUser currentUser, UUID documentId, ArtifactStatus decision,
			String notes) {
		StaffUser staff = currentUser.requireStaff();
		if (decision == ArtifactStatus.pending) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_DECISION", "Decide approved or rejected.");
		}
		VerificationArtifact document = documents.findById(documentId)
			.orElseThrow(() -> ApiException.notFound("Document not found."));
		document.review(decision, staff.getId(), notes);
		return DocumentView.from(documents.saveAndFlush(document));
	}

	@Transactional
	public VendorView decideVerification(CurrentUser currentUser, UUID vendorId, VerificationStatus decision) {
		StaffUser staff = currentUser.requireStaff();
		Vendor vendor = findVendor(vendorId);
		vendor.decideVerification(decision, staff.getId());
		return VendorView.from(vendors.saveAndFlush(vendor));
	}

	@Transactional
	public VendorView setStatus(CurrentUser currentUser, UUID vendorId, VendorStatus status) {
		currentUser.requireStaff();
		Vendor vendor = findVendor(vendorId);
		vendor.setStatus(status);
		return VendorView.from(vendors.saveAndFlush(vendor));
	}

	private Vendor findVendor(UUID vendorId) {
		return vendors.findById(vendorId).orElseThrow(() -> ApiException.notFound("Vendor not found."));
	}

	public record VendorReview(VendorView vendor, List<DocumentView> documents) {
	}

}
