package org.isaas.vendorcatalog.vendor;

import java.util.List;
import java.util.UUID;

import org.isaas.vendorcatalog.api.ApiException;
import org.isaas.vendorcatalog.identity.CurrentUser;
import org.isaas.vendorcatalog.vendor.VendorViews.DocumentView;
import org.isaas.vendorcatalog.vendor.VendorViews.JoinRequestView;
import org.isaas.vendorcatalog.vendor.VendorViews.MemberView;
import org.isaas.vendorcatalog.vendor.VendorViews.VendorView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Everything a member does within their own vendor. The vendor is always the
 * caller's own (from their vendor_users row); a request never names it.
 */
@Service
public class MembershipService {

	private final VendorRepository vendors;

	private final VendorUserRepository users;

	private final VendorJoinRequestRepository joinRequests;

	private final VerificationArtifactRepository documents;

	public MembershipService(VendorRepository vendors, VendorUserRepository users,
			VendorJoinRequestRepository joinRequests, VerificationArtifactRepository documents) {
		this.vendors = vendors;
		this.users = users;
		this.joinRequests = joinRequests;
		this.documents = documents;
	}

	@Transactional(readOnly = true)
	public VendorView myVendor(CurrentUser currentUser) {
		return VendorView.from(vendors.findById(currentUser.requireMember().getVendorId()).orElseThrow());
	}

	@Transactional(readOnly = true)
	public List<MemberView> members(CurrentUser currentUser) {
		return users.findByVendorIdOrderByCreatedAt(currentUser.requireMember().getVendorId())
			.stream()
			.map(MemberView::from)
			.toList();
	}

	/**
	 * Adds a member by email. Works while the vendor is pending, so the person
	 * holding the verification documents can be brought in. They get access
	 * when they first log in with that email, verified.
	 */
	@Transactional
	public MemberView invite(CurrentUser currentUser, String name, String email, VendorUserRole role) {
		VendorUser admin = currentUser.requireAdmin();
		VendorUser invited = users.saveAndFlush(new VendorUser(admin.getVendorId(), name, email, role, null));
		joinRequests.saveAndFlush(VendorJoinRequest.invite(admin.getVendorId(), name, email, role, admin.getId()));
		return MemberView.from(invited);
	}

	/** Changes a member's role or status. A vendor always keeps at least one active admin. */
	@Transactional
	public MemberView updateMember(CurrentUser currentUser, UUID memberId, VendorUserRole role,
			VendorUserStatus status) {
		VendorUser admin = currentUser.requireAdmin();
		VendorUser member = users.findByIdAndVendorId(memberId, admin.getVendorId())
			.orElseThrow(() -> ApiException.notFound("Member not found."));
		boolean losesAdmin = member.isActiveAdmin() && ((role != null && role != VendorUserRole.admin)
				|| (status != null && status != VendorUserStatus.active));
		if (losesAdmin && users.countByVendorIdAndRoleAndStatus(admin.getVendorId(), VendorUserRole.admin,
				VendorUserStatus.active) <= 1) {
			throw ApiException.conflict("LAST_ADMIN", "A vendor must keep at least one active admin.");
		}
		if (role != null) {
			member.setRole(role);
		}
		if (status != null) {
			member.setStatus(status);
		}
		return MemberView.from(users.saveAndFlush(member));
	}

	@Transactional(readOnly = true)
	public List<JoinRequestView> joinRequests(CurrentUser currentUser, JoinRequestStatus status) {
		return joinRequests.findByVendorIdAndStatusOrderByCreatedAt(currentUser.requireAdmin().getVendorId(), status)
			.stream()
			.map(JoinRequestView::from)
			.toList();
	}

	/** Approval creates the member row; role defaults to the requested one. */
	@Transactional
	public MemberView approve(CurrentUser currentUser, UUID requestId, VendorUserRole role) {
		VendorUser admin = currentUser.requireAdmin();
		VendorJoinRequest request = pendingRequest(admin, requestId);
		VendorUserRole granted = role != null ? role : request.getRequestedRole();
		VendorUser member = users.saveAndFlush(new VendorUser(admin.getVendorId(), request.getName(),
				request.getEmail(), granted, request.getAuthProviderId()));
		request.decide(JoinRequestStatus.approved, admin.getId(), null);
		joinRequests.saveAndFlush(request);
		return MemberView.from(member);
	}

	@Transactional
	public JoinRequestView reject(CurrentUser currentUser, UUID requestId, String notes) {
		VendorUser admin = currentUser.requireAdmin();
		VendorJoinRequest request = pendingRequest(admin, requestId);
		request.decide(JoinRequestStatus.rejected, admin.getId(), notes);
		return JoinRequestView.from(joinRequests.saveAndFlush(request));
	}

	@Transactional(readOnly = true)
	public List<DocumentView> documents(CurrentUser currentUser) {
		return documents.findByVendorIdOrderByCreatedAt(currentUser.requireMember().getVendorId())
			.stream()
			.map(DocumentView::from)
			.toList();
	}

	@Transactional
	public DocumentView submitDocument(CurrentUser currentUser, ArtifactType type, String fileUrl) {
		VendorUser admin = currentUser.requireAdmin();
		VerificationArtifact document = documents
			.saveAndFlush(new VerificationArtifact(admin.getVendorId(), type, fileUrl, admin.getId()));
		return DocumentView.from(document);
	}

	private VendorJoinRequest pendingRequest(VendorUser admin, UUID requestId) {
		VendorJoinRequest request = joinRequests.findByIdAndVendorId(requestId, admin.getVendorId())
			.orElseThrow(() -> ApiException.notFound("Join request not found."));
		if (request.getStatus() != JoinRequestStatus.pending) {
			throw ApiException.conflict("NOT_PENDING", "This join request has already been decided.");
		}
		return request;
	}

}
