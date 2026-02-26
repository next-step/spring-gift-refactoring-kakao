package gift.member;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/members")
public class AdminMemberController {
    private final MemberRepository memberRepository;

    public AdminMemberController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("members", memberRepository.findAll());
        return "member/list";
    }

    @GetMapping("/new")
    public String newForm() {
        return "member/new";
    }

    @PostMapping
    public String create(
        @RequestParam String email,
        @RequestParam String password,
        Model model
    ) {
        if (memberRepository.existsByEmail(email)) {
            populateNewFormError(model, email, "이미 등록된 이메일입니다.");
            return "member/new";
        }

        memberRepository.save(new Member(email, password));
        return "redirect:/admin/members";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        final Member member = memberRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. id=" + id));
        model.addAttribute("member", member);
        return "member/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(
        @PathVariable Long id,
        @RequestParam String email,
        @RequestParam String password
    ) {
        final Member member = memberRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. id=" + id));
        member.update(email, password);
        memberRepository.save(member);
        return "redirect:/admin/members";
    }

    @PostMapping("/{id}/charge-point")
    public String chargePoint(
        @PathVariable Long id,
        @RequestParam int amount
    ) {
        final Member member = memberRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. id=" + id));
        member.chargePoint(amount);
        memberRepository.save(member);
        return "redirect:/admin/members";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        memberRepository.deleteById(id);
        return "redirect:/admin/members";
    }

    private void populateNewFormError(Model model, String email, String error) {
        model.addAttribute("error", error);
        model.addAttribute("email", email);
    }
}
