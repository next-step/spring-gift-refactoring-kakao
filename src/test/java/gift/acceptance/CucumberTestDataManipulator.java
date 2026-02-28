package gift.acceptance;

import gift.category.Category;
import gift.member.Member;
import gift.option.Option;
import gift.order.Order;
import gift.product.Product;
import gift.support.DataManipulator;
import gift.support.TestCategoryRepository;
import gift.support.TestMemberRepository;
import gift.support.TestOptionRepository;
import gift.support.TestOrderRepository;
import gift.support.TestProductRepository;
import gift.support.TestWishRepository;
import gift.wish.Wish;
import jakarta.annotation.PostConstruct;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * cucumber 인수 테스트용 테스트 데이터 조작기
 */
@Component
@Transactional
@Profile("acceptance-test")
public class CucumberTestDataManipulator implements DataManipulator {

    @Autowired
    private TestCategoryRepository categoryRepo;

    @Autowired
    private TestMemberRepository memberRepo;

    @Autowired
    private TestProductRepository productRepo;

    @Autowired
    private TestOptionRepository optionRepo;

    @Autowired
    private TestWishRepository wishRepo;

    @Autowired
    private TestOrderRepository orderRepo;

    @Override
    public Category addCategory(String name, String color, String imageUrl, String description) {
        return categoryRepo.save(Category.builder()
                .name(name)
                .color(color)
                .imageUrl(imageUrl)
                .description(description)
                .build());
    }

    @Override
    public Member addMember(String email, String password, String kakaoAccessToken, int point) {
        return memberRepo.save(Member.builder()
                .email(email)
                .password(password)
                .kakaoAccessToken(kakaoAccessToken)
                .point(point)
                .build());
    }

    @Override
    public Option addOption(Long productId, String name, int quantity) {
        Product product = productRepo.findById(productId)
                .orElseThrow(AssertionError::new);
        return optionRepo.save(Option.builder()
                .product(product)
                .name(name)
                .quantity(quantity)
                .build());
    }

    @Override
    public Order addOrder(
            Long optionId, Long memberId, int quantity, String message
    ) {
        Option option = optionRepo.findById(optionId)
                .orElseThrow(AssertionError::new);
        return orderRepo.save(Order.builder()
                .option(option)
                .memberId(memberId)
                .quantity(quantity)
                .message(message)
                .build());
    }

    @Override
    public Product addProduct(String name, int price, String imageUrl, Long categoryId) {
        Category category = categoryRepo.findById(categoryId)
                .orElseThrow(AssertionError::new);
        return productRepo.save(Product.builder()
                .name(name)
                .price(price)
                .imageUrl(imageUrl)
                .category(category)
                .build());
    }

    @Override
    public Wish addWish(Long memberId, Long productId) {
        Product product = productRepo.findById(productId)
                .orElseThrow(AssertionError::new);
        return wishRepo.save(Wish.builder()
                .memberId(memberId)
                .product(product)
                .build());
    }

    @Override
    public void initAll() {
        orderRepo.deleteAllInBatch();
        wishRepo.deleteAllInBatch();
        optionRepo.deleteAllInBatch();
        productRepo.deleteAllInBatch();
        memberRepo.deleteAllInBatch();
        categoryRepo.deleteAllInBatch();
    }

    @PostConstruct
    private void checkBeansNonNull() {
        requireNonNulls(
                categoryRepo, memberRepo, productRepo,
                optionRepo, wishRepo, orderRepo
        );
    }

    private static void requireNonNulls(Object... objs) {
        for (Object obj : objs) {
            Objects.requireNonNull(obj);
        }
    }
}
