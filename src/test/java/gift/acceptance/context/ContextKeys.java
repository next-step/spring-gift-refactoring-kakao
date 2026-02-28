package gift.acceptance.context;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContextKeys {

    public static final String
            CURRENT_CATEGORY = "__currentCategory",
            CURRENT_PRODUCT = "__currentProduct",
            CURRENT_OPTION = "__currentOption",
            CURRENT_MEMBER = "__currentMember",
            CURRENT_WISH = "__currentWish";

    public static final String
            MEMBER_A = "__memberA",
            MEMBER_B = "__memberB";

    public static final String MEMBER_A_WISH = "__memberAWish";
    public static final String WISH_CATEGORY = "__wishCategory";
}
