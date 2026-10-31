package org.isaas.vendorcatalog.accesstoken;

import java.util.List;
import java.util.UUID;

import org.isaas.vendorcatalog.accesstoken.AccessTokenService.AccessTokenView;
import org.isaas.vendorcatalog.accesstoken.AccessTokenService.CreatedAccessToken;
import org.isaas.vendorcatalog.identity.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/me/access-tokens")
public class AccessTokenController {

	private static final int DEFAULT_LIFETIME_DAYS = 30;

	private final AccessTokenService accessTokens;

	public AccessTokenController(AccessTokenService accessTokens) {
		this.accessTokens = accessTokens;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CreatedAccessToken create(CurrentUser currentUser, @Valid @RequestBody CreateAccessTokenRequest body) {
		int days = body.expiresInDays() == null ? DEFAULT_LIFETIME_DAYS : body.expiresInDays();
		return accessTokens.create(currentUser, body.name(), days);
	}

	@GetMapping
	public List<AccessTokenView> list(CurrentUser currentUser) {
		return accessTokens.list(currentUser);
	}

	@DeleteMapping("/{tokenId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void revoke(CurrentUser currentUser, @PathVariable UUID tokenId) {
		accessTokens.revoke(currentUser, tokenId);
	}

	/** The database caps the lifetime at 90 days too (vendor_access_tokens_max_lifetime). */
	public record CreateAccessTokenRequest(@NotBlank @Size(max = 100) String name,
			@Min(1) @Max(90) Integer expiresInDays) {
	}

}
