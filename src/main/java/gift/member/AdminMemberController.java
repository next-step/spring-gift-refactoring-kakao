package gift.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Controller
@RequestMapping("/admin/members")
public class AdminMemberController {
    private final MemberService memberService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("members", memberService.findAll());
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
        try {
            memberService.create(email, password);
            return "redirect:/admin/members";
        } catch (IllegalArgumentException e) {
            populateNewFormError(model, email, e.getMessage());
            return "member/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Member member = memberService.findById(id);
        model.addAttribute("member", member);
        return "member/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @RequestParam String email,
            @RequestParam String password
    ) {
        memberService.update(id, email, password);
        return "redirect:/admin/members";
    }

    @PostMapping("/{id}/charge-point")
    public String chargePoint(
            @PathVariable Long id,
            @RequestParam int amount
    ) {
        memberService.chargePoint(id, amount);
        return "redirect:/admin/members";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        memberService.delete(id);
        return "redirect:/admin/members";
    }

    private void populateNewFormError(Model model, String email, String error) {
        model.addAttribute("error", error);
        model.addAttribute("email", email);
    }
}
