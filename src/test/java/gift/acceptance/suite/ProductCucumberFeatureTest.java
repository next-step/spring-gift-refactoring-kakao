package gift.acceptance.suite;

import org.junit.platform.suite.api.SelectClasspathResource;

@SelectClasspathResource("features/product.feature")
public class ProductCucumberFeatureTest extends AbstractCucumberFeatureTest {

}
