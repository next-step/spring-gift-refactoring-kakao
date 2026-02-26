package gift.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/*
 * 카카오 OAuth2 로그인 흐름을 처리한다.
 * 1. /login — 사용자를 카카오 인가 페이지로 리다이렉트
 * 2. /callback — 인가 코드를 받아 액세스 토큰으로 교환하고,
 *    사용자 정보를 조회하여 미가입 시 자동 회원가입 후 서비스 JWT를 발급
 */
@RestController
@RequestMapping("/api/auth/kakao")
public class KakaoAuthController {
    private final KakaoLoginProperties properties;
    private final KakaoAuthService kakaoAuthService;

    public KakaoAuthController(KakaoLoginProperties properties, KakaoAuthService kakaoAuthService) {
        this.properties = properties;
        this.kakaoAuthService = kakaoAuthService;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        final String kakaoAuthUrl = UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
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

    @GetMapping("/callback")
    public ResponseEntity<TokenResponse> callback(@RequestParam("code") String code) {
        final TokenResponse tokenResponse = kakaoAuthService.handleCallback(code);
        return ResponseEntity.ok(tokenResponse);
    }
}
