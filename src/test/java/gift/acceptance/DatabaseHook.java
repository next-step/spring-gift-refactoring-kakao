package gift.acceptance;

import io.cucumber.java.Before;
import org.flywaydb.core.Flyway;

/**
 * 매 시나리오 전에 DB를 초기 데이터(V1, V2)로 되돌린다.
 * 테이블을 다시 만들어서 자동 증가 번호도 처음부터 시작한다.
 * 이유는 docs/work/02-acceptance-test/test-strategy.md에 있다.
 */
public class DatabaseHook {

    private final Flyway flyway;

    public DatabaseHook(Flyway flyway) {
        this.flyway = flyway;
    }

    @Before(order = 1)
    public void resetToInitialData() {
        flyway.clean();
        flyway.migrate();
    }
}
