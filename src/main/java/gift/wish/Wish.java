package gift.wish;

import gift.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "wish")
public class Wish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Member 엔티티를 직접 참조하지 않고 ID만 저장하여 느슨한 결합 유지
    private Long memberId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    public Wish(Long memberId, Product product) {
        this.memberId = memberId;
        this.product = product;
    }

}
