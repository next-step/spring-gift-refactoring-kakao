package gift.option.service;

import gift.option.dto.OptionRequest;
import gift.option.dto.OptionResponse;
import gift.option.entity.Option;
import gift.option.exception.OptionErrorCode;
import gift.option.exception.OptionException;
import gift.option.repository.OptionRepository;
import gift.product.entity.Product;
import gift.product.exception.ProductErrorCode;
import gift.product.exception.ProductException;
import gift.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductService productService;

    public List<OptionResponse> getOptions(Long productId) {
        findProductOrThrow(productId);
        return optionRepository.findByProductId(productId).stream()
            .map(OptionResponse::from)
            .toList();
    }

    @Transactional
    public OptionResponse createOption(Long productId, OptionRequest request) {
        Product product = findProductOrThrow(productId);
        if (optionRepository.existsByProductIdAndName(productId, request.name())) {
            throw new OptionException(OptionErrorCode.DUPLICATE_OPTION_NAME);
        }
        Option saved = optionRepository.save(request.toEntity(product));
        return OptionResponse.from(saved);
    }

    @Transactional
    public void deleteOption(Long productId, Long optionId) {
        findProductOrThrow(productId);
        List<Option> options = optionRepository.findByProductId(productId);
        Option.assertDeletableIn(options.size());
        Option option = optionRepository.findById(optionId)
            .orElseThrow(() -> new OptionException(OptionErrorCode.OPTION_NOT_FOUND));
        option.assertBelongsTo(productId);
        optionRepository.delete(option);
    }

    public Optional<Option> findById(Long id) {
        return optionRepository.findById(id);
    }

    @Transactional
    public Option save(Option option) {
        return optionRepository.save(option);
    }

    private Product findProductOrThrow(Long productId) {
        try {
            return productService.findByIdOrThrow(productId);
        } catch (ProductException exception) {
            if (exception.getErrorCode() == ProductErrorCode.PRODUCT_NOT_FOUND) {
                throw new OptionException(OptionErrorCode.PRODUCT_NOT_FOUND);
            }
            throw exception;
        }
    }
}
