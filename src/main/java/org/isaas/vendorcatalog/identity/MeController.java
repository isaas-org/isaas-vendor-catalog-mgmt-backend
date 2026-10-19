package org.isaas.vendorcatalog.identity;

import java.util.UUID;

import org.isaas.vendorcatalog.staff.StaffRole;
import org.isaas.vendorcatalog.staff.StaffUser;
import org.isaas.vendorcatalog.vendor.Vendor;
import org.isaas.vendorcatalog.vendor.VendorRepository;
import org.isaas.vendorcatalog.vendor.VendorStatus;
import org.isaas.vendorcatalog.vendor.VendorUser;
import org.isaas.vendorcatalog.vendor.VendorUserRole;
import org.isaas.vendorcatalog.vendor.VendorUserStatus;
import org.isaas.vendorcatalog.vendor.VerificationStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Who the caller is. Frontends use it to route an unaffiliated user to
 * onboarding; agent apps use it for their whoami tool.
 */
@RestController
public class MeController {

	private final VendorRepository vendors;

	public MeController(VendorRepository vendors) {
		this.vendors = vendors;
	}

	@GetMapping("/me")
	public MeResponse me(CurrentUser currentUser) {
		return new MeResponse(currentUser.subject(), currentUser.email(), currentUser.emailVerified(),
				currentUser.via(), membership(currentUser.member()), staff(currentUser.staff()));
	}

	private Membership membership(VendorUser member) {
		if (member == null) {
			return null;
		}
		Vendor vendor = vendors.findById(member.getVendorId()).orElseThrow();
		return new Membership(member.getId(), member.getRole(), member.getStatus(), vendor.getId(),
				vendor.getCompanyName(), vendor.getSlug(), vendor.getStatus(), vendor.getVerificationStatus());
	}

	private static Staff staff(StaffUser staffUser) {
		return staffUser == null ? null : new Staff(staffUser.getId(), staffUser.getRole(), staffUser.isActive());
	}

	public record MeResponse(String subject, String email, boolean emailVerified, CurrentUser.Via authenticatedWith,
			Membership membership, Staff staff) {
	}

	public record Membership(UUID userId, VendorUserRole role, VendorUserStatus status, UUID vendorId,
			String vendorName, String vendorSlug, VendorStatus vendorStatus, VerificationStatus verificationStatus) {
	}

	public record Staff(UUID staffId, StaffRole role, boolean active) {
	}

}
