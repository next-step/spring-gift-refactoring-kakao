package gift.order;

import gift.member.Member;
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
    @JoinColumn(name = "option_id", nullable = false)
    private Option option;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private int quantity;
    private String message;
    private LocalDateTime orderDateTime;

    public Order(Option option, Member member, int quantity, String message) {
        this.option = option;
        this.member = member;
        this.quantity = quantity;
        this.message = message;
        this.orderDateTime = LocalDateTime.now();
    }

}
