package gift.wish;

public class WishResult {
    private final Wish wish;
    private final boolean created;

    public WishResult(Wish wish, boolean created) {
        this.wish = wish;
        this.created = created;
    }

    public Wish getWish() {
        return wish;
    }

    public boolean isCreated() {
        return created;
    }
}
