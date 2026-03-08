package gift;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.member.MemberService;

@SpringBootTest
class MemberPointConcurrencyTest {

    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @BeforeEach
    void setUp() {
        databaseCleaner.clear();
    }

    @Test
    void 동시_포인트_충전시_lost_update_발생() throws InterruptedException {
        Member member = memberRepository.save(new Member("test@test.com", "pw"));
        Long memberId = member.getId();

        int threadCount = 10;
        int chargeAmount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    memberService.chargePoint(memberId, chargeAmount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        Member result = memberRepository.findById(memberId).orElseThrow();
        // 10번 x 1000원 = 10000원이어야 하지만 lost update로 더 적을 수 있음
        assertThat(result.getPoint()).isEqualTo(threadCount * chargeAmount);
    }
}
