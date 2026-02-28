package gift.member;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.auth.TokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    @Nested
    @DisplayName("POST /api/members/register")
    class Register {

        @Test
        @DisplayName("정상 가입 시 201과 토큰을 반환한다")
        void returnsCreatedWithToken() throws Exception {
            var request = new MemberRequest("test@test.com", "password");

            given(memberService.register(any(MemberRequest.class)))
                .willReturn(new TokenResponse("jwt-token"));

            mockMvc.perform(post("/api/members/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"));
        }

        @Test
        @DisplayName("이메일 중복 시 400을 반환한다")
        void returnsBadRequestWhenDuplicated() throws Exception {
            var request = new MemberRequest("dup@test.com", "password");

            given(memberService.register(any(MemberRequest.class)))
                .willThrow(new IllegalArgumentException("Email is already registered."));

            mockMvc.perform(post("/api/members/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("이메일 형식이 잘못되면 400을 반환한다")
        void returnsBadRequestWhenEmailInvalid() throws Exception {
            var body = """
                {"email": "not-an-email", "password": "password"}
                """;

            mockMvc.perform(post("/api/members/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("비밀번호가 빈 값이면 400을 반환한다")
        void returnsBadRequestWhenPasswordBlank() throws Exception {
            var body = """
                {"email": "test@test.com", "password": ""}
                """;

            mockMvc.perform(post("/api/members/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/members/login")
    class Login {

        @Test
        @DisplayName("정상 로그인 시 200과 토큰을 반환한다")
        void returnsOkWithToken() throws Exception {
            var request = new MemberRequest("test@test.com", "password");

            given(memberService.login(any(MemberRequest.class)))
                .willReturn(new TokenResponse("jwt-token"));

            mockMvc.perform(post("/api/members/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
        }

        @Test
        @DisplayName("인증 실패 시 400을 반환한다")
        void returnsBadRequestWhenAuthFails() throws Exception {
            var request = new MemberRequest("test@test.com", "wrong");

            given(memberService.login(any(MemberRequest.class)))
                .willThrow(new IllegalArgumentException("Invalid email or password."));

            mockMvc.perform(post("/api/members/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }
}
