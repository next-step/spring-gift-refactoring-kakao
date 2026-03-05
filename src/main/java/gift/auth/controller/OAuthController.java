package gift.auth.controller;

import gift.auth.exception.InvalidOAuthProviderException;
import gift.auth.service.OAuthService;
import gift.auth.dto.TokenResponse;
import gift.external.ExternalProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/auth")
@RequiredArgsConstructor
public class OAuthController {
    private final OAuthService oAuthService;

    @GetMapping(path = "/{provider}/login")
    public ResponseEntity<Void> login(@PathVariable String provider) {
        ExternalProvider externalProvider = parseProvider(provider);
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, oAuthService.getLoginUri(externalProvider).toString())
            .build();
    }

    @GetMapping(path = "/{provider}/callback")
    public ResponseEntity<TokenResponse> callback(
        @PathVariable String provider,
        @RequestParam("code") String code
    ) {
        ExternalProvider externalProvider = parseProvider(provider);
        return ResponseEntity.ok(oAuthService.handleCallback(externalProvider, code));
    }

    private ExternalProvider parseProvider(String provider) {
        try {
            return ExternalProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOAuthProviderException();
        }
    }
}
