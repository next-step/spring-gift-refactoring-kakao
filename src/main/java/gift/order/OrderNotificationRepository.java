package gift.order;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderNotificationRepository extends JpaRepository<OrderNotification, Long> {

    // ADR 0002: 재시도 대상. 한 번 이상 실패했거나, 커밋 직후 발송 중에 앱이 꺼져 시도 0번으로 오래 남은 "보낼 것".
    @Query("select n.id from OrderNotification n"
            + " where n.status = :status"
            + " and (n.attemptCount >= 1 or n.createdAt < :createdBefore)"
            + " order by n.id")
    List<Long> findRetryTargetIds(
            @Param("status") OrderNotification.Status status, @Param("createdBefore") LocalDateTime createdBefore);
}
