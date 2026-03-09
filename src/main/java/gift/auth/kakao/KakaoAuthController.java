package gift.auth.kakao;

import gift.auth.OAuthLoginService;
import gift.auth.TokenResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/*
 * Handles the Kakao OAuth2 login flow.
 * 1. /login redirects the user to Kakao's authorization page
 * 2. /callback receives the authorization code and delegates to OAuthLoginService
 */
@RestController
@RequestMapping("/api/auth/kakao")
public class KakaoAuthController {
    private final KakaoLoginProperties properties;
    private final OAuthLoginService oAuthLoginService;

    public KakaoAuthController(KakaoLoginProperties properties, OAuthLoginService oAuthLoginService) {
        this.properties = properties;
        this.oAuthLoginService = oAuthLoginService;
    }

    @GetMapping(path = "/login")
    public ResponseEntity<Void> login() {
        String kakaoAuthUrl = UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", properties.clientId())
            .queryParam("redirect_uri", properties.redirectUri())
            .queryParam("scope", "account_email,talk_message")
            .build()
            .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, kakaoAuthUrl)
            .build();
    }

    @GetMapping(path = "/callback")
    public ResponseEntity<TokenResponse> callback(@RequestParam("code") String code) {
        return ResponseEntity.ok(oAuthLoginService.login(code));
    }
}
