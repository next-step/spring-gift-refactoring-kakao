package gift.acceptance.suite;

import org.junit.platform.suite.api.SelectClasspathResource;

@SelectClasspathResource("features/wish.feature")
public class WishCucumberFeatureTest extends AbstractCucumberFeatureTest {

}
