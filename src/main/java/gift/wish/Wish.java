package gift.wish;

import gift.common.ForbiddenException;
import gift.product.Product;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "product_id"}))
public class Wish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 엔티티 참조 없이 FK만 저장
    private Long memberId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private LocalDateTime createdDate;

    protected Wish() {
    }

    public Wish(Long memberId, Product product) {
        this.memberId = memberId;
        this.product = product;
        this.createdDate = LocalDateTime.now();
    }

    public void validateOwnership(Long memberId) {
        if (!this.memberId.equals(memberId)) {
            throw new ForbiddenException("본인의 위시만 삭제할 수 있습니다.");
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

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
