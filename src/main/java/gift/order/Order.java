package gift.order;

import gift.option.Option;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "option_id")
    private Option option;

    // primitive FK
    private Long memberId;

    private int quantity;

    private String message;

    private LocalDateTime orderDateTime;

    protected Order() { }

    public Order(Option option, Long memberId, int quantity, String message) {
        validateQuantity(quantity);
        this.option = option;
        this.memberId = memberId;
        this.quantity = quantity;
        this.message = message;
        this.orderDateTime = LocalDateTime.now();
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("주문 수량은 1 이상이어야 합니다.");
        }
    }
}
