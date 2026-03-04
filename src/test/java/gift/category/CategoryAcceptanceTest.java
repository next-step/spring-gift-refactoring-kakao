package gift.category;

import gift.member.MemberRepository;
import gift.option.OptionRepository;
import gift.order.OrderRepository;
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

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
    }

    @Nested
    @DisplayName("카테고리 목록 조회")
    class GetCategories {

        @Test
        @DisplayName("성공: 카테고리가 없으면 빈 목록을 반환한다")
        void success_emptyList() {
            RestAssured.given()
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
        }

        @Test
        @DisplayName("성공: 저장된 카테고리 목록을 반환한다")
        void success_withCategories() {
            // Given
            categoryRepository.save(new Category("카테고리1", "#FF0000", "http://image1.com", "설명1"));
            categoryRepository.save(new Category("카테고리2", "#00FF00", "http://image2.com", "설명2"));

            // When & Then
            RestAssured.given()
                .when()
                .get("/api/categories")
                .then()
                .statusCode(200)
                .body("$", hasSize(2));
        }
    }

    @Nested
    @DisplayName("카테고리 생성")
    class CreateCategory {

        @Test
        @DisplayName("성공: 필수값을 모두 입력하면 카테고리를 생성한다")
        void success() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "신규 카테고리",
                        "color": "#0000FF",
                        "imageUrl": "http://image.com/category.png",
                        "description": "카테고리 설명"
                    }
                    """)
                .when()
                .post("/api/categories")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("신규 카테고리"))
                .body("color", equalTo("#0000FF"));
        }

        @Test
        @DisplayName("성공: description이 없어도 카테고리를 생성한다")
        void success_withoutDescription() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "카테고리",
                        "color": "#000000",
                        "imageUrl": "http://image.com/img.png"
                    }
                    """)
                .when()
                .post("/api/categories")
                .then()
                .statusCode(201)
                .body("name", equalTo("카테고리"));
        }

        @Test
        @DisplayName("실패: name이 비어있으면 400을 반환한다")
        void fail_emptyName() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "",
                        "color": "#000000",
                        "imageUrl": "http://image.com/img.png"
                    }
                    """)
                .when()
                .post("/api/categories")
                .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("실패: color가 비어있으면 400을 반환한다")
        void fail_emptyColor() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "카테고리",
                        "color": "",
                        "imageUrl": "http://image.com/img.png"
                    }
                    """)
                .when()
                .post("/api/categories")
                .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("실패: imageUrl이 비어있으면 400을 반환한다")
        void fail_emptyImageUrl() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "카테고리",
                        "color": "#000000",
                        "imageUrl": ""
                    }
                    """)
                .when()
                .post("/api/categories")
                .then()
                .statusCode(400);
        }
    }

    @Nested
    @DisplayName("카테고리 수정")
    class UpdateCategory {

        @Test
        @DisplayName("성공: 존재하는 카테고리를 수정한다")
        void success() {
            // Given
            Category category = categoryRepository.save(new Category("기존", "#000", "http://old.com", "기존 설명"));

            // When & Then
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "수정됨",
                        "color": "#FFF",
                        "imageUrl": "http://new.com",
                        "description": "수정된 설명"
                    }
                    """)
                .when()
                .put("/api/categories/" + category.getId())
                .then()
                .statusCode(200)
                .body("name", equalTo("수정됨"))
                .body("color", equalTo("#FFF"));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 카테고리를 수정하면 404를 반환한다")
        void fail_notFound() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "name": "수정됨",
                        "color": "#FFF",
                        "imageUrl": "http://new.com"
                    }
                    """)
                .when()
                .put("/api/categories/999999")
                .then()
                .statusCode(404);
        }
    }

    @Nested
    @DisplayName("카테고리 삭제")
    class DeleteCategory {

        @Test
        @DisplayName("성공: 존재하는 카테고리를 삭제한다")
        void success() {
            // Given
            Category category = categoryRepository.save(new Category("삭제할 카테고리", "#000", "http://img.com", "설명"));

            // When & Then
            RestAssured.given()
                .when()
                .delete("/api/categories/" + category.getId())
                .then()
                .statusCode(204);
        }

        @Test
        @DisplayName("성공: 존재하지 않는 카테고리를 삭제해도 204를 반환한다")
        void success_notFound() {
            RestAssured.given()
                .when()
                .delete("/api/categories/999999")
                .then()
                .statusCode(204);
        }
    }
}
