package gift.acceptance.context;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 시나리오 간 상태를 공유하는 컨텍스트 객체.
 * <p>
 * Cucumber {@code @ScenarioScope}로 시나리오마다 새 인스턴스가 생성된다. Given에서 데이터를 준비하면 ID를 저장하고, When에서 꺼내 API
 * 호출에 사용한다.
 */
@Component
@ScenarioScope
@Profile("acceptance-test")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScenarioContext {

    public static final long NON_EXISTENT_ID = 99999L;
    private final Map<String, Long> ids = new HashMap<>();
    @Setter
    @Getter
    private Response response;
    @Setter
    @Getter
    private String authToken;
    @Setter
    @Getter
    private String secondAuthToken;

    // --- Entity ID Storage ---

    public void saveId(String name, Long id) {
        ids.put(name, id);
    }

    public boolean doesNotHaveId(String name) {
        return !ids.containsKey(name);
    }

    public Long currentCategoryId() {
        return getId(ContextKeys.CURRENT_CATEGORY);
    }

    // --- Typed Accessors ---

    public Long getId(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        Long id = ids.get(name);
        if (id == null) {
            throw new AssertionError(
                    "ID not found for name: '" + name + "'. Available: " + ids.keySet());
        }

        return id;
    }

    public Long currentProductId() {
        return getId(ContextKeys.CURRENT_PRODUCT);
    }

    public Long currentOptionId() {
        return getId(ContextKeys.CURRENT_OPTION);
    }

    public Long currentMemberId() {
        return getId(ContextKeys.CURRENT_MEMBER);
    }

    public Long currentWishId() {
        return getId(ContextKeys.CURRENT_WISH);
    }

    /**
     * DataTable 플레이스홀더를 해석한다.
     * <p>
     * "{optionId}" 같은 플레이스홀더는 contextKey로 조회하고, 숫자 문자열은 그대로 파싱한다.
     */
    public Long resolvePlaceHolderId(String value, String contextKey) {
        if (value == null) {
            return null;
        }

        if (value.startsWith("{") && value.endsWith("}")) {
            return getId(contextKey);
        }

        return Long.parseLong(value);
    }
}
