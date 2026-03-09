package gift.auth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gift.member.Member;
import gift.member.MemberRepository;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminAuthController {

    private final MemberRepository memberRepository;

    public AdminAuthController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @GetMapping("/login")
    public String loginForm() {
        return "admin/login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model
    ) {
        Member member = memberRepository.findByEmail(email).orElse(null);
        if (member == null || !member.authenticate(password) || !member.isAdmin()) {
            model.addAttribute("error", "관리자 계정으로 로그인해주세요.");
            return "admin/login";
        }

        session.setAttribute("adminMemberId", member.getId());
        return "redirect:/admin/products";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login";
    }
}
