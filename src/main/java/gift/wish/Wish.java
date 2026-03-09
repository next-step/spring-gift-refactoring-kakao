package gift.wish;

import gift.product.Product;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Wish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // primitive FK - no entity reference
    private Long memberId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    protected Wish() {
    }

    public Wish(Long memberId, Product product) {
        this.memberId = memberId;
        this.product = product;
    }

    public void validateOwnership(Long memberId) {
        if (!this.memberId.equals(memberId)) {
            throw new IllegalAccessException("다른 회원의 위시를 삭제할 수 없습니다.");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Product getProduct() {
        return product;
    }
}
