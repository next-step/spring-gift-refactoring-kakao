package gift.acceptance;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import java.util.Map;

/**
 * 시나리오에서 쓰는 회원 별칭(user1, user2)과 초기 데이터의 계정을 잇는다.
 */
public final class Members {

    private record Account(String email, String password) {}

    private static final Map<String, Account> ACCOUNTS = Map.of(
            "user1", new Account("user1@example.com", "password1"),
            "user2", new Account("user2@example.com", "password2"));

    private Members() {}

    public static String email(String alias) {
        return account(alias).email();
    }

    static String login(String alias) {
        Account account = account(alias);
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", account.email(), "password", account.password()))
                .when()
                .post("/api/members/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    private static Account account(String alias) {
        Account account = ACCOUNTS.get(alias);
        if (account == null) {
            throw new IllegalArgumentException("모르는 회원 별칭: " + alias);
        }
        return account;
    }
}
