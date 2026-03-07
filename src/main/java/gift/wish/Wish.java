package gift.wish;

import gift.product.Product;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wish {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Member는 별개의 애그리게이트이므로 직접 참조 대신 ID만 보관한다.
  // 주문 처리에 필요한 것은 회원의 신원(id)뿐이며, 엔티티를 직접 참조하면
  // 애그리게이트 경계를 침범하고 불필요한 결합도가 생긴다.
  private Long memberId;

  @ManyToOne
  @JoinColumn(name = "product_id")
  private Product product;

  public Wish(Long memberId, Product product) {
    this.memberId = memberId;
    this.product = product;
  }
}
