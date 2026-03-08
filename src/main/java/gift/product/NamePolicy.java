package gift.product;

public enum NamePolicy {
    STANDARD,
    ALLOW_KAKAO;

    public boolean allowsKakao() {
        return this == ALLOW_KAKAO;
    }
}
