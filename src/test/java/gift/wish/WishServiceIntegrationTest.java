package gift.wish;

import gift.IntegrationTest;
import gift.IntegrationTestFixtures;
import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class WishServiceIntegrationTest {

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
    private OptionRepository optionRepository;

    private Member member;
    private Product product;

    @BeforeEach
    void setUp() {
        Category category = IntegrationTestFixtures.savedCategory(categoryRepository);
        product = IntegrationTestFixtures.savedProduct(productRepository, category);
        IntegrationTestFixtures.savedOption(optionRepository, product);
        member = IntegrationTestFixtures.savedMemberWithPoints(memberRepository, "wish@test.com", 100000);
    }

    @Test
    void add_createsWishWithCreatedDate() {
        var result = wishService.add(member.getId(), product.getId());

        assertThat(result.created()).isTrue();
        assertThat(result.wish().getCreatedDate()).isNotNull();
    }

    @Test
    void findByMemberId_defaultSortByCreatedDateDesc() {
        // Create a second product for a second wish
        Product product2 = productRepository.save(
            new Product("두번째 상품", 5000, "http://img.test/p2.png", product.getCategory()));
        IntegrationTestFixtures.savedOption(optionRepository, product2);

        wishService.add(member.getId(), product.getId());
        wishService.add(member.getId(), product2.getId());

        var result = wishService.findByMemberId(member.getId(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        // DESC order: second wish (later createdDate) should come first
        assertThat(result.getContent().get(0).getCreatedDate())
            .isAfterOrEqualTo(result.getContent().get(1).getCreatedDate());
    }

    @Test
    void add_duplicate_returnsExisting() {
        var first = wishService.add(member.getId(), product.getId());
        var second = wishService.add(member.getId(), product.getId());

        assertThat(second.created()).isFalse();
        assertThat(second.wish().getId()).isEqualTo(first.wish().getId());
    }

    @Test
    void remove_deletesWish() {
        var addResult = wishService.add(member.getId(), product.getId());

        wishService.remove(member.getId(), addResult.wish().getId());

        var result = wishService.findByMemberId(member.getId(), PageRequest.of(0, 10));
        assertThat(result.getContent()).isEmpty();
    }
}
