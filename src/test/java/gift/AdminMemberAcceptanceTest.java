package gift;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import gift.member.Member;
import gift.member.MemberRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AdminMemberAcceptanceTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Autowired
    MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        databaseCleaner.clear();
    }

    @Test
    void 회원_목록_조회() throws Exception {
        memberRepository.save(new Member("a@test.com", "pw"));
        memberRepository.save(new Member("b@test.com", "pw"));

        mockMvc.perform(get("/admin/members"))
            .andExpect(status().isOk())
            .andExpect(view().name("member/list"))
            .andExpect(model().attribute("members", hasSize(2)));
    }

    @Test
    void 회원_생성_성공() throws Exception {
        mockMvc.perform(post("/admin/members")
                .param("email", "new@test.com")
                .param("password", "pw"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/members"));
    }

    @Test
    void 회원_생성_실패_이메일_중복() throws Exception {
        memberRepository.save(new Member("dup@test.com", "pw"));

        mockMvc.perform(post("/admin/members")
                .param("email", "dup@test.com")
                .param("password", "pw"))
            .andExpect(status().isOk())
            .andExpect(view().name("member/new"))
            .andExpect(model().attributeExists("error"));
    }

    @Test
    void 회원_수정() throws Exception {
        Member member = memberRepository.save(new Member("old@test.com", "pw"));

        mockMvc.perform(post("/admin/members/{id}/edit", member.getId())
                .param("email", "new@test.com")
                .param("password", "newpw"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/members"));
    }

    @Test
    void 포인트_충전() throws Exception {
        Member member = memberRepository.save(new Member("a@test.com", "pw"));

        mockMvc.perform(post("/admin/members/{id}/charge-point", member.getId())
                .param("amount", "1000"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/members"));
    }

    @Test
    void 회원_삭제() throws Exception {
        Member member = memberRepository.save(new Member("a@test.com", "pw"));

        mockMvc.perform(post("/admin/members/{id}/delete", member.getId()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/members"));
    }
}
