package gift.option.internal;

import gift.global.NotFoundException;
import gift.option.Option;
import gift.product.Product;
import gift.product.ProductQueryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OptionService {

    private final ProductQueryPort productQueryPort;
    private final OptionRepository optionRepo;

    public List<OptionResponse> getOptions(Long productId) {
        productQueryPort.validateExists(productId);

        return optionRepo.findAllByProductId(productId).stream()
                .map(OptionResponse::from)
                .toList();
    }

    @Transactional
    public OptionResponse createOption(Long productId, OptionRequest createRequest) {
        Product product = productQueryPort.getReference(productId);

        String name = createRequest.name();
        int quantity = createRequest.quantity();

        if (optionRepo.existsByProductIdAndName(productId, name)) {
            throw new DuplicateOptionNameException();
        }

        Option build = Option.builder()
                .product(product)
                .name(name)
                .quantity(quantity)
                .build();

        Option newEntity = optionRepo.save(build);

        return OptionResponse.from(newEntity);
    }

    @Transactional
    public void deleteOption(Long productId, Long optionId) {
        productQueryPort.validateExists(productId);

        if (optionRepo.countByProductId(productId) <= 1) {
            throw FailedToDeleteOptionException.byInsufficientRemainingOptions();
        }

        Option find = optionRepo.findByIdAndProductId(optionId, productId)
                .orElseThrow(NotFoundException::optionNotFound);

        optionRepo.delete(find);
    }
}
