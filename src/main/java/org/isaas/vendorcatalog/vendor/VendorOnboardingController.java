package org.isaas.vendorcatalog.vendor;

import java.util.List;
import java.util.UUID;

import org.isaas.vendorcatalog.identity.CurrentUser;
import org.isaas.vendorcatalog.vendor.VendorViews.JoinRequestView;
import org.isaas.vendorcatalog.vendor.VendorViews.VendorSummary;
import org.isaas.vendorcatalog.vendor.VendorViews.VendorView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
public class VendorOnboardingController {

	private final VendorOnboardingService onboarding;

	public VendorOnboardingController(VendorOnboardingService onboarding) {
		this.onboarding = onboarding;
	}

	@PostMapping("/vendors")
	@ResponseStatus(HttpStatus.CREATED)
	public VendorView createVendor(CurrentUser currentUser, @Valid @RequestBody CreateVendorRequest body) {
		return onboarding.createVendor(currentUser, body.companyName(), body.slug(), body.websiteUrl(),
				body.supportEmail(), body.adminName());
	}

	@GetMapping("/vendors")
	public List<VendorSummary> searchJoinable(@RequestParam(name = "q", defaultValue = "") String query) {
		return onboarding.searchJoinable(query);
	}

	@PostMapping("/vendors/{vendorId}/join-requests")
	@ResponseStatus(HttpStatus.CREATED)
	public JoinRequestView requestToJoin(CurrentUser currentUser, @PathVariable UUID vendorId,
			@Valid @RequestBody JoinVendorRequest body) {
		return onboarding.requestToJoin(currentUser, vendorId, body.name(), body.requestedRole());
	}

	@GetMapping("/me/join-requests")
	public List<JoinRequestView> myJoinRequests(CurrentUser currentUser) {
		return onboarding.myJoinRequests(currentUser);
	}

	/** slug format is checked by the database (vendors_slug_format). */
	public record CreateVendorRequest(@NotBlank @Size(max = 200) String companyName,
			@NotBlank @Size(max = 100) String slug, @Size(max = 500) String websiteUrl,
			@Email @Size(max = 320) String supportEmail, @NotBlank @Size(max = 200) String adminName) {
	}

	public record JoinVendorRequest(@NotBlank @Size(max = 200) String name, @NotNull VendorUserRole requestedRole) {
	}

}
