package org.isaas.vendorcatalog.vendor;

import java.util.List;
import java.util.UUID;

import org.isaas.vendorcatalog.identity.CurrentUser;
import org.isaas.vendorcatalog.vendor.VendorViews.DocumentView;
import org.isaas.vendorcatalog.vendor.VendorViews.JoinRequestView;
import org.isaas.vendorcatalog.vendor.VendorViews.MemberView;
import org.isaas.vendorcatalog.vendor.VendorViews.VendorView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The caller's own vendor: /vendor, singular, because it is never chosen by the request. */
@RestController
@RequestMapping("/vendor")
public class MembershipController {

	private final MembershipService membership;

	public MembershipController(MembershipService membership) {
		this.membership = membership;
	}

	@GetMapping
	public VendorView myVendor(CurrentUser currentUser) {
		return membership.myVendor(currentUser);
	}

	@GetMapping("/members")
	public List<MemberView> members(CurrentUser currentUser) {
		return membership.members(currentUser);
	}

	@PatchMapping("/members/{memberId}")
	public MemberView updateMember(CurrentUser currentUser, @PathVariable UUID memberId,
			@RequestBody UpdateMemberRequest body) {
		return membership.updateMember(currentUser, memberId, body.role(), body.status());
	}

	@PostMapping("/invitations")
	@ResponseStatus(HttpStatus.CREATED)
	public MemberView invite(CurrentUser currentUser, @Valid @RequestBody InviteRequest body) {
		return membership.invite(currentUser, body.name(), body.email(), body.role());
	}

	@GetMapping("/join-requests")
	public List<JoinRequestView> joinRequests(CurrentUser currentUser,
			@RequestParam(defaultValue = "pending") JoinRequestStatus status) {
		return membership.joinRequests(currentUser, status);
	}

	@PostMapping("/join-requests/{requestId}/approve")
	public MemberView approve(CurrentUser currentUser, @PathVariable UUID requestId,
			@RequestBody(required = false) ApproveRequest body) {
		return membership.approve(currentUser, requestId, body == null ? null : body.role());
	}

	@PostMapping("/join-requests/{requestId}/reject")
	public JoinRequestView reject(CurrentUser currentUser, @PathVariable UUID requestId,
			@RequestBody(required = false) RejectRequest body) {
		return membership.reject(currentUser, requestId, body == null ? null : body.notes());
	}

	@GetMapping("/verification-documents")
	public List<DocumentView> documents(CurrentUser currentUser) {
		return membership.documents(currentUser);
	}

	@PostMapping("/verification-documents")
	@ResponseStatus(HttpStatus.CREATED)
	public DocumentView submitDocument(CurrentUser currentUser, @Valid @RequestBody SubmitDocumentRequest body) {
		return membership.submitDocument(currentUser, body.artifactType(), body.fileUrl());
	}

	public record UpdateMemberRequest(VendorUserRole role, VendorUserStatus status) {
	}

	public record InviteRequest(@NotBlank @Size(max = 200) String name, @NotBlank @Email @Size(max = 320) String email,
			@NotNull VendorUserRole role) {
	}

	public record ApproveRequest(VendorUserRole role) {
	}

	public record RejectRequest(@Size(max = 2000) String notes) {
	}

	public record SubmitDocumentRequest(@NotNull ArtifactType artifactType,
			@NotBlank @Size(max = 2000) String fileUrl) {
	}

}
