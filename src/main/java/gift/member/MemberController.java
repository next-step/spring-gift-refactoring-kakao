package gift.member;

import gift.auth.JwtPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles member registration and login.
 *
 * @author brian.kim
 * @since 1.0
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;
    private final JwtPort jwtPort;

    @PostMapping("/register")
    public ResponseEntity<MemberResponse> register(@Valid @RequestBody MemberRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        final Member member = memberRepository.save(
                Member.builder()
                        .email(request.email())
                        .password(request.password())
                        .build()
        );

        Long memberId = member.getId();

        final String token = jwtPort.issueMemberJwt(memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MemberResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<MemberResponse> login(@Valid @RequestBody MemberRequest request) {
        final Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (member.getPassword() == null || !member.getPassword().equals(request.password())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        Long memberId = member.getId();

        final String token = jwtPort.issueMemberJwt(memberId);
        return ResponseEntity.ok(new MemberResponse(token));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
