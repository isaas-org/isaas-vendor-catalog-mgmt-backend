package org.isaas.vendorcatalog;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.isaas.vendorcatalog.TestData.email;
import static org.isaas.vendorcatalog.TestData.login;
import static org.isaas.vendorcatalog.TestData.unique;
import static org.isaas.vendorcatalog.TestData.unverifiedLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Creating a vendor, joining one, invites and member management. */
@SpringBootTest
@AutoConfigureMockMvc
class MembershipTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	TestData data;

	@Test
	void creatingAVendorMakesYouItsAdminAndStartsItPending() throws Exception {
		String subject = unique("auth0|founder");
		String email = email("founder");
		String slug = unique("acme");

		mvc.perform(post("/vendors").with(login(subject, email))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"companyName": "Acme", "slug": "%s", "adminName": "Ada Founder"}
					""".formatted(slug)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("pending"))
			.andExpect(jsonPath("$.verificationStatus").value("pending"));

		mvc.perform(get("/me").with(login(subject, email)))
			.andExpect(jsonPath("$.membership.role").value("admin"))
			.andExpect(jsonPath("$.membership.vendorSlug").value(slug));

		mvc.perform(post("/vendors").with(login(subject, email))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"companyName": "Second", "slug": "%s", "adminName": "Ada"}
					""".formatted(unique("second"))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_A_MEMBER"));
	}

	@Test
	void creatingAVendorNeedsAVerifiedEmail() throws Exception {
		mvc.perform(post("/vendors").with(unverifiedLogin(unique("auth0|x"), email("x")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"companyName": "Acme", "slug": "%s", "adminName": "Ada"}
					""".formatted(unique("acme"))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
	}

	@Test
	void slugRulesComeFromTheDatabase() throws Exception {
		mvc.perform(post("/vendors").with(login(unique("auth0|slug"), email("slug")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"companyName": "Acme", "slug": "Not A Slug", "adminName": "Ada"}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("RULE_VIOLATION"))
			.andExpect(jsonPath("$.constraint").value("vendors_slug_format"));
	}

	@Test
	void aPendingVendorCantBeAskedToJoinAndIsntListed() throws Exception {
		UUID pending = data.pendingVendor();
		String subject = unique("auth0|joiner");

		mvc.perform(post("/vendors/{id}/join-requests", pending).with(login(subject, email("joiner")))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Jo Joiner", "requestedRole": "sales"}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("RULE_VIOLATION"))
			.andExpect(jsonPath("$.detail").value(containsString("join request not allowed")));

		mvc.perform(get("/vendors").param("q", "Vendor").with(login(subject, email("joiner"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", not(hasItem(pending.toString()))));
	}

	@Test
	void anAdminApprovesAJoinRequestAndTheRequesterBecomesAMember() throws Exception {
		UUID vendor = data.activeVendor();
		String adminSubject = unique("auth0|admin");
		String adminEmail = email("admin");
		data.member(vendor, adminEmail, "admin", adminSubject);
		String joinerSubject = unique("auth0|joiner");
		String joinerEmail = email("joiner");

		String created = mvc.perform(post("/vendors/{id}/join-requests", vendor)
			.with(login(joinerSubject, joinerEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Jo Joiner", "requestedRole": "admin"}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("pending"))
			.andReturn().getResponse().getContentAsString();
		String requestId = JsonPath.read(created, "$.id");

		// The second open request from the same person is refused by the database.
		mvc.perform(post("/vendors/{id}/join-requests", vendor).with(login(joinerSubject, joinerEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Jo Joiner", "requestedRole": "sales"}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.constraint").value("vendor_join_requests_one_pending"));

		// The admin grants a lower role than requested.
		mvc.perform(post("/vendor/join-requests/{id}/approve", requestId).with(login(adminSubject, adminEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"role": "support"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.role").value("support"))
			.andExpect(jsonPath("$.signedIn").value(true));

		mvc.perform(get("/me").with(login(joinerSubject, joinerEmail)))
			.andExpect(jsonPath("$.membership.vendorId").value(vendor.toString()))
			.andExpect(jsonPath("$.membership.role").value("support"));

		mvc.perform(post("/vendor/join-requests/{id}/reject", requestId).with(login(adminSubject, adminEmail)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("NOT_PENDING"));
	}

	@Test
	void aPendingVendorsAdminCanInviteAndTheInviteeGetsInOnFirstLogin() throws Exception {
		UUID vendor = data.pendingVendor();
		String adminSubject = unique("auth0|admin");
		String adminEmail = email("admin");
		data.member(vendor, adminEmail, "admin", adminSubject);
		String inviteeEmail = email("finance");

		mvc.perform(post("/vendor/invitations").with(login(adminSubject, adminEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Fin Ance", "email": "%s", "role": "support"}
					""".formatted(inviteeEmail)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.signedIn").value(false));

		mvc.perform(get("/me").with(login(unique("auth0|finance"), inviteeEmail)))
			.andExpect(jsonPath("$.membership.vendorId").value(vendor.toString()))
			.andExpect(jsonPath("$.membership.role").value("support"));

		// One email, one vendor.
		mvc.perform(post("/vendor/invitations").with(login(adminSubject, adminEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "Again", "email": "%s", "role": "sales"}
					""".formatted(inviteeEmail)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.constraint").value("vendor_users_email_key"));
	}

	@Test
	void onlyAnAdminManagesMembers() throws Exception {
		UUID vendor = data.activeVendor();
		String salesSubject = unique("auth0|sales");
		String salesEmail = email("sales");
		data.member(vendor, salesEmail, "sales", salesSubject);

		mvc.perform(post("/vendor/invitations").with(login(salesSubject, salesEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"name": "X", "email": "%s", "role": "admin"}
					""".formatted(email("x"))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ADMIN_REQUIRED"));
	}

	@Test
	void theLastActiveAdminCantBeDemotedOrDisabled() throws Exception {
		UUID vendor = data.activeVendor();
		String adminSubject = unique("auth0|admin");
		String adminEmail = email("admin");
		UUID admin = data.member(vendor, adminEmail, "admin", adminSubject);

		mvc.perform(patch("/vendor/members/{id}", admin).with(login(adminSubject, adminEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"role": "sales"}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("LAST_ADMIN"));

		UUID second = data.member(vendor, email("second"), "admin", null);
		mvc.perform(patch("/vendor/members/{id}", second).with(login(adminSubject, adminEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"status": "disabled"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("disabled"));
	}

	@Test
	void anAdminCantTouchAnotherVendorsMembers() throws Exception {
		UUID vendorA = data.activeVendor();
		String adminSubject = unique("auth0|admin");
		String adminEmail = email("admin");
		data.member(vendorA, adminEmail, "admin", adminSubject);
		UUID otherMember = data.member(data.activeVendor(), email("other"), "sales", null);

		mvc.perform(patch("/vendor/members/{id}", otherMember).with(login(adminSubject, adminEmail))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"status": "disabled"}
					"""))
			.andExpect(status().isNotFound());
	}

}
