package gift.member.internal;

import gift.auth.JwtPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private final MemberService memberService;
    private final JwtPort jwtPort;

    @PostMapping("/register")
    public ResponseEntity<MemberResponse> register(
            @Valid @RequestBody MemberRequest request
    ) {

        Long memberId = memberService.register(request);

        String token = jwtPort.issueMemberJwt(memberId);

        MemberResponse response = new MemberResponse(token);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<MemberResponse> login(
            @Valid @RequestBody MemberRequest request
    ) {

        Long memberId = memberService.login(request);

        String token = jwtPort.issueMemberJwt(memberId);

        MemberResponse response = new MemberResponse(token);

        return ResponseEntity.ok(response);
    }
}
