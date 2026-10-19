package org.isaas.vendorcatalog;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.isaas.vendorcatalog.TestData.email;
import static org.isaas.vendorcatalog.TestData.login;
import static org.isaas.vendorcatalog.TestData.unique;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Staff verification of vendors, and personal access tokens for agent apps. */
@SpringBootTest
@AutoConfigureMockMvc
class StaffAndAccessTokenTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	TestData data;

	@Test
	void staffVerifyThenActivateAVendor() throws Exception {
		String staffSubject = unique("auth0|staff");
		String staffEmail = email("staff");
		data.staff(staffEmail, staffSubject, "reviewer");
		UUID vendor = data.pendingVendor();

		// The database refuses an active vendor that isn't verified.
		mvc.perform(post("/staff/vendors/{id}/status", vendor).with(login(staffSubject, staffEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"status": "active"}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.constraint").value("vendors_active_requires_verified"));

		mvc.perform(post("/staff/vendors/{id}/verification", vendor).with(login(staffSubject, staffEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"decision": "verified"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.verificationStatus").value("verified"))
			.andExpect(jsonPath("$.status").value("pending"))
			.andExpect(jsonPath("$.verifiedAt", notNullValue()));

		mvc.perform(post("/staff/vendors/{id}/status", vendor).with(login(staffSubject, staffEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"status": "active"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("active"));

		mvc.perform(get("/staff/vendors/{id}", vendor).with(login(staffSubject, staffEmail)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.vendor.id").value(vendor.toString()));
	}

	@Test
	void staffReviewDocumentsSubmittedByTheVendorAdmin() throws Exception {
		UUID vendor = data.pendingVendor();
		String adminSubject = unique("auth0|admin");
		String adminEmail = email("admin");
		data.member(vendor, adminEmail, "admin", adminSubject);
		String staffSubject = unique("auth0|staff");
		String staffEmail = email("staff");
		data.staff(staffEmail, staffSubject, "reviewer");

		String submitted = mvc.perform(post("/vendor/verification-documents").with(login(adminSubject, adminEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"artifactType": "proof_of_authority", "fileUrl": "https://docs.test/letter.pdf"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("pending"))
			.andReturn().getResponse().getContentAsString();
		String documentId = JsonPath.read(submitted, "$.id");

		mvc.perform(post("/staff/verification-documents/{id}/review", documentId)
			.with(login(staffSubject, staffEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"decision": "approved", "notes": "officer listing matches"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("approved"))
			.andExpect(jsonPath("$.reviewedAt", notNullValue()));
	}

	@Test
	void membersAndDisabledStaffAreNotStaff() throws Exception {
		UUID vendor = data.activeVendor();
		String adminSubject = unique("auth0|admin");
		String adminEmail = email("admin");
		data.member(vendor, adminEmail, "admin", adminSubject);

		mvc.perform(get("/staff/vendors").with(login(adminSubject, adminEmail)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("STAFF_REQUIRED"));
	}

	@Test
	void anAccessTokenActsAsItsOwnerUntilRevoked() throws Exception {
		UUID vendor = data.activeVendor();
		String subject = unique("auth0|agent-owner");
		String userEmail = email("owner");
		UUID owner = data.member(vendor, userEmail, "sales", subject);

		String created = mvc.perform(post("/me/access-tokens").with(login(subject, userEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "my agent", "expiresInDays": 90}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.token", matchesPattern("isaas_vpat_[0-9A-Za-z]{43}")))
			.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(created, "$.token");
		String tokenId = JsonPath.read(created, "$.id");

		mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticatedWith").value("access_token"))
			.andExpect(jsonPath("$.membership.userId").value(owner.toString()))
			.andExpect(jsonPath("$.membership.role").value("sales"));

		// A token can't manage tokens, and the list never shows token values.
		mvc.perform(get("/me/access-tokens").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));
		mvc.perform(get("/me/access-tokens").with(login(subject, userEmail)))
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].token").doesNotExist())
			.andExpect(jsonPath("$[0].lastUsedAt", notNullValue()));

		mvc.perform(delete("/me/access-tokens/{id}", tokenId).with(login(subject, userEmail)))
			.andExpect(status().isNoContent());
		mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void disablingTheOwnerStopsTheirTokens() throws Exception {
		UUID vendor = data.activeVendor();
		String subject = unique("auth0|leaver");
		String userEmail = email("leaver");
		UUID owner = data.member(vendor, userEmail, "sales", subject);
		String created = mvc.perform(post("/me/access-tokens").with(login(subject, userEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "agent"}
					"""))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(created, "$.token");

		data.disableMember(owner);

		mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void unknownTokensAndOverlongLifetimesAreRefused() throws Exception {
		mvc.perform(get("/me").header(HttpHeaders.AUTHORIZATION, "Bearer isaas_vpat_" + "0".repeat(43)))
			.andExpect(status().isUnauthorized());

		UUID vendor = data.activeVendor();
		String subject = unique("auth0|greedy");
		String userEmail = email("greedy");
		data.member(vendor, userEmail, "admin", subject);
		mvc.perform(post("/me/access-tokens").with(login(subject, userEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "forever", "expiresInDays": 365}
					"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	void anUnaffiliatedUserCantCreateTokens() throws Exception {
		mvc.perform(post("/me/access-tokens").with(login(unique("auth0|nobody"), email("nobody")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "agent"}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("NOT_A_MEMBER"));
	}

}
