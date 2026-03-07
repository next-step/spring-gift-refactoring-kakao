package gift.steps;

import gift.auth.JwtProvider;
import gift.category.Category;
import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.product.ProductRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonSteps {

    @Autowired
    private SharedContext context;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Given("이름이 {string}인 카테고리가 등록되어 있고")
    public void 카테고리가_등록되어_있고(String name) {
        Category category = categoryRepository.save(new Category(name, "#000000", "", ""));
        context.storeId("categoryId", category.getId());
    }

    @And("{string} 상품이 가격 {int}, 이미지 {string}로 등록되어 있고")
    public void 상품이_등록되어_있고(String name, int price, String imageUrl) {
        Long categoryId = ((Number) context.getId("categoryId")).longValue();
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        Product product = productRepository.save(new Product(name, price, imageUrl, category));
        context.storeId("productId", product.getId());
    }

    @And("재고 {int}개인 {string} 옵션이 등록되어 있고")
    public void 옵션이_등록되어_있고(int quantity, String optionName) {
        Long productId = ((Number) context.getId("productId")).longValue();
        Product product = productRepository.findById(productId).orElseThrow();
        Option option = optionRepository.save(new Option(product, optionName, quantity));
        context.storeOption(optionName, option);
    }

    @And("포인트 {int}을 가진 회원 {string}이 등록되어 있고")
    public void 회원이_등록되어_있고(int point, String email) {
        Member member = new Member(email, "password");
        member.chargePoint(point);
        memberRepository.save(member);
        String token = jwtProvider.createToken(email);
        context.storeMemberToken(email, token);
    }

    @Then("로그인에 성공한다")
    @Then("상품 목록이 조회된다")
    @Then("상품이 조회된다")
    @Then("상품이 수정된다")
    @Then("카테고리 목록이 조회된다")
    @Then("카테고리가 수정된다")
    @Then("옵션 목록이 조회된다")
    @Then("위시가 추가된다")
    @Then("위시 목록이 조회된다")
    @Then("주문 목록이 조회된다")
    public void 응답_OK() {
        assertThat(context.getResponse().statusCode()).isEqualTo(200);
    }

    @Then("회원가입에 성공한다")
    @Then("상품이 생성된다")
    @Then("카테고리가 생성된다")
    @Then("옵션이 추가된다")
    @Then("주문이 완료된다")
    public void 응답_CREATED() {
        assertThat(context.getResponse().statusCode()).isEqualTo(201);
    }

    @Then("상품이 삭제된다")
    @Then("카테고리가 삭제된다")
    @Then("옵션이 삭제된다")
    @Then("위시가 삭제된다")
    public void 응답_NO_CONTENT() {
        assertThat(context.getResponse().statusCode()).isEqualTo(204);
    }

    @Then("회원가입이 거부된다")
    @Then("로그인이 거부된다")
    @Then("상품 생성이 거부된다")
    @Then("옵션 추가가 거부된다")
    @Then("옵션 삭제가 거부된다")
    @Then("주문이 거부된다")
    public void 응답_BAD_REQUEST() {
        assertThat(context.getResponse().statusCode()).isEqualTo(400);
    }

    @Then("인증에 실패한다")
    public void 응답_UNAUTHORIZED() {
        assertThat(context.getResponse().statusCode()).isEqualTo(401);
    }

    @Then("권한이 없어 거부된다")
    public void 응답_FORBIDDEN() {
        assertThat(context.getResponse().statusCode()).isEqualTo(403);
    }

    @Then("상품을 찾을 수 없다")
    @Then("카테고리를 찾을 수 없다")
    @Then("옵션을 찾을 수 없다")
    public void 응답_NOT_FOUND() {
        assertThat(context.getResponse().statusCode()).isEqualTo(404);
    }
}
