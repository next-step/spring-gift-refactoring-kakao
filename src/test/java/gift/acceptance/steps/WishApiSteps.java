package gift.acceptance.steps;

import static gift.acceptance.context.ScenarioContext.NON_EXISTENT_ID;

import gift.acceptance.client.ApiClient;
import gift.acceptance.context.ContextKeys;
import gift.acceptance.context.ScenarioContext;
import gift.auth.JwtProvider;
import gift.wish.WishRequest;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WishApiSteps {

    private static final String
            BASE_URI = "/api/wishes";

    private final ScenarioContext context;
    private final JwtProvider jwtProvider;
    private final ApiClient apiClient;

    @When("인증된 사용자가 위시 추가 요청을 보낸다")
    public void addWish(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().getFirst();

        String productId = row.get("productId");

        WishRequest createRequest = new WishRequest(context.resolvePlaceHolderId(
                productId, ContextKeys.CURRENT_PRODUCT
        ));

        apiClient.authPost(BASE_URI, createRequest);
    }

    @When("인증된 사용자가 위시 목록을 페이지 {int} 사이즈 {int}로 조회 요청을 보낸다")
    public void getWishesPaginated(int page, int size) {
        apiClient.authGet(BASE_URI, page, size);
    }

    @When("인증된 사용자가 해당 위시 삭제 요청을 보낸다")
    public void deleteWish() {
        Long wishId = context.currentWishId();

        String uri = BASE_URI + "/" + wishId;

        apiClient.authDelete(uri);
    }

    @When("인증된 사용자가 존재하지 않는 위시 삭제 요청을 보낸다")
    public void deleteWishNotFound() {
        String uri = BASE_URI + "/" + NON_EXISTENT_ID;

        apiClient.authDelete(uri);
    }

    // --- Invalid token scenarios ---

    @When("유효하지 않은 토큰으로 위시 목록 조회 요청을 보낸다")
    public void getWishesInvalidToken() {
        apiClient.tokenGet("invalid-token", BASE_URI);
    }

    @When("유효하지 않은 토큰으로 위시 추가 요청을 보낸다")
    public void addWishInvalidToken(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().getFirst();

        String productId = row.get("productId");

        WishRequest createRequest = new WishRequest(
                Long.parseLong(productId)
        );

        apiClient.tokenPost("invalid-token", BASE_URI, createRequest);
    }

    @When("유효하지 않은 토큰으로 위시 삭제 요청을 보낸다")
    public void deleteWishInvalidToken() {
        apiClient.tokenDelete("invalid-token", "/api/wishes/1");
    }

    // --- W-E5: Cross-member delete ---

    @Given("회원B의 토큰이 준비된다")
    public void prepareMemberBToken() {
        String token = jwtProvider.createToken("memberB@test.com");
        context.setSecondAuthToken(token);
    }

    @When("회원B가 회원A의 위시 삭제 요청을 보낸다")
    public void deleteMemberAWishWithMemberBToken() {
        Long wishId = context.getId(ContextKeys.MEMBER_A_WISH);
        String secondAuthToken = context.getSecondAuthToken();

        String uri = BASE_URI + "/" + wishId;

        apiClient.tokenDelete(secondAuthToken, uri);
    }

}
