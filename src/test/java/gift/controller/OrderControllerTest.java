package gift.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gift.auth.JwtProvider;
import gift.client.KakaoMessageClient;
import gift.dto.OrderRequest;
import gift.model.Category;
import gift.model.Member;
import gift.model.Option;
import gift.model.Order;
import gift.model.Product;
import gift.repository.CategoryRepository;
import gift.repository.MemberRepository;
import gift.repository.OptionRepository;
import gift.repository.OrderRepository;
import gift.repository.ProductRepository;
import gift.repository.WishRepository;
import gift.model.Wish;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private KakaoMessageClient kakaoMessageClient;

    private Member member;
    private String token;
    private Option option;

    @BeforeEach
    void setUp() {
        member = new Member("order-test@example.com", "password");
        member.chargePoint(10_000_000);
        member = memberRepository.save(member);
        token = jwtProvider.createToken(member.getEmail());

        var category = categoryRepository.save(new Category("전자기기", "#1E90FF", "https://example.com/img.jpg", "설명"));
        var product = productRepository.save(new Product("테스트상품", 10000, "https://example.com/img.jpg", category));
        option = optionRepository.save(new Option(product, "기본옵션", 100));
    }

    @Test
    @DisplayName("POST /api/orders - 주문을 생성하면 재고와 포인트가 차감된다")
    void createOrder() throws Exception {
        var request = new OrderRequest(option.getId(), 2, "선물입니다");

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.optionId").value(option.getId()))
            .andExpect(jsonPath("$.quantity").value(2))
            .andExpect(jsonPath("$.message").value("선물입니다"));

        var updatedOption = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(98);

        var updatedMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(updatedMember.getPoint()).isEqualTo(10_000_000 - 20000);
    }

    @Test
    @DisplayName("POST /api/orders - 메시지 없이 주문할 수 있다")
    void createOrderWithoutMessage() throws Exception {
        var request = new OrderRequest(option.getId(), 1, null);

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/orders - 인증 없이 주문하면 401을 반환한다")
    void createOrderWithoutAuth() throws Exception {
        var request = new OrderRequest(option.getId(), 1, null);

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/orders - 존재하지 않는 옵션으로 주문하면 404를 반환한다")
    void createOrderWithNonExistentOption() throws Exception {
        var request = new OrderRequest(999999L, 1, null);

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/orders - 재고보다 많은 수량을 주문하면 400을 반환한다")
    void createOrderExceedingStock() throws Exception {
        var request = new OrderRequest(option.getId(), 101, null);

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/orders - 포인트가 부족하면 400을 반환한다")
    void createOrderInsufficientPoints() throws Exception {
        // 포인트가 적은 회원 생성
        var poorMember = new Member("poor@example.com", "password");
        poorMember.chargePoint(100);  // 100원만 보유
        poorMember = memberRepository.save(poorMember);
        var poorToken = jwtProvider.createToken(poorMember.getEmail());

        var request = new OrderRequest(option.getId(), 1, null);  // 10000원짜리 상품

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + poorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/orders - 포인트 부족 시 주문 실패하면 재고가 원래대로 유지된다")
    void createOrderInsufficientPoints_stockRemainsUnchanged() throws Exception {
        var poorMember = new Member("poor-stock@example.com", "password");
        poorMember.chargePoint(100);
        poorMember = memberRepository.save(poorMember);
        var poorToken = jwtProvider.createToken(poorMember.getEmail());

        int originalStock = option.getQuantity();

        var request = new OrderRequest(option.getId(), 1, null);

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + poorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        entityManager.clear();

        var updatedOption = optionRepository.findById(option.getId()).orElseThrow();
        assertThat(updatedOption.getQuantity()).isEqualTo(originalStock);
    }

    @Test
    @DisplayName("POST /api/orders - 위시리스트에 있는 상품을 주문하면 위시리스트에서 자동 제거된다")
    void createOrder_removesWish() throws Exception {
        wishRepository.save(new Wish(member.getId(), option.getProduct()));
        assertThat(wishRepository.findByMemberIdAndProductId(member.getId(), option.getProduct().getId())).isPresent();

        var request = new OrderRequest(option.getId(), 1, "위시 정리 테스트");

        mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        entityManager.flush();
        entityManager.clear();

        assertThat(wishRepository.findByMemberIdAndProductId(member.getId(), option.getProduct().getId())).isEmpty();
    }

    @Test
    @DisplayName("GET /api/orders - 내 주문 목록을 조회한다")
    void getOrders() throws Exception {
        orderRepository.save(new Order(option, member.getId(), 1, "테스트 주문"));

        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer " + token)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/orders - 인증 없이 조회하면 401을 반환한다")
    void getOrdersWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/orders")
                .header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized());
    }
}
