package gift.acceptance.steps;

import static gift.acceptance.context.ContextKeys.CURRENT_CATEGORY;
import static gift.acceptance.context.ContextKeys.CURRENT_MEMBER;
import static gift.acceptance.context.ContextKeys.CURRENT_OPTION;
import static gift.acceptance.context.ContextKeys.CURRENT_PRODUCT;
import static gift.acceptance.context.ContextKeys.CURRENT_WISH;
import static gift.acceptance.context.ContextKeys.MEMBER_A;
import static gift.acceptance.context.ContextKeys.MEMBER_A_WISH;
import static gift.acceptance.context.ContextKeys.MEMBER_B;
import static gift.acceptance.context.ContextKeys.WISH_CATEGORY;

import gift.acceptance.context.ScenarioContext;
import gift.category.Category;
import gift.member.Member;
import gift.option.Option;
import gift.order.Order;
import gift.product.Product;
import gift.support.DataManipulator;
import gift.wish.Wish;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CommonDataSteps {

    private static final String
            DEFAULT_COLOR = "#000000",
            DEFAULT_URL = "https://example.com/img.png";

    private final DataManipulator dataManipulator;
    private final ScenarioContext context;

    // --- Category ---

    @Given("{string} 카테고리가 존재한다")
    public void categoryExists(String name) {
        Category category = dataManipulator.addCategory(name, DEFAULT_COLOR, DEFAULT_URL, null);

        context.saveId(name, category.getId());
        context.saveId(CURRENT_CATEGORY, category.getId());
    }

    @Given("다음 카테고리가 존재한다")
    public void categoriesExist(DataTable dataTable) {
        for (Map<String, String> row : dataTable.asMaps()) {
            String name = row.get("name");
            String color = row.get("color");
            String imageUrl = row.get("imageUrl");
            String description = row.getOrDefault("description", null);

            Category category = dataManipulator.addCategory(name, color, imageUrl, description);

            context.saveId(name, category.getId());
            context.saveId(CURRENT_CATEGORY, category.getId());
        }
    }

    // --- Member ---

    @Given("이메일 {string} 비밀번호 {string} 회원이 존재한다")
    public void memberExists(String email, String password) {
        Member member = dataManipulator.addMember(email, password, null, 0);

        context.saveId(CURRENT_MEMBER, member.getId());
        context.saveId(email, member.getId());
    }

    // --- Product ---

    @Given("{string} 카테고리에 {string} 상품이 존재한다")
    public void productExistsInCategory(String categoryName, String productName) {
        ensureCategoryExists(categoryName);

        Long categoryId = context.getId(categoryName);

        Product product = dataManipulator.addProduct(productName, 10000, DEFAULT_URL, categoryId);

        context.saveId(productName, product.getId());
        context.saveId(CURRENT_PRODUCT, product.getId());
    }

    private void ensureCategoryExists(String categoryName) {
        if (context.doesNotHaveId(categoryName)) {
            Category category = dataManipulator.addCategory(
                    categoryName, DEFAULT_COLOR, DEFAULT_URL, null
            );

            context.saveId(categoryName, category.getId());
            context.saveId(CURRENT_CATEGORY, category.getId());
        }
    }

    @Given("{string} 카테고리에 가격 {int}인 {string} 상품이 존재한다")
    public void productExistsWithPrice(String categoryName, int price, String productName) {
        ensureCategoryExists(categoryName);

        Long categoryId = context.getId(categoryName);

        Product product = dataManipulator.addProduct(productName, price, DEFAULT_URL, categoryId);

        context.saveId(productName, product.getId());
        context.saveId(CURRENT_PRODUCT, product.getId());
    }

    @Given("{string} 카테고리에 상품 {int}개가 존재한다")
    public void multipleProductsExist(String categoryName, int count) {
        ensureCategoryExists(categoryName);

        Long categoryId = context.getId(categoryName);

        for (int i = 1; i <= count; i++) {
            String name = "상품" + i;
            Product product = dataManipulator.addProduct(name, 10000, DEFAULT_URL, categoryId);

            context.saveId(name, product.getId());
            context.saveId(CURRENT_PRODUCT, product.getId());
        }
    }

    // --- Option ---

    @Given("{string} 카테고리에 {string} 상품과 {string} 상품이 존재한다")
    public void twoProductsExist(String categoryName, String product1, String product2) {
        ensureCategoryExists(categoryName);

        Long categoryId = context.getId(categoryName);

        Product p1 = dataManipulator.addProduct(product1, 10000, DEFAULT_URL, categoryId);
        Product p2 = dataManipulator.addProduct(product2, 10000, DEFAULT_URL, categoryId);

        context.saveId(product1, p1.getId());
        context.saveId(product2, p2.getId());
        context.saveId(CURRENT_PRODUCT, p2.getId());
    }

    @Given("해당 상품에 수량 {int}인 {string} 옵션이 존재한다")
    public void optionExistsWithQuantity(int quantity, String name) {
        Long productId = context.currentProductId();

        Option option = dataManipulator.addOption(productId, name, quantity);

        context.saveId(name, option.getId());
        context.saveId(CURRENT_OPTION, option.getId());
    }

    @Given("해당 상품에 {string} 옵션이 존재한다")
    public void optionExists(String name) {
        Long productId = context.currentProductId();

        Option option = dataManipulator.addOption(productId, name, 100);

        context.saveId(name, option.getId());
        context.saveId(CURRENT_OPTION, option.getId());
    }

    @Given("해당 상품에 {string} 옵션 1개만 존재한다")
    public void singleOptionExists(String name) {
        Long productId = context.currentProductId();

        Option option = dataManipulator.addOption(productId, name, 100);

        context.saveId(name, option.getId());
        context.saveId(CURRENT_OPTION, option.getId());
    }

    @Given("해당 상품에 수량 {int}인 {string}과 수량 {int}인 {string} 옵션이 존재한다")
    public void twoOptionsWithQuantity(int qty1, String name1, int qty2, String name2) {
        Long productId = context.currentProductId();

        Option opt1 = dataManipulator.addOption(productId, name1, qty1);
        Option opt2 = dataManipulator.addOption(productId, name2, qty2);

        context.saveId(name1, opt1.getId());
        context.saveId(name2, opt2.getId());
        context.saveId(CURRENT_OPTION, opt1.getId());
    }

    @Given("해당 상품에 {string}, {string} 2개의 옵션이 존재한다")
    public void multipleOptionsExist(String name1, String name2) {
        Long productId = context.currentProductId();

        Option opt1 = dataManipulator.addOption(productId, name1, 100);
        Option opt2 = dataManipulator.addOption(productId, name2, 100);

        context.saveId(name1, opt1.getId());
        context.saveId(name2, opt2.getId());
        context.saveId(CURRENT_OPTION, opt1.getId());
    }

    // --- Wish ---

    @Given("{string}에 {string}, {string} 옵션이 존재한다")
    public void optionsForProduct(String productName, String opt1Name, String opt2Name) {
        Long productId = context.getId(productName);

        Option opt1 = dataManipulator.addOption(productId, opt1Name, 100);
        Option opt2 = dataManipulator.addOption(productId, opt2Name, 100);

        context.saveId(opt1Name, opt1.getId());
        context.saveId(opt2Name, opt2.getId());
    }

    @Given("해당 회원에게 위시 {int}개가 등록되어 있다")
    public void wishesExist(int count) {
        Long memberId = context.currentMemberId();
        Long categoryId;

        if (context.doesNotHaveId(WISH_CATEGORY)) {
            Category category = dataManipulator.addCategory("위시카테고리", DEFAULT_COLOR, DEFAULT_URL,
                    null);
            categoryId = category.getId();
            context.saveId(WISH_CATEGORY, categoryId);
        } else {
            categoryId = context.getId(WISH_CATEGORY);
        }

        for (int i = 1; i <= count; i++) {
            String productName = "위시상품" + i;
            Product product = dataManipulator.addProduct(productName, 10000, DEFAULT_URL,
                    categoryId);

            Wish wish = dataManipulator.addWish(memberId, product.getId());

            context.saveId(productName, product.getId());
            context.saveId("__wish" + i, wish.getId());
            context.saveId(CURRENT_WISH, wish.getId());
        }
    }

    @Given("해당 회원에게 {string} 상품 위시가 이미 등록되어 있다")
    public void wishExistsForProduct(String productName) {
        Long memberId = context.currentMemberId();
        Long productId = context.getId(productName);

        Wish wish = dataManipulator.addWish(memberId, productId);

        context.saveId(CURRENT_WISH, wish.getId());
        context.saveId("__wish_" + productName, wish.getId());
    }

    // --- Order ---

    @Given("해당 회원에게 위시가 등록되어 있다")
    public void wishExists() {
        Long memberId = context.currentMemberId();

        Category category = dataManipulator.addCategory("위시카테고리", DEFAULT_COLOR, DEFAULT_URL, null);
        Product product = dataManipulator.addProduct("위시상품", 10000, DEFAULT_URL, category.getId());
        Wish wish = dataManipulator.addWish(memberId, product.getId());

        context.saveId(CURRENT_WISH, wish.getId());
    }

    // --- Multi-member (Wish W-E5) ---

    @Given("해당 회원에게 주문 {int}개가 존재한다")
    public void ordersExist(int count) {
        Long memberId = context.currentMemberId();

        Category category = dataManipulator.addCategory("주문카테고리", DEFAULT_COLOR, DEFAULT_URL, null);
        Product product = dataManipulator.addProduct("주문상품", 10000, DEFAULT_URL, category.getId());
        Option option = dataManipulator.addOption(product.getId(), "주문옵션", 10000);

        for (int i = 1; i <= count; i++) {
            Order order = dataManipulator.addOrder(option.getId(), memberId, 1, "메시지" + i);
            context.saveId("__order" + i, order.getId());
        }
    }

    @Given("회원A와 회원B가 존재한다")
    public void twoMembersExist() {
        Member memberA = dataManipulator.addMember(
                "memberA@test.com", "password", null, 0
        );
        Member memberB = dataManipulator.addMember(
                "memberB@test.com", "password", null, 0
        );

        context.saveId(CURRENT_MEMBER, memberA.getId());
        context.saveId(MEMBER_A, memberA.getId());
        context.saveId(MEMBER_B, memberB.getId());
    }

    // --- Helper ---

    @Given("회원A에게 위시가 등록되어 있다")
    public void memberAHasWish() {
        Long memberAId = context.getId(MEMBER_A);

        Category category = dataManipulator.addCategory(
                "위시카테고리", DEFAULT_COLOR, DEFAULT_URL, null
        );
        Product product = dataManipulator.addProduct(
                "위시상품", 10000, DEFAULT_URL, category.getId()
        );
        Wish wish = dataManipulator.addWish(memberAId, product.getId());

        context.saveId(MEMBER_A_WISH, wish.getId());
    }
}
