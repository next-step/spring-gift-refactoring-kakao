package gift;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import gift.category.CategoryRepository;
import gift.member.MemberRepository;
import gift.option.OptionRepository;
import gift.order.OrderRepository;
import gift.product.ProductRepository;
import gift.wish.WishRepository;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KakaoAuthAcceptanceTest {

    @LocalServerPort
    int port;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    WishRepository wishRepository;

    @Autowired
    OptionRepository optionRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        orderRepository.deleteAll();
        wishRepository.deleteAll();
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    // ── 카카오 로그인 리다이렉트 ──

    @Test
    void 카카오_로그인_리다이렉트_성공() {
        // given — 별도 설정 불필요

        // when
        var response = given()
            .redirects().follow(false)
        .when()
            .get("/api/auth/kakao/login");

        // then
        response.then()
            .statusCode(302)
            .header("Location", containsString("https://kauth.kakao.com/oauth/authorize"))
            .header("Location", containsString("response_type=code"))
            .header("Location", containsString("client_id=test"))
            .header("Location", containsString("scope=account_email,talk_message"));
    }

    // ── 카카오 콜백 ──

    @Test
    void 카카오_콜백_실패_잘못된_인가코드() {
        // given — 유효하지 않은 인가코드

        // when
        var response = given()
            .queryParam("code", "invalid-code")
        .when()
            .get("/api/auth/kakao/callback");

        // then — 외부 카카오 API 호출 실패로 500 반환
        response.then()
            .statusCode(500);
    }

    @Test
    void 카카오_콜백_실패_인가코드_누락() {
        // given — code 파라미터 없음

        // when
        var response = given()
        .when()
            .get("/api/auth/kakao/callback");

        // then
        response.then()
            .statusCode(400);
    }
}
