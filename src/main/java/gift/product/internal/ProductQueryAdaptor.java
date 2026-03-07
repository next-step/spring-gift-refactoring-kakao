package gift.product.internal;

import gift.global.NotFoundException;
import gift.product.Product;
import gift.product.ProductQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductQueryAdaptor implements ProductQueryPort {

    private final ProductRepository productRepo;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Product getReference(Long id) {
        return productRepo.findById(id)
                .orElseThrow(NotFoundException::productNotFound);
    }

    @Override
    public void validateExists(Long id) {
        if (!productRepo.existsById(id)) {
            throw NotFoundException.productNotFound();
        }
    }
}
