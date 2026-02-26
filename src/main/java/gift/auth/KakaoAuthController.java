package gift.auth;

import gift.member.Member;
import gift.member.MemberRepository;
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
    private final KakaoLoginClient kakaoLoginClient;
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    public KakaoAuthController(
            KakaoLoginProperties properties,
            KakaoLoginClient kakaoLoginClient,
            MemberRepository memberRepository,
            JwtProvider jwtProvider) {
        this.properties = properties;
        this.kakaoLoginClient = kakaoLoginClient;
        this.memberRepository = memberRepository;
        this.jwtProvider = jwtProvider;
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
        final KakaoLoginClient.KakaoTokenResponse kakaoToken = kakaoLoginClient.requestAccessToken(code);
        final KakaoLoginClient.KakaoUserResponse kakaoUser = kakaoLoginClient.requestUserInfo(kakaoToken.accessToken());
        final String email = kakaoUser.email();

        final Member member = memberRepository.findByEmail(email).orElseGet(() -> new Member(email));
        member.updateKakaoAccessToken(kakaoToken.accessToken());
        memberRepository.save(member);

        final String token = jwtProvider.createToken(member.getEmail());
        return ResponseEntity.ok(new TokenResponse(token));
    }
}
