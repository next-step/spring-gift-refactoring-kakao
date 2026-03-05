package gift.option;

import gift.category.Category;
import gift.product.Product;
import gift.product.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
