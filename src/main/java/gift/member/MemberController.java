package gift.member;

import gift.auth.JwtProvider;
import gift.auth.TokenResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberController {
  private final MemberService memberService;
  private final JwtProvider jwtProvider;

  public MemberController(MemberService memberService, JwtProvider jwtProvider) {
    this.memberService = memberService;
    this.jwtProvider = jwtProvider;
  }

  @PostMapping("/register")
  public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterMemberRequest request) {
    Member member = memberService.register(request.email(), request.password());
    String token = jwtProvider.createToken(member.getEmail());
    return ResponseEntity.created(URI.create("/api/members/" + member.getId()))
        .body(new TokenResponse(token));
  }

  @PostMapping("/login")
  public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginMemberRequest request) {
    Member member = memberService.login(request.email(), request.password());
    String token = jwtProvider.createToken(member.getEmail());
    return ResponseEntity.ok(new TokenResponse(token));
  }
}
