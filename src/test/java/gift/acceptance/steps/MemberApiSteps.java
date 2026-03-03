package gift.acceptance.steps;

import gift.acceptance.client.ApiClient;
import gift.member.internal.MemberRequest;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MemberApiSteps {

    private static final String BASE_URL = "/api/members";

    private final ApiClient apiClient;

    @When("회원가입 요청을 보낸다")
    public void registerMember(DataTable dataTable) {
        MemberRequest registerRequest = toMemberRequest(dataTable);

        apiClient.post(
                BASE_URL + "/register",
                registerRequest
        );
    }

    private MemberRequest toMemberRequest(DataTable dataTable) {
        Map<String, String> row = dataTable.asMaps().getFirst();

        String email = row.get("email");
        String password = row.get("password");

        return new MemberRequest(
                nullToEmpty(email),
                nullToEmpty(password)
        );
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    @When("로그인 요청을 보낸다")
    public void loginMember(DataTable dataTable) {
        MemberRequest loginRequest = toMemberRequest(dataTable);

        apiClient.post(
                BASE_URL + "/login",
                loginRequest
        );
    }
}
