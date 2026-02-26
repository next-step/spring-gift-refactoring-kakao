package gift.ui.seed;

import gift.application.CreateMemberRequest;
import gift.application.MemberService;
import gift.model.Member;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("seed")
@RestController
@RequestMapping("/api/seed/members")
public class SeedMemberController {
    private final MemberService memberService;

    public SeedMemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public Member create(@RequestBody final CreateMemberRequest request) {
        return memberService.create(request);
    }
}
