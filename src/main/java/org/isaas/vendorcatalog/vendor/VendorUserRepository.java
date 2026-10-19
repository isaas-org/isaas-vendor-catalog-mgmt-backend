package org.isaas.vendorcatalog.vendor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendorUserRepository extends JpaRepository<VendorUser, UUID> {

	Optional<VendorUser> findByAuthProviderId(String authProviderId);

	/**
	 * Binds an Auth0 identity to an invited or seeded row that has none yet.
	 * Callers must only pass an email the token marks as verified.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(nativeQuery = true, value = """
			UPDATE vendors.vendor_users
			SET auth_provider_id = :authProviderId
			WHERE lower(email) = lower(:email) AND auth_provider_id IS NULL
			""")
	int bindAuthProviderId(@Param("authProviderId") String authProviderId, @Param("email") String email);

	List<VendorUser> findByVendorIdOrderByCreatedAt(UUID vendorId);

	Optional<VendorUser> findByIdAndVendorId(UUID id, UUID vendorId);

	long countByVendorIdAndRoleAndStatus(UUID vendorId, VendorUserRole role, VendorUserStatus status);

}
