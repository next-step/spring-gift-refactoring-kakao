package gift.acceptance.steps;

import gift.acceptance.FailableWishCleaner;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;

/**
 * 주문 뒤 위시 정리 단계 정의(docs/work/08-wish-cleanup/tech-spec.md).
 * 위시가 남았는지는 위시 목록 API로 확인한다(WishSteps의 "위시 번호는 …다").
 */
public class WishCleanupSteps {

    private final FailableWishCleaner wishCleaner;

    public WishCleanupSteps(FailableWishCleaner wishCleaner) {
        this.wishCleaner = wishCleaner;
    }

    @Before
    public void resetWishCleaner() {
        wishCleaner.reset();
    }

    @Given("^위시 정리가 실패한다$")
    public void failNextCleanup() {
        wishCleaner.failNext();
    }
}
