package gift.wish;

import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WishServiceTest {

    @Autowired
    private WishService wishService;

    @Autowired
    private WishRepository wishRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Member member;
    private Product product;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(new Member("wish@test.com", "password"));
        Category category = categoryRepository.save(new Category("식품", "#000000", "http://img.url", "설명"));
        product = productRepository.save(new Product("아메리카노", 4500, "http://img.url", category));
    }

    @Test
    @DisplayName("동일한 회원이 같은 상품을 중복 등록해도 위시는 1개만 존재한다")
    void addWishDuplicateReturnsExisting() {
        Wish first = wishService.addWish(member.getId(), product.getId());
        Wish second = wishService.addWish(member.getId(), product.getId());

        assertThat(second.getId()).isEqualTo(first.getId());
        long count = wishRepository.countByMemberIdAndProductId(member.getId(), product.getId());
        assertThat(count).isEqualTo(1);
    }
}
