package gift.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.dto.MemberRequest;
import gift.model.Member;
import gift.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("POST /api/members/register - 회원가입에 성공하면 201과 토큰을 반환한다")
    void register() throws Exception {
        var request = new MemberRequest("newuser@example.com", "password123");

        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("POST /api/members/register - 이미 등록된 이메일로 가입하면 400을 반환한다")
    void registerDuplicateEmail() throws Exception {
        memberRepository.save(new Member("existing@example.com", "password"));
        var request = new MemberRequest("existing@example.com", "password123");

        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/members/register - 이메일 형식이 아니면 400을 반환한다")
    void registerInvalidEmail() throws Exception {
        var request = new MemberRequest("not-an-email", "password123");

        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/members/register - 비밀번호가 빈 문자열이면 400을 반환한다")
    void registerBlankPassword() throws Exception {
        var request = new MemberRequest("test@example.com", "");

        mockMvc.perform(post("/api/members/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/members/login - 올바른 자격 증명으로 로그인하면 토큰을 반환한다")
    void login() throws Exception {
        memberRepository.save(new Member("user@example.com", passwordEncoder.encode("password123")));
        var request = new MemberRequest("user@example.com", "password123");

        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("POST /api/members/login - 잘못된 비밀번호로 로그인하면 400을 반환한다")
    void loginWrongPassword() throws Exception {
        memberRepository.save(new Member("user@example.com", passwordEncoder.encode("correctPassword")));
        var request = new MemberRequest("user@example.com", "wrongPassword");

        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/members/login - 존재하지 않는 이메일로 로그인하면 400을 반환한다")
    void loginNonExistentEmail() throws Exception {
        var request = new MemberRequest("unknown@example.com", "password");

        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/members/login - 카카오 회원(비밀번호 null)은 비밀번호 로그인이 실패한다")
    void loginKakaoMemberWithPasswordFails() throws Exception {
        memberRepository.save(new Member("kakao@example.com"));  // password is null
        var request = new MemberRequest("kakao@example.com", "anyPassword");

        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
