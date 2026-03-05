package gift.member;

public interface TokenProvider {
    String createToken(String email);
}
