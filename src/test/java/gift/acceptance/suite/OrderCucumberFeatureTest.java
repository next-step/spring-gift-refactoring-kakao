package gift.acceptance.suite;

import org.junit.platform.suite.api.SelectClasspathResource;

@SelectClasspathResource("features/order.feature")
public class OrderCucumberFeatureTest extends AbstractCucumberFeatureTest {

}
