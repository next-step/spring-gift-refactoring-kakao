package gift.wish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.OptionRepository;
import gift.order.OrderRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WishServiceTest {

    @Autowired
    private WishService wishService;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OptionRepository optionRepository;

    private Member member;
    private Product product;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        wishRepository.deleteAll();
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();

        member = memberRepository.save(new Member("test@example.com", "password"));
        Category category = categoryRepository.save(
            new Category("테스트", "#000000", "https://test.com/img.jpg", "설명"));
        product = productRepository.save(
            new Product("테스트상품", 1000, "https://test.com/img.jpg", category));
    }

    @Test
    @DisplayName("위시리스트에 상품을 추가한다")
    void addWish_success() {
        var result = wishService.addWish(member.getId(), product.getId());

        assertThat(result.created()).isTrue();
        assertThat(result.wish().getId()).isNotNull();
        assertThat(result.wish().getMemberId()).isEqualTo(member.getId());
        assertThat(result.wish().getProduct().getId()).isEqualTo(product.getId());
    }

    @Test
    @DisplayName("이미 추가된 상품을 다시 추가하면 기존 위시를 반환한다 (멱등)")
    void addWish_duplicate_returnsExisting() {
        var first = wishService.addWish(member.getId(), product.getId());
        var second = wishService.addWish(member.getId(), product.getId());

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        assertThat(second.wish().getId()).isEqualTo(first.wish().getId());
        assertThat(wishRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 상품을 추가하면 예외 발생")
    void addWish_productNotFound() {
        assertThatThrownBy(() -> wishService.addWish(member.getId(), 999L))
            .isInstanceOf(NoSuchElementException.class);
    }
}
