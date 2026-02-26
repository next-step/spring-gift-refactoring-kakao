package gift.fixture;

import gift.option.Option;

public class OptionFixture {

    public static Option 기본옵션(int quantity) {
        return new Option(null, "TALL", quantity);
    }

}
