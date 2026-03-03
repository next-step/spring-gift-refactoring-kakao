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

  // primitive FK - no entity reference
  private Long memberId;

  @ManyToOne
  @JoinColumn(name = "product_id")
  private Product product;

  public Wish(Long memberId, Product product) {
    this.memberId = memberId;
    this.product = product;
  }
}
