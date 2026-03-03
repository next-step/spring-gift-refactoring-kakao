package gift.product.common;

public interface ProductNameRule {

    boolean notValid(String productName);

    String describeRule();
}
