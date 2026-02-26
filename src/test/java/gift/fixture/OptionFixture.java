package gift.fixture;

import gift.option.Option;

public class OptionFixture {

    public static Option 기본옵션(int quantity) {
        return 옵션("TALL", quantity);
    }

    public static Option 옵션(String name, int quantity) {
        return new Option(null, name, quantity);
    }

}
