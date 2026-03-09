package gift;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.OptionRepository;
import gift.order.OrderRepository;
import gift.product.ProductRepository;
import gift.wish.WishRepository;
import io.restassured.RestAssured;
import io.restassured.filter.session.SessionFilter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminAuthAcceptanceTest {

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

    // ── 인터셉터: 미인증 접근 차단 ──

    @Test
    void 미인증_상태로_Admin_페이지_접근시_로그인으로_리다이렉트() {
        given()
            .redirects().follow(false)
        .when()
            .get("/admin/products")
        .then()
            .statusCode(302)
            .header("Location", containsString("/admin/login"));
    }

    @Test
    void 로그인_페이지는_인터셉터_제외() {
        given()
        .when()
            .get("/admin/login")
        .then()
            .statusCode(200);
    }

    // ── 로그인 ──

    @Test
    void Admin_로그인_성공_후_Admin_페이지_접근_가능() {
        // given
        createAdminMember("admin@example.com", "admin1234");
        SessionFilter sessionFilter = new SessionFilter();

        // when — 로그인
        given()
            .filter(sessionFilter)
            .formParam("email", "admin@example.com")
            .formParam("password", "admin1234")
            .redirects().follow(false)
        .when()
            .post("/admin/login")
        .then()
            .statusCode(302)
            .header("Location", containsString("/admin/products"));

        // then — 세션으로 Admin 페이지 접근
        given()
            .filter(sessionFilter)
        .when()
            .get("/admin/products")
        .then()
            .statusCode(200);
    }

    @Test
    void 일반_유저로_Admin_로그인_시도시_거부() {
        // given
        memberRepository.save(new Member("user@example.com", "password123"));

        // when
        given()
            .formParam("email", "user@example.com")
            .formParam("password", "password123")
        .when()
            .post("/admin/login")
        .then()
            .statusCode(200)
            .body(containsString("관리자 계정으로 로그인해주세요"));
    }

    @Test
    void 잘못된_비밀번호로_Admin_로그인_시도시_거부() {
        // given
        createAdminMember("admin@example.com", "admin1234");

        // when
        given()
            .formParam("email", "admin@example.com")
            .formParam("password", "wrong-password")
        .when()
            .post("/admin/login")
        .then()
            .statusCode(200)
            .body(containsString("관리자 계정으로 로그인해주세요"));
    }

    @Test
    void 존재하지_않는_이메일로_Admin_로그인_시도시_거부() {
        given()
            .formParam("email", "nobody@example.com")
            .formParam("password", "password123")
        .when()
            .post("/admin/login")
        .then()
            .statusCode(200)
            .body(containsString("관리자 계정으로 로그인해주세요"));
    }

    // ── 로그아웃 ──

    @Test
    void 로그아웃_후_Admin_페이지_접근_불가() {
        // given — 로그인
        createAdminMember("admin@example.com", "admin1234");
        SessionFilter sessionFilter = new SessionFilter();

        given()
            .filter(sessionFilter)
            .formParam("email", "admin@example.com")
            .formParam("password", "admin1234")
            .redirects().follow(false)
        .when()
            .post("/admin/login");

        // when — 로그아웃
        given()
            .filter(sessionFilter)
            .redirects().follow(false)
        .when()
            .post("/admin/logout")
        .then()
            .statusCode(302)
            .header("Location", containsString("/admin/login"));

        // then — 세션 무효화되어 접근 불가
        given()
            .filter(sessionFilter)
            .redirects().follow(false)
        .when()
            .get("/admin/products")
        .then()
            .statusCode(302)
            .header("Location", containsString("/admin/login"));
    }

    private void createAdminMember(String email, String password) {
        // Member를 저장한 뒤 SQL로 role을 ADMIN으로 변경
        Member member = memberRepository.save(new Member(email, password));
        memberRepository.flush();
        // V3 마이그레이션이 기본값 USER로 설정하므로, 직접 업데이트
        memberRepository.findByEmail(email).ifPresent(m -> {
            // role 필드를 직접 설정하기 위해 native query 대신 리플렉션 사용
            try {
                var roleField = Member.class.getDeclaredField("role");
                roleField.setAccessible(true);
                roleField.set(m, gift.member.Role.ADMIN);
                memberRepository.save(m);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
