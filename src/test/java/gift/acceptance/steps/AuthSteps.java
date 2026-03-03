package gift.acceptance.steps;

import gift.acceptance.client.ApiClient;
import gift.acceptance.context.ContextKeys;
import gift.acceptance.context.ScenarioContext;
import gift.auth.internal.JwtProvider;
import gift.member.Member;
import gift.support.DataManipulator;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthSteps {

    private static final int DEFAULT_POINT = 1_000_000_000;

    private final DataManipulator dataManipulator;
    private final JwtProvider jwtProvider;
    private final ScenarioContext context;
    private final ApiClient apiClient;

    @Given("회원이 존재하고 유효한 토큰을 가진다")
    public void memberWithToken() {
        createMemberAndSetToken(DEFAULT_POINT);
    }

    private void createMemberAndSetToken(int point) {
        String email = "test-" + System.nanoTime() + "@example.com";

        Member member = dataManipulator.addMember(email, "password", null, point);
        String token = jwtProvider.createToken(email);

        context.setAuthToken(token);
        context.saveId(ContextKeys.CURRENT_MEMBER, member.getId());
    }

    @Given("포인트가 충분한 회원이 존재하고 유효한 토큰을 가진다")
    public void memberWithTokenAndSufficientPoints() {
        createMemberAndSetToken(DEFAULT_POINT);
    }

    @Given("포인트가 {int}인 회원이 존재하고 유효한 토큰을 가진다")
    public void memberWithSpecificPoints(int point) {
        createMemberAndSetToken(point);
    }

    @When("인증이 필요한 API에 해당 토큰으로 요청한다")
    public void requestAuthenticatedApi() {
        apiClient.authGet("/api/wishes");
    }
}
