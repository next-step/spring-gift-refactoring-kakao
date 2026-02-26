package gift.acceptance;

import io.cucumber.spring.ScenarioScope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class AcceptanceTestContext {
    private ResponseEntity<String> response;
    private String token;

    public ResponseEntity<String> getResponse() {
        return response;
    }

    public void setResponse(ResponseEntity<String> response) {
        this.response = response;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
