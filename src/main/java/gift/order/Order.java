package gift.order;

import gift.option.Option;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "option_id")
    private Option option;
    // Member 엔티티를 직접 참조하지 않고 ID만 저장하여 느슨한 결합 유지
    private Long memberId;
    private int quantity;
    private String message;
    private LocalDateTime orderDateTime;

    public Order(Option option, Long memberId, int quantity, String message) {
        this.option = option;
        this.memberId = memberId;
        this.quantity = quantity;
        this.message = message;
        this.orderDateTime = LocalDateTime.now();
    }

}
