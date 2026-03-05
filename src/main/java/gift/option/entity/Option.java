package gift.option.entity;

import gift.option.exception.OptionErrorCode;
import gift.option.exception.OptionException;
import gift.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Option {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int quantity;

    public Option(Product product, String name, int quantity) {
        this.product = product;
        this.name = name;
        this.quantity = quantity;
    }

    public void subtractQuantity(int amount) {
        if (amount > this.quantity) {
            throw new OptionException(OptionErrorCode.INSUFFICIENT_OPTION_QUANTITY);
        }
        this.quantity -= amount;
    }
}
