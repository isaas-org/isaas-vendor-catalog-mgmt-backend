package org.isaas.vendorcatalog.vendor;

import java.util.List;
import java.util.UUID;

import org.isaas.vendorcatalog.api.ApiException;
import org.isaas.vendorcatalog.identity.CurrentUser;
import org.isaas.vendorcatalog.vendor.VendorViews.JoinRequestView;
import org.isaas.vendorcatalog.vendor.VendorViews.VendorSummary;
import org.isaas.vendorcatalog.vendor.VendorViews.VendorView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The two ways in for a signed-in person who isn't a member yet: create a new
 * vendor and become its first admin, or ask to join an existing one.
 */
@Service
public class VendorOnboardingService {

	private final VendorRepository vendors;

	private final VendorUserRepository users;

	private final VendorJoinRequestRepository joinRequests;

	public VendorOnboardingService(VendorRepository vendors, VendorUserRepository users,
			VendorJoinRequestRepository joinRequests) {
		this.vendors = vendors;
		this.users = users;
		this.joinRequests = joinRequests;
	}

	/**
	 * The new vendor starts pending and unverified. Its admin can draft
	 * listings and invite colleagues; publishing waits for staff to verify it.
	 */
	@Transactional
	public VendorView createVendor(CurrentUser currentUser, String companyName, String slug, String websiteUrl,
			String supportEmail, String adminName) {
		String email = currentUser.requireVerifiedEmail();
		requireUnaffiliated(currentUser);
		Vendor vendor = vendors.saveAndFlush(new Vendor(companyName, slug, websiteUrl, supportEmail));
		users.saveAndFlush(new VendorUser(vendor.getId(), adminName, email, VendorUserRole.admin,
				currentUser.subject()));
		return VendorView.from(vendor);
	}

	@Transactional(readOnly = true)
	public List<VendorSummary> searchJoinable(String nameFragment) {
		return vendors.findTop20ByStatusAndCompanyNameContainingIgnoreCaseOrderByCompanyName(VendorStatus.active,
				nameFragment.strip())
			.stream()
			.map(VendorSummary::from)
			.toList();
	}

	/** The database rejects a request to a vendor that isn't active, and a second open request. */
	@Transactional
	public JoinRequestView requestToJoin(CurrentUser currentUser, UUID vendorId, String name,
			VendorUserRole requestedRole) {
		String email = currentUser.requireVerifiedEmail();
		requireUnaffiliated(currentUser);
		if (!vendors.existsById(vendorId)) {
			throw ApiException.notFound("Vendor not found.");
		}
		VendorJoinRequest request = joinRequests.saveAndFlush(
				VendorJoinRequest.peerRequest(vendorId, currentUser.subject(), name, email, requestedRole));
		return JoinRequestView.from(request);
	}

	@Transactional(readOnly = true)
	public List<JoinRequestView> myJoinRequests(CurrentUser currentUser) {
		currentUser.requireLogin();
		return joinRequests.findByAuthProviderIdOrderByCreatedAtDesc(currentUser.subject())
			.stream()
			.map(JoinRequestView::from)
			.toList();
	}

	/** One email, one vendor: vendor_users.email is unique across vendors. */
	private static void requireUnaffiliated(CurrentUser currentUser) {
		if (currentUser.member() != null) {
			throw ApiException.conflict("ALREADY_A_MEMBER", "You already belong to a vendor.");
		}
	}

}
