package gift.auth;

import gift.member.Member;
import lombok.RequiredArgsConstructor;
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
 * 2. /callback receives the authorization code, exchanges it for tokens,
 *    fetches user info, auto-registers if new, and issues a service JWT
 *
 * The issued JWT is used for subsequent API authentication via Authorization header.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/auth/kakao")
public class KakaoAuthController {
    private final KakaoLoginProperties properties;
    private final KakaoAuthService kakaoAuthService;
    private final JwtProvider jwtProvider;

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
        Member member = kakaoAuthService.loginWithKakao(code);
        String token = jwtProvider.createToken(member.getEmail());
        return ResponseEntity.ok(new TokenResponse(token));
    }
}
