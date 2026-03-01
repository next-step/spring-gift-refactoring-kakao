package gift.member.admin;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Admin controller for managing members.
 *
 * @author brian.kim
 * @since 1.0
 */
@Controller
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @GetMapping
    public String list(Model model) {
        List<MemberDto> members = adminMemberService.getAllMembers();

        model.addAttribute("members", members);

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
        if (adminMemberService.hasEmailRegistered(email)) {
            model.addAttribute("error", "Email is already registered.");
            model.addAttribute("email", email);
            return "member/new";
        }

        adminMemberService.createMember(email, password);

        return "redirect:/admin/members";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        MemberDto member = adminMemberService.findMember(id);

        model.addAttribute("member", member);

        return "member/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @RequestParam String email,
            @RequestParam String password
    ) {
        adminMemberService.updateMember(id, email, password);

        return "redirect:/admin/members";
    }

    @PostMapping("/{id}/charge-point")
    public String chargePoint(
            @PathVariable Long id,
            @RequestParam int amount
    ) {
        adminMemberService.chargePoint(id, amount);

        return "redirect:/admin/members";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        adminMemberService.deleteMember(id);

        return "redirect:/admin/members";
    }
}
