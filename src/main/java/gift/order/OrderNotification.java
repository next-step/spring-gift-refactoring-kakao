package gift.order;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

// ADR 0002: 주문 알림 아웃박스. 주문과 같은 트랜잭션에 저장하고 커밋 뒤 보낸다.
@Entity
@Table(name = "order_notification")
public class OrderNotification {
    private static final int LAST_ERROR_MAX_LENGTH = 1000;
    // ADR 0002: 5번까지 보낸다. 다섯 번째 실패에서 FAILED가 되고 더 보내지 않는다.
    private static final int MAX_ATTEMPTS = 5;

    public enum Status {
        PENDING,
        SENT,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private Long memberId;
    // 주문 시점의 알림 문구(template_object). 카카오 액세스 토큰은 저장하지 않는다.
    private String message;

    @Enumerated(EnumType.STRING)
    private Status status;

    private int attemptCount;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    protected OrderNotification() {}

    public OrderNotification(Long orderId, Long memberId, String message) {
        this.orderId = orderId;
        this.memberId = memberId;
        this.message = message;
        this.status = Status.PENDING;
        this.attemptCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public void markSent() {
        this.attemptCount++;
        this.status = Status.SENT;
        this.sentAt = LocalDateTime.now();
    }

    // 커밋 직후 발송과 재시도가 같은 규칙을 따른다.
    public void recordFailure(String error) {
        this.attemptCount++;
        this.lastError = truncate(error);
        if (this.attemptCount >= MAX_ATTEMPTS) {
            this.status = Status.FAILED;
        }
    }

    // last_error 열은 varchar(1000)이다.
    private static String truncate(String error) {
        if (error == null || error.length() <= LAST_ERROR_MAX_LENGTH) {
            return error;
        }
        return error.substring(0, LAST_ERROR_MAX_LENGTH);
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getMessage() {
        return message;
    }

    public Status getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }
}
