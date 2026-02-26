package gift.order;

import gift.option.Option;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "option_id")
    private Option option;
    // Member entity를 직접 참조하지 않고 ID만 저장하여 느슨한 결합 유지
    private Long memberId;
    private int quantity;
    private String message;
    private LocalDateTime orderDateTime;

    protected Order() {
    }

    public Order(Option option, Long memberId, int quantity, String message) {
        this.option = option;
        this.memberId = memberId;
        this.quantity = quantity;
        this.message = message;
        this.orderDateTime = LocalDateTime.now();
    }

}
