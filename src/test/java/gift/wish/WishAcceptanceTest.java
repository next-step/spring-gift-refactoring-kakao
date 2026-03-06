package gift.wish;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.OptionRepository;
import gift.order.OrderRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WishAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OptionRepository optionRepository;

    private Member member;
    private Product product;
    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        // FK 순서대로 삭제
        orderRepository.deleteAll();
        wishRepository.deleteAll();
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();

        // 회원 및 상품 준비
        member = memberRepository.save(new Member("test@example.com", "password"));
        Category category = categoryRepository.save(new Category("카테고리", "#000", "http://img.com", "설명"));
        product = productRepository.save(new Product("테스트 상품", 10000, "http://img.com", category));

        // 토큰 획득
        token = getToken("test@example.com", "password");
    }

    private String getToken(String email, String password) {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password))
            .when()
            .post("/api/members/login")
            .then()
            .extract()
            .path("token");
    }

    @Nested
    @DisplayName("위시 목록 조회")
    class GetWishes {

        @Test
        @DisplayName("성공: 인증된 사용자의 위시 목록을 반환한다")
        void success() {
            // Given
            wishRepository.save(new Wish(member.getId(), product));

            // When & Then
            RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .param("page", 0)
                .param("size", 10)
                .when()
                .get("/api/wishes")
                .then()
                .statusCode(200)
                .body("content", hasSize(1));
        }

        @Test
        @DisplayName("성공: 다른 사용자의 위시는 조회되지 않는다")
        void success_onlyOwnWishes() {
            // Given: 다른 회원의 위시
            Member otherMember = memberRepository.save(new Member("other@example.com", "password"));
            wishRepository.save(new Wish(otherMember.getId(), product));

            // When & Then: 본인의 위시만 조회
            RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .param("page", 0)
                .param("size", 10)
                .when()
                .get("/api/wishes")
                .then()
                .statusCode(200)
                .body("content", hasSize(0));
        }

        @Test
        @DisplayName("실패: 인증 헤더 없이 조회하면 401을 반환한다")
        void fail_noAuthHeader() {
            RestAssured.given()
                .param("page", 0)
                .param("size", 10)
                .when()
                .get("/api/wishes")
                .then()
                .statusCode(401);
        }

        @Test
        @DisplayName("실패: 잘못된 토큰으로 조회하면 401을 반환한다")
        void fail_invalidToken() {
            RestAssured.given()
                .header("Authorization", "Bearer invalid-token")
                .param("page", 0)
                .param("size", 10)
                .when()
                .get("/api/wishes")
                .then()
                .statusCode(401);
        }
    }

    @Nested
    @DisplayName("위시 추가")
    class AddWish {

        @Test
        @DisplayName("성공: 새 상품을 위시리스트에 추가한다")
        void success() {
            RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "productId": %d
                    }
                    """.formatted(product.getId()))
                .when()
                .post("/api/wishes")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("productId", equalTo(product.getId().intValue()));
        }

        @Test
        @DisplayName("성공: 이미 추가된 상품을 다시 추가하면 기존 위시를 반환한다")
        void success_duplicateReturnsExisting() {
            // Given: 이미 위시에 추가된 상품
            Wish existing = wishRepository.save(new Wish(member.getId(), product));

            // When & Then: 다시 추가해도 기존 위시 반환 (200)
            RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "productId": %d
                    }
                    """.formatted(product.getId()))
                .when()
                .post("/api/wishes")
                .then()
                .statusCode(200)
                .body("id", equalTo(existing.getId().intValue()));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 상품을 추가하면 404를 반환한다")
        void fail_productNotFound() {
            RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "productId": 999999
                    }
                    """)
                .when()
                .post("/api/wishes")
                .then()
                .statusCode(404);
        }

        @Test
        @DisplayName("실패: 인증 헤더 없이 추가하면 401을 반환한다")
        void fail_noAuthHeader() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "productId": %d
                    }
                    """.formatted(product.getId()))
                .when()
                .post("/api/wishes")
                .then()
                .statusCode(401);
        }
    }

    @Nested
    @DisplayName("위시 삭제")
    class RemoveWish {

        @Test
        @DisplayName("성공: 본인의 위시를 삭제한다")
        void success() {
            // Given
            Wish wish = wishRepository.save(new Wish(member.getId(), product));

            // When & Then
            RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/wishes/" + wish.getId())
                .then()
                .statusCode(204);
        }

        @Test
        @DisplayName("실패: 다른 사용자의 위시를 삭제하면 403을 반환한다")
        void fail_forbidden() {
            // Given: 다른 회원의 위시
            Member otherMember = memberRepository.save(new Member("other@example.com", "password"));
            Wish otherWish = wishRepository.save(new Wish(otherMember.getId(), product));

            // When & Then
            RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/wishes/" + otherWish.getId())
                .then()
                .statusCode(403);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 위시를 삭제하면 404를 반환한다")
        void fail_notFound() {
            RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/wishes/999999")
                .then()
                .statusCode(404);
        }

        @Test
        @DisplayName("실패: 인증 헤더 없이 삭제하면 401을 반환한다")
        void fail_noAuthHeader() {
            // Given
            Wish wish = wishRepository.save(new Wish(member.getId(), product));

            // When & Then
            RestAssured.given()
                .when()
                .delete("/api/wishes/" + wish.getId())
                .then()
                .statusCode(401);
        }
    }
}
