package org.isaas.vendorcatalog.staff;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffUserRepository extends JpaRepository<StaffUser, UUID> {

	Optional<StaffUser> findByAuthProviderId(String authProviderId);

	/** Same first-login binding as vendor users; only for a verified email. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(nativeQuery = true, value = """
			UPDATE platform.staff_users
			SET auth_provider_id = :authProviderId
			WHERE lower(email) = lower(:email) AND auth_provider_id IS NULL
			""")
	int bindAuthProviderId(@Param("authProviderId") String authProviderId, @Param("email") String email);

}
