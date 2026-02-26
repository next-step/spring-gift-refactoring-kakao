package gift.member;

import gift.category.CategoryRepository;
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
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberAcceptanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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
    @DisplayName("회원 가입")
    class Register {

        @Test
        @DisplayName("성공: 새로운 이메일로 가입하면 토큰을 반환한다")
        void success() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "email": "test@example.com",
                        "password": "password123"
                    }
                    """)
                .when()
                .post("/api/members/register")
                .then()
                .statusCode(201)
                .body("token", notNullValue());
        }

        @Test
        @DisplayName("실패: 이미 등록된 이메일로 가입하면 400을 반환한다")
        void fail_duplicateEmail() {
            // Given: 이미 등록된 회원
            memberRepository.save(new Member("existing@example.com", "password"));

            // When & Then
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "email": "existing@example.com",
                        "password": "newpassword"
                    }
                    """)
                .when()
                .post("/api/members/register")
                .then()
                .statusCode(400)
                .body(equalTo("Email is already registered."));
        }

        @Test
        @DisplayName("실패: 이메일 형식이 올바르지 않으면 400을 반환한다")
        void fail_invalidEmailFormat() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "email": "invalid-email",
                        "password": "password123"
                    }
                    """)
                .when()
                .post("/api/members/register")
                .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("실패: 이메일이 비어있으면 400을 반환한다")
        void fail_emptyEmail() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "email": "",
                        "password": "password123"
                    }
                    """)
                .when()
                .post("/api/members/register")
                .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("실패: 비밀번호가 비어있으면 400을 반환한다")
        void fail_emptyPassword() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "email": "test@example.com",
                        "password": ""
                    }
                    """)
                .when()
                .post("/api/members/register")
                .then()
                .statusCode(400);
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("성공: 올바른 이메일과 비밀번호로 로그인하면 토큰을 반환한다")
        void success() {
            // Given: 등록된 회원
            memberRepository.save(new Member("test@example.com", "password123"));

            // When & Then
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "email": "test@example.com",
                        "password": "password123"
                    }
                    """)
                .when()
                .post("/api/members/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 이메일로 로그인하면 400을 반환한다")
        void fail_emailNotFound() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "email": "notfound@example.com",
                        "password": "password123"
                    }
                    """)
                .when()
                .post("/api/members/login")
                .then()
                .statusCode(400)
                .body(equalTo("Invalid email or password."));
        }

        @Test
        @DisplayName("실패: 비밀번호가 틀리면 400을 반환한다")
        void fail_wrongPassword() {
            // Given: 등록된 회원
            memberRepository.save(new Member("test@example.com", "correctPassword"));

            // When & Then
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "email": "test@example.com",
                        "password": "wrongPassword"
                    }
                    """)
                .when()
                .post("/api/members/login")
                .then()
                .statusCode(400)
                .body(equalTo("Invalid email or password."));
        }
    }
}
