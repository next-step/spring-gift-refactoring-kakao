package gift.acceptance;

import gift.wish.RepositoryWishCleaner;
import gift.wish.WishCleaner;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 인수 테스트에서 위시 정리를 감싸는 감싸개.
 * 평소에는 진짜 RepositoryWishCleaner에 맡기고, failNext()로 정해 두면 다음 한 번은 맡기지 않고 예외를 던진다.
 * 시나리오마다 WishCleanupSteps의 @Before가 reset()을 부른다(docs/work/08-wish-cleanup/tech-spec.md 5장).
 */
@Component
@Primary
public class FailableWishCleaner implements WishCleaner {

    public static final String FAILURE_MESSAGE = "가짜 위시 정리 실패";

    private final RepositoryWishCleaner delegate;
    private boolean failNext;

    public FailableWishCleaner(RepositoryWishCleaner delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized void clean(Long memberId, Long productId) {
        if (failNext) {
            failNext = false;
            throw new IllegalStateException(FAILURE_MESSAGE);
        }
        delegate.clean(memberId, productId);
    }

    public synchronized void failNext() {
        this.failNext = true;
    }

    public synchronized void reset() {
        this.failNext = false;
    }
}
