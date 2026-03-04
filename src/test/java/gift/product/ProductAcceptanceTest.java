package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.MemberRepository;
import gift.option.OptionRepository;
import gift.order.OrderRepository;
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
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductAcceptanceTest {

    @LocalServerPort
    private int port;

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
    private MemberRepository memberRepository;

    private Category category;

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

        category = categoryRepository.save(new Category("테스트 카테고리", "#000", "http://img.com", "설명"));
    }

    @Nested
    @DisplayName("상품 목록 조회")
    class GetProducts {

        @Test
        @DisplayName("성공: 페이징된 상품 목록을 반환한다")
        void success() {
            // Given
            productRepository.save(new Product("상품1", 1000, "http://img1.com", category));
            productRepository.save(new Product("상품2", 2000, "http://img2.com", category));

            // When & Then
            RestAssured.given()
                .param("page", 0)
                .param("size", 10)
                .when()
                .get("/api/products")
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(2));
        }
    }

    @Nested
    @DisplayName("상품 단건 조회")
    class GetProduct {

        @Test
        @DisplayName("성공: 존재하는 상품을 조회한다")
        void success() {
            // Given
            Product product = productRepository.save(new Product("테스트 상품", 5000, "http://img.com", category));

            // When & Then
            RestAssured.given()
                .when()
                .get("/api/products/" + product.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("테스트 상품"))
                .body("price", equalTo(5000));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 상품을 조회하면 404를 반환한다")
        void fail_notFound() {
            RestAssured.given()
                .when()
                .get("/api/products/999999")
                .then()
                .statusCode(404);
        }
    }

    @Nested
    @DisplayName("상품 생성")
    class CreateProduct {

        @Test
        @DisplayName("성공: 유효한 이름으로 상품을 생성한다")
        void success() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "새 상품",
                        "price": 10000,
                        "imageUrl": "http://img.com/product.png",
                        "categoryId": %d
                    }
                    """.formatted(category.getId()))
                .when()
                .post("/api/products")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("새 상품"));
        }

        @Test
        @DisplayName("성공: 허용된 특수문자가 포함된 이름으로 상품을 생성한다")
        void success_withAllowedSpecialChars() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "상품 (A+B)/C_D",
                        "price": 10000,
                        "imageUrl": "http://img.com/product.png",
                        "categoryId": %d
                    }
                    """.formatted(category.getId()))
                .when()
                .post("/api/products")
                .then()
                .statusCode(201);
        }

        @Test
        @DisplayName("성공: 정확히 15자인 이름으로 상품을 생성한다")
        void success_maxLength() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "123456789012345",
                        "price": 10000,
                        "imageUrl": "http://img.com/product.png",
                        "categoryId": %d
                    }
                    """.formatted(category.getId()))
                .when()
                .post("/api/products")
                .then()
                .statusCode(201);
        }

        @Test
        @DisplayName("실패: 이름이 16자 이상이면 400을 반환한다")
        void fail_nameTooLong() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "1234567890123456",
                        "price": 10000,
                        "imageUrl": "http://img.com/product.png",
                        "categoryId": %d
                    }
                    """.formatted(category.getId()))
                .when()
                .post("/api/products")
                .then()
                .statusCode(400)
                .body(containsString("최대 15자"));
        }

        @Test
        @DisplayName("실패: 허용되지 않은 특수문자가 포함되면 400을 반환한다")
        void fail_invalidSpecialChars() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "상품@#$",
                        "price": 10000,
                        "imageUrl": "http://img.com/product.png",
                        "categoryId": %d
                    }
                    """.formatted(category.getId()))
                .when()
                .post("/api/products")
                .then()
                .statusCode(400)
                .body(containsString("허용되지 않는 특수 문자"));
        }

        @Test
        @DisplayName("실패: '카카오'가 포함된 이름은 400을 반환한다")
        void fail_containsKakao() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "카카오 상품",
                        "price": 10000,
                        "imageUrl": "http://img.com/product.png",
                        "categoryId": %d
                    }
                    """.formatted(category.getId()))
                .when()
                .post("/api/products")
                .then()
                .statusCode(400)
                .body(containsString("카카오"));
        }

        @Test
        @DisplayName("실패: 이름이 비어있으면 400을 반환한다")
        void fail_emptyName() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "",
                        "price": 10000,
                        "imageUrl": "http://img.com/product.png",
                        "categoryId": %d
                    }
                    """.formatted(category.getId()))
                .when()
                .post("/api/products")
                .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 카테고리로 생성하면 404를 반환한다")
        void fail_categoryNotFound() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "상품",
                        "price": 10000,
                        "imageUrl": "http://img.com/product.png",
                        "categoryId": 999999
                    }
                    """)
                .when()
                .post("/api/products")
                .then()
                .statusCode(404);
        }

        @Test
        @DisplayName("실패: 가격이 0이면 400을 반환한다")
        void fail_zeroPrice() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "상품",
                        "price": 0,
                        "imageUrl": "http://img.com/product.png",
                        "categoryId": %d
                    }
                    """.formatted(category.getId()))
                .when()
                .post("/api/products")
                .then()
                .statusCode(400);
        }
    }

    @Nested
    @DisplayName("상품 수정")
    class UpdateProduct {

        @Test
        @DisplayName("성공: 존재하는 상품을 수정한다")
        void success() {
            // Given
            Product product = productRepository.save(new Product("기존 상품", 5000, "http://old.com", category));

            // When & Then
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "수정된 상품",
                        "price": 7000,
                        "imageUrl": "http://new.com",
                        "categoryId": %d
                    }
                    """.formatted(category.getId()))
                .when()
                .put("/api/products/" + product.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("수정된 상품"))
                .body("price", equalTo(7000));
        }

        @Test
        @DisplayName("실패: 수정 시에도 이름 검증이 적용된다")
        void fail_invalidName() {
            // Given
            Product product = productRepository.save(new Product("기존 상품", 5000, "http://old.com", category));

            // When & Then
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "카카오 상품",
                        "price": 7000,
                        "imageUrl": "http://new.com",
                        "categoryId": %d
                    }
                    """.formatted(category.getId()))
                .when()
                .put("/api/products/" + product.getId())
                .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 상품을 수정하면 404를 반환한다")
        void fail_notFound() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "상품",
                        "price": 7000,
                        "imageUrl": "http://new.com",
                        "categoryId": %d
                    }
                    """.formatted(category.getId()))
                .when()
                .put("/api/products/999999")
                .then()
                .statusCode(404);
        }
    }

    @Nested
    @DisplayName("상품 삭제")
    class DeleteProduct {

        @Test
        @DisplayName("성공: 존재하는 상품을 삭제한다")
        void success() {
            // Given
            Product product = productRepository.save(new Product("삭제할 상품", 5000, "http://img.com", category));

            // When & Then
            RestAssured.given()
                .when()
                .delete("/api/products/" + product.getId())
                .then()
                .statusCode(204);
        }
    }
}
