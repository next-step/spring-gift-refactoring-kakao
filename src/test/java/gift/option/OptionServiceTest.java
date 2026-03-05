package gift.option;

import gift.category.entity.Category;
import gift.option.dto.OptionRequest;
import gift.option.entity.Option;
import gift.option.exception.OptionErrorCode;
import gift.option.exception.OptionException;
import gift.option.repository.OptionRepository;
import gift.option.service.OptionService;
import gift.product.entity.Product;
import gift.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OptionServiceTest {
    @Mock
    private OptionRepository optionRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OptionService optionService;

    @Test
    @DisplayName("옵션 조회 시 상품이 없으면 PRODUCT_NOT_FOUND 예외를 던진다")
    void getOptions_productNotFound_throwsException() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        OptionException exception = assertThrows(OptionException.class, () -> optionService.getOptions(1L));

        assertEquals(OptionErrorCode.PRODUCT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("옵션 생성 시 옵션명이 중복이면 DUPLICATE_OPTION_NAME 예외를 던진다")
    void createOption_duplicateName_throwsException() {
        Product product = new Product("아메리카노", 4500, "http://image.png", new Category("음료", "#000000", "http://image.png", null));
        OptionRequest request = new OptionRequest("기본 옵션", 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(optionRepository.existsByProductIdAndName(1L, request.name())).thenReturn(true);

        OptionException exception = assertThrows(OptionException.class, () -> optionService.createOption(1L, request));

        assertEquals(OptionErrorCode.DUPLICATE_OPTION_NAME, exception.getErrorCode());
    }

    @Test
    @DisplayName("옵션 삭제 시 옵션이 1개뿐이면 CANNOT_DELETE_LAST_OPTION 예외를 던진다")
    void deleteOption_lastOption_throwsException() {
        Product product = new Product("아메리카노", 4500, "http://image.png", new Category("음료", "#000000", "http://image.png", null));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(optionRepository.findByProductId(1L)).thenReturn(List.of(new Option(product, "기본 옵션", 10)));

        OptionException exception = assertThrows(OptionException.class, () -> optionService.deleteOption(1L, 1L));

        assertEquals(OptionErrorCode.CANNOT_DELETE_LAST_OPTION, exception.getErrorCode());
    }

    @Test
    @DisplayName("옵션이 요청한 상품 소속이 아니면 OPTION_NOT_FOUND 예외를 던진다")
    void deleteOption_notBelongToProduct_throwsException() {
        Product requestProduct = new Product("아메리카노", 4500, "http://image.png", new Category("음료", "#000000", "http://image.png", null));
        ReflectionTestUtils.setField(requestProduct, "id", 1L);
        Product otherProduct = new Product("카페라떼", 5000, "http://image2.png", new Category("음료", "#000000", "http://image.png", null));
        ReflectionTestUtils.setField(otherProduct, "id", 2L);
        Option target = new Option(otherProduct, "다른 상품 옵션", 10);

        when(productRepository.findById(1L)).thenReturn(Optional.of(requestProduct));
        when(optionRepository.findByProductId(1L)).thenReturn(List.of(
            new Option(requestProduct, "기본 옵션", 10),
            new Option(requestProduct, "추가 옵션", 5)
        ));
        when(optionRepository.findById(10L)).thenReturn(Optional.of(target));

        OptionException exception = assertThrows(OptionException.class, () -> optionService.deleteOption(1L, 10L));

        assertEquals(OptionErrorCode.OPTION_NOT_FOUND, exception.getErrorCode());
    }
}
