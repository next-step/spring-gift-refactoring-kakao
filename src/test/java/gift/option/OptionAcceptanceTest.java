package gift.option;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.MemberRepository;
import gift.order.OrderRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import gift.wish.WishRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OptionAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Category category;
    private Product product;

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

        category = categoryRepository.save(new Category("카테고리", "#000", "http://img.com", "설명"));
        product = productRepository.save(new Product("테스트 상품", 10000, "http://img.com", category));
    }

    @Nested
    @DisplayName("옵션 목록 조회")
    class GetOptions {

        @Test
        @DisplayName("성공: 상품의 옵션 목록을 반환한다")
        void success() {
            // Given
            optionRepository.save(new Option(product, "옵션1", 100));
            optionRepository.save(new Option(product, "옵션2", 200));

            // When & Then
            RestAssured.given()
                .when()
                .get("/api/products/" + product.getId() + "/options")
                .then()
                .statusCode(200)
                .body("$", hasSize(2));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 상품의 옵션을 조회하면 404를 반환한다")
        void fail_productNotFound() {
            RestAssured.given()
                .when()
                .get("/api/products/999999/options")
                .then()
                .statusCode(404);
        }
    }

    @Nested
    @DisplayName("옵션 생성")
    class CreateOption {

        @Test
        @DisplayName("성공: 유효한 옵션을 생성한다")
        void success() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "새 옵션",
                        "quantity": 100
                    }
                    """)
                .when()
                .post("/api/products/" + product.getId() + "/options")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("새 옵션"))
                .body("quantity", equalTo(100));
        }

        @Test
        @DisplayName("성공: 허용된 특수문자가 포함된 이름으로 옵션을 생성한다")
        void success_withAllowedSpecialChars() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "옵션 (A+B)/C_D",
                        "quantity": 50
                    }
                    """)
                .when()
                .post("/api/products/" + product.getId() + "/options")
                .then()
                .statusCode(201);
        }

        @Test
        @DisplayName("성공: 정확히 50자인 이름으로 옵션을 생성한다")
        void success_maxLength() {
            String fiftyChars = "가".repeat(50);
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "%s",
                        "quantity": 50
                    }
                    """.formatted(fiftyChars))
                .when()
                .post("/api/products/" + product.getId() + "/options")
                .then()
                .statusCode(201);
        }

        @Test
        @DisplayName("실패: 이름이 51자 이상이면 400을 반환한다")
        void fail_nameTooLong() {
            String fiftyOneChars = "가".repeat(51);
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "%s",
                        "quantity": 50
                    }
                    """.formatted(fiftyOneChars))
                .when()
                .post("/api/products/" + product.getId() + "/options")
                .then()
                .statusCode(400)
                .body(containsString("최대 50자"));
        }

        @Test
        @DisplayName("실패: 허용되지 않은 특수문자가 포함되면 400을 반환한다")
        void fail_invalidSpecialChars() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "옵션@#$%",
                        "quantity": 50
                    }
                    """)
                .when()
                .post("/api/products/" + product.getId() + "/options")
                .then()
                .statusCode(400)
                .body(containsString("허용되지 않는 특수 문자"));
        }

        @Test
        @DisplayName("실패: 동일 상품에 중복된 옵션명이면 400을 반환한다")
        void fail_duplicateName() {
            // Given: 이미 존재하는 옵션
            optionRepository.save(new Option(product, "기존 옵션", 100));

            // When & Then
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "기존 옵션",
                        "quantity": 50
                    }
                    """)
                .when()
                .post("/api/products/" + product.getId() + "/options")
                .then()
                .statusCode(400)
                .body(containsString("이미 존재하는 옵션명"));
        }

        @Test
        @DisplayName("실패: 이름이 비어있으면 400을 반환한다")
        void fail_emptyName() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "",
                        "quantity": 50
                    }
                    """)
                .when()
                .post("/api/products/" + product.getId() + "/options")
                .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("실패: 수량이 0이면 400을 반환한다")
        void fail_zeroQuantity() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "옵션",
                        "quantity": 0
                    }
                    """)
                .when()
                .post("/api/products/" + product.getId() + "/options")
                .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 상품에 옵션을 생성하면 404를 반환한다")
        void fail_productNotFound() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "옵션",
                        "quantity": 50
                    }
                    """)
                .when()
                .post("/api/products/999999/options")
                .then()
                .statusCode(404);
        }
    }

    @Nested
    @DisplayName("옵션 삭제")
    class DeleteOption {

        @Test
        @DisplayName("성공: 옵션이 2개 이상이면 삭제할 수 있다")
        void success_multipleOptions() {
            // Given: 2개의 옵션
            Option option1 = optionRepository.save(new Option(product, "옵션1", 100));
            optionRepository.save(new Option(product, "옵션2", 200));

            // When & Then
            RestAssured.given()
                .when()
                .delete("/api/products/" + product.getId() + "/options/" + option1.getId())
                .then()
                .statusCode(204);
        }

        @Test
        @DisplayName("실패: 옵션이 1개인 상품은 옵션을 삭제할 수 없다")
        void fail_lastOption() {
            // Given: 1개의 옵션만 존재
            Option onlyOption = optionRepository.save(new Option(product, "유일한 옵션", 100));

            // When & Then
            RestAssured.given()
                .when()
                .delete("/api/products/" + product.getId() + "/options/" + onlyOption.getId())
                .then()
                .statusCode(400)
                .body(containsString("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다"));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 옵션을 삭제하면 404를 반환한다")
        void fail_optionNotFound() {
            // Given: 옵션 2개 이상 필요 (옵션 수 체크를 통과하기 위해)
            optionRepository.save(new Option(product, "옵션1", 100));
            optionRepository.save(new Option(product, "옵션2", 100));

            // When & Then
            RestAssured.given()
                .when()
                .delete("/api/products/" + product.getId() + "/options/999999")
                .then()
                .statusCode(404);
        }

        @Test
        @DisplayName("실패: 다른 상품의 옵션을 삭제하면 404를 반환한다")
        void fail_wrongProduct() {
            // Given: 다른 상품의 옵션
            Product otherProduct = productRepository.save(new Product("다른 상품", 5000, "http://img.com", category));
            Option otherOption = optionRepository.save(new Option(otherProduct, "다른 옵션", 100));
            // product에 옵션 2개 추가 (옵션 수 체크를 통과하기 위해)
            optionRepository.save(new Option(product, "옵션1", 100));
            optionRepository.save(new Option(product, "옵션2", 100));

            // When & Then: product의 옵션으로 삭제 시도
            RestAssured.given()
                .when()
                .delete("/api/products/" + product.getId() + "/options/" + otherOption.getId())
                .then()
                .statusCode(404);
        }
    }
}
