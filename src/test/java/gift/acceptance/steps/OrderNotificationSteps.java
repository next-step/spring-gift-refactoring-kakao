package gift.acceptance.steps;

import static org.assertj.core.api.Assertions.assertThat;

import gift.acceptance.FakeOrderNotificationSender;
import gift.acceptance.FakeOrderNotificationSender.Received;
import gift.acceptance.Members;
import gift.acceptance.ScenarioContext;
import gift.order.OrderNotificationRetryJob;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 주문 알림(아웃박스) 단계 정의.
 * 알림 상태를 보는 API가 없어서 포인트처럼 DB에서 확인한다(docs/work/07-order-notification-outbox/tech-spec.md 8장).
 * 카카오 대신 FakeOrderNotificationSender가 알림을 받는다.
 */
public class OrderNotificationSteps {

    private static final Map<String, String> STATUS = Map.of(
            "보낼 것", "PENDING",
            "보냄", "SENT",
            "실패", "FAILED");

    private final ScenarioContext context;
    private final JdbcTemplate jdbcTemplate;
    private final FakeOrderNotificationSender sender;
    private final OrderNotificationRetryJob retryJob;

    public OrderNotificationSteps(
            ScenarioContext context,
            JdbcTemplate jdbcTemplate,
            FakeOrderNotificationSender sender,
            OrderNotificationRetryJob retryJob) {
        this.context = context;
        this.jdbcTemplate = jdbcTemplate;
        this.sender = sender;
        this.retryJob = retryJob;
    }

    @Before
    public void resetFakeSender() {
        sender.reset();
    }

    // 카카오 로그인 콜백은 카카오 서버에 기대서, 카카오 액세스 토큰은 DB에 직접 넣는다.
    @Given("^(user1|user2)에게 카카오 액세스 토큰 \"([^\"]*)\"(?:이|가) 있다$")
    public void giveKakaoAccessToken(String member, String kakaoAccessToken) {
        int updated = jdbcTemplate.update(
                "update member set kakao_access_token = ? where email = ?", kakaoAccessToken, Members.email(member));
        assertThat(updated).isEqualTo(1);
    }

    @Given("^카카오 발송이 다음 (\\d+)번 실패한다$")
    public void failNext(int times) {
        sender.failNext(times);
    }

    @Given("^카카오 발송이 항상 실패한다$")
    public void failAlways() {
        sender.failAlways();
    }

    // 커밋 직후 발송 중에 앱이 꺼져 시도 0번으로 남은 알림을 DB로 만든다(tech-spec 7장).
    // 만든 시각은 앱과 같은 JVM 시계로 정한다(OrderNotification은 LocalDateTime.now()로 기록한다).
    @Given("^응답의 주문 알림을 시도 0번 \"보낼 것\", 만든 시각 (\\d+)분 전으로 바꾼다$")
    public void makeUnattemptedPending(int minutesAgo) {
        int updated = jdbcTemplate.update(
                "update order_notification set status = 'PENDING', attempt_count = 0, last_error = null,"
                        + " sent_at = null, created_at = ? where order_id = ?",
                LocalDateTime.now().minusMinutes(minutesAgo),
                responseOrderId());
        assertThat(updated).isEqualTo(1);
    }

    // 테스트에서는 주기 실행을 기다리지 않고 재시도 작업을 직접 부른다(tech-spec 7장).
    @When("^주문 알림 재시도가 (\\d+)번 돈다$")
    public void runRetry(int times) {
        for (int i = 0; i < times; i++) {
            retryJob.retryOnce();
        }
    }

    @Then("^(user1|user2)의 주문 알림은 (\\d+)건이다$")
    public void notificationCountOfMember(String member, int expected) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from order_notification n join member m on n.member_id = m.id where m.email = ?",
                Integer.class,
                Members.email(member));
        assertThat(count).as("%s의 주문 알림 수", member).isEqualTo(expected);
    }

    @Then("^응답의 주문 알림은 (\\d+)건이다$")
    public void notificationCountOfResponseOrder(int expected) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from order_notification where order_id = ?", Integer.class, responseOrderId());
        assertThat(count).as("응답의 주문 알림 수").isEqualTo(expected);
    }

    @Then("^응답의 주문 알림은 \"(보낼 것|보냄|실패)\", 시도 횟수 (\\d+)(?:이다|다)$")
    public void notificationStatus(String status, int attemptCount) {
        Map<String, Object> row = notificationOfResponseOrder();
        assertThat(row.get("STATUS")).as("주문 알림 상태").isEqualTo(STATUS.get(status));
        assertThat(((Number) row.get("ATTEMPT_COUNT")).intValue())
                .as("주문 알림 시도 횟수")
                .isEqualTo(attemptCount);
    }

    @Then("^응답의 주문 알림에 보낸 시각이 있다$")
    public void notificationHasSentAt() {
        assertThat(notificationOfResponseOrder().get("SENT_AT")).as("보낸 시각").isNotNull();
    }

    @Then("^응답의 주문 알림의 마지막 오류는 가짜 발송 실패다$")
    public void notificationLastErrorIsFakeFailure() {
        assertThat((String) notificationOfResponseOrder().get("LAST_ERROR"))
                .as("마지막 오류")
                .contains(FakeOrderNotificationSender.FAILURE_MESSAGE);
    }

    @Then("^카카오 발송기가 받은 알림은 (?:그대로 )?(\\d+)건이다$")
    public void receivedCount(int expected) {
        assertThat(sender.received()).as("가짜 발송기가 받은 알림 수").hasSize(expected);
    }

    @Then("^카카오 발송기가 마지막으로 받은 카카오 액세스 토큰은 \"([^\"]*)\"(?:이다|다)$")
    public void lastReceivedToken(String expected) {
        assertThat(lastReceived().kakaoAccessToken()).isEqualTo(expected);
    }

    // 문구는 Java text block이라 마지막에 줄바꿈이 있고, Gherkin DocString에는 마지막 줄바꿈이 없어서 붙여 비교한다.
    @Then("^카카오 발송기가 마지막으로 받은 문구는 다음과 같다:$")
    public void lastReceivedMessage(String expected) {
        assertThat(lastReceived().message()).isEqualTo(expected + "\n");
    }

    private Received lastReceived() {
        List<Received> received = sender.received();
        assertThat(received).as("가짜 발송기가 받은 알림").isNotEmpty();
        return received.get(received.size() - 1);
    }

    private long responseOrderId() {
        return context.lastResponse().jsonPath().getLong("id");
    }

    private Map<String, Object> notificationOfResponseOrder() {
        return jdbcTemplate.queryForMap(
                "select status, attempt_count, last_error, sent_at from order_notification where order_id = ?",
                responseOrderId());
    }
}
