package org.isaas.vendorcatalog.vendor;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

	/** Join targets: only active (so verified) vendors are listed. */
	List<Vendor> findTop20ByStatusAndCompanyNameContainingIgnoreCaseOrderByCompanyName(VendorStatus status,
			String nameFragment);

	List<Vendor> findByVerificationStatusOrderByCreatedAt(VerificationStatus verificationStatus);

}
