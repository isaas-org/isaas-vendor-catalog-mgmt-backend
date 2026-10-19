package org.isaas.vendorcatalog.accesstoken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendorAccessTokenRepository extends JpaRepository<VendorAccessToken, UUID> {

	Optional<VendorAccessToken> findByTokenHash(byte[] tokenHash);

	List<VendorAccessToken> findByVendorUserIdOrderByCreatedAtDesc(UUID vendorUserId);

	Optional<VendorAccessToken> findByIdAndVendorUserId(UUID id, UUID vendorUserId);

	/** Records use at most once a minute per token, so every request doesn't write. */
	@Modifying
	@Query(nativeQuery = true, value = """
			UPDATE vendors.vendor_access_tokens
			SET last_used_at = now()
			WHERE id = :id AND (last_used_at IS NULL OR last_used_at < now() - interval '1 minute')
			""")
	int touch(@Param("id") UUID id);

}
