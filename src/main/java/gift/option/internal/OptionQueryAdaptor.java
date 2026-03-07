package gift.option.internal;

import gift.global.NotFoundException;
import gift.option.Option;
import gift.option.OptionQueryPort;
import gift.product.Product;
import gift.product.ProductDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OptionQueryAdaptor implements OptionQueryPort {

    private final OptionRepository optionRepo;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Option getReference(Long id) {
        return optionRepo.findById(id)
                .orElseThrow(NotFoundException::optionNotFound);
    }

    @Override
    public ProductDto getAssociatedProduct(Long optionId) {
        Option option = optionRepo.findWithProductById(optionId)
                .orElseThrow(NotFoundException::optionNotFound);

        return convertToProductDto(option.getProduct());
    }

    private static ProductDto convertToProductDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getImageUrl(),
                product.getCategory().getId()
        );
    }
}
