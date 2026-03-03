package gift.wish;

import gift.product.Product;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Wish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId; // Primitive FK (no entity reference)

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    protected Wish() { }

    public Wish(Long memberId, Product product) {
        this.memberId = memberId;
        this.product = product;
    }
}
