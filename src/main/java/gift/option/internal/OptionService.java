package gift.option.internal;

import gift.global.NotFoundException;
import gift.option.Option;
import gift.product.Product;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OptionService {

    private final OptionProductRepository productRepo;
    private final OptionRepository optionRepo;

    public List<OptionResponse> getOptions(Long productId) {
        Product product = productRepo.findByIdLeftJoinFetchOptions(productId)
                .orElseThrow(NotFoundException::productNotFound);

        return product.getOptions().stream()
                .map(OptionResponse::from)
                .toList();
    }

    @Transactional
    public OptionResponse createOption(Long productId, OptionRequest createRequest) {
        Product product = productRepo.findById(productId)
                .orElseThrow(NotFoundException::productNotFound);

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
        Product product = productRepo.findByIdLeftJoinFetchOptions(productId)
                .orElseThrow(NotFoundException::productNotFound);

        if (product.getOptions().size() <= 1) {
            throw FailedToDeleteOptionException.byInsufficientRemainingOptions();
        }

        Option find = optionRepo.findByIdAndProductId(optionId, productId)
                .orElseThrow(NotFoundException::optionNotFound);

        optionRepo.delete(find);
    }
}
