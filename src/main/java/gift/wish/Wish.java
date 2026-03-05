package gift.wish;

import gift.member.Member;
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

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    public Wish(Member member, Product product) {
        this.member = member;
        this.product = product;
    }

    public void validateOwner(Long memberId) {
        if (!this.member.getId().equals(memberId)) {
            throw new IllegalStateException("본인의 위시만 삭제할 수 있습니다.");
        }
    }

}
