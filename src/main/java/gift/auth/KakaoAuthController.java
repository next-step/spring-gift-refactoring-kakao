package gift.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * 카카오 OAuth2 로그인 흐름을 처리한다.
 * 1. /login - 카카오 인가 페이지로 리다이렉트
 * 2. /callback - 인가 코드를 받아 액세스 토큰으로 교환하고,
 *    사용자 정보를 조회하여 신규 회원이면 자동 등록 후 서비스 JWT 발급
 */
@RestController
@RequestMapping("/api/auth/kakao")
public class KakaoAuthController {
    private final KakaoAuthService kakaoAuthService;

    @Autowired
    public KakaoAuthController(KakaoAuthService kakaoAuthService) {
        this.kakaoAuthService = kakaoAuthService;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        String kakaoAuthUrl = kakaoAuthService.buildAuthorizationUrl();
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, kakaoAuthUrl)
            .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<TokenResponse> callback(@RequestParam("code") String code) {
        return ResponseEntity.ok(kakaoAuthService.processCallback(code));
    }
}
