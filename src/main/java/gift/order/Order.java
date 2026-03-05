package gift.order;

import gift.option.Option;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "option_id")
  private Option option;

  // Member는 별개의 애그리게이트이므로 직접 참조 대신 ID만 보관한다.
  // 주문 처리에 필요한 것은 회원의 신원(id)뿐이며, 엔티티를 직접 참조하면
  // 애그리게이트 경계를 침범하고 불필요한 결합도가 생긴다.
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
