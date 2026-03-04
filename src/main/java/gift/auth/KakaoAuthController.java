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
 * 카카오 OAuth2 로그인 플로우를 처리한다.
 * 1. /login - 카카오 인가 페이지로 리다이렉트
 * 2. /callback - 인가 코드를 받아 토큰 교환, 사용자 정보 조회,
 *    신규 회원 자동 가입 후 서비스 JWT 발급
 *
 * 발급된 JWT는 이후 API 인증에 Authorization 헤더로 사용된다.
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
