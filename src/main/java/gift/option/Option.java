package gift.option;

import gift.product.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "option")
public class Option {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private String name;

    private int quantity;

    public Option(Product product, String name, int quantity) {
        this.product = product;
        this.name = name;
        this.quantity = quantity;
    }

    public int calculateTotalPrice(int quantity) {
        return this.product.getPrice() * quantity;
    }

    public void subtractQuantity(int amount) {
        if (amount > this.quantity) {
            throw new IllegalArgumentException("차감할 수량이 현재 재고보다 많습니다.");
        }
        this.quantity -= amount;
    }

}
