package org.isaas.vendorcatalog.vendor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorJoinRequestRepository extends JpaRepository<VendorJoinRequest, UUID> {

	List<VendorJoinRequest> findByVendorIdAndStatusOrderByCreatedAt(UUID vendorId, JoinRequestStatus status);

	Optional<VendorJoinRequest> findByIdAndVendorId(UUID id, UUID vendorId);

	List<VendorJoinRequest> findByAuthProviderIdOrderByCreatedAtDesc(String authProviderId);

}
