package gift.wish;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.auth.AuthenticationResolver;
import gift.category.Category;
import gift.member.Member;
import gift.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WishController.class)
class WishControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WishService wishService;

    @MockitoBean
    private AuthenticationResolver authenticationResolver;

    private Member member;
    private Product product;
    private Wish wish;

    @BeforeEach
    void setUp() throws Exception {
        var category = new Category("교환권", "#ffffff", "img.png", "");
        setId(category, 1L);

        product = new Product("아메리카노", 5000, "img.png", category);
        setId(product, 1L);

        member = new Member("test@test.com", "password");
        setId(member, 1L);

        wish = new Wish(1L, product);
        setId(wish, 1L);
    }

    private void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }

    @Nested
    @DisplayName("GET /api/wishes")
    class GetWishes {

        @Test
        @DisplayName("위시 목록을 반환한다")
        void returnsWishes() throws Exception {
            given(authenticationResolver.extractMember(anyString())).willReturn(member);
            given(wishService.getWishes(eq(1L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(WishResponse.from(wish))));

            mockMvc.perform(get("/api/wishes")
                    .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productId").value(1));
        }

        @Test
        @DisplayName("인증 실패 시 401을 반환한다")
        void returnsUnauthorized() throws Exception {
            given(authenticationResolver.extractMember(anyString())).willReturn(null);

            mockMvc.perform(get("/api/wishes")
                    .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/wishes")
    class AddWish {

        @Test
        @DisplayName("신규 위시 생성 시 201을 반환한다")
        void returnsCreatedForNewWish() throws Exception {
            var request = new WishRequest(1L);

            given(authenticationResolver.extractMember(anyString())).willReturn(member);
            given(wishService.addWish(eq(1L), any(WishRequest.class)))
                .willReturn(new WishService.AddWishResult(wish, true));

            mockMvc.perform(post("/api/wishes")
                    .header("Authorization", "Bearer test-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1));
        }

        @Test
        @DisplayName("이미 존재하는 위시면 200을 반환한다")
        void returnsOkForExistingWish() throws Exception {
            var request = new WishRequest(1L);

            given(authenticationResolver.extractMember(anyString())).willReturn(member);
            given(wishService.addWish(eq(1L), any(WishRequest.class)))
                .willReturn(new WishService.AddWishResult(wish, false));

            mockMvc.perform(post("/api/wishes")
                    .header("Authorization", "Bearer test-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1));
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 404를 반환한다")
        void returnsNotFound() throws Exception {
            var request = new WishRequest(999L);

            given(authenticationResolver.extractMember(anyString())).willReturn(member);
            given(wishService.addWish(eq(1L), any(WishRequest.class)))
                .willThrow(new NoSuchElementException("상품이 존재하지 않습니다."));

            mockMvc.perform(post("/api/wishes")
                    .header("Authorization", "Bearer test-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("인증 실패 시 401을 반환한다")
        void returnsUnauthorized() throws Exception {
            var request = new WishRequest(1L);

            given(authenticationResolver.extractMember(anyString())).willReturn(null);

            mockMvc.perform(post("/api/wishes")
                    .header("Authorization", "Bearer invalid")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /api/wishes/{id}")
    class RemoveWish {

        @Test
        @DisplayName("위시를 삭제하고 204를 반환한다")
        void returnsNoContent() throws Exception {
            given(authenticationResolver.extractMember(anyString())).willReturn(member);

            mockMvc.perform(delete("/api/wishes/1")
                    .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("위시가 존재하지 않으면 404를 반환한다")
        void returnsNotFound() throws Exception {
            given(authenticationResolver.extractMember(anyString())).willReturn(member);
            willThrow(new NoSuchElementException("위시가 존재하지 않습니다."))
                .given(wishService).removeWish(1L, 999L);

            mockMvc.perform(delete("/api/wishes/999")
                    .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("본인의 위시가 아니면 403을 반환한다")
        void returnsForbidden() throws Exception {
            given(authenticationResolver.extractMember(anyString())).willReturn(member);
            willThrow(new IllegalArgumentException("본인의 위시만 삭제할 수 있습니다."))
                .given(wishService).removeWish(1L, 1L);

            mockMvc.perform(delete("/api/wishes/1")
                    .header("Authorization", "Bearer test-token"))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증 실패 시 401을 반환한다")
        void returnsUnauthorized() throws Exception {
            given(authenticationResolver.extractMember(anyString())).willReturn(null);

            mockMvc.perform(delete("/api/wishes/1")
                    .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        }
    }
}
