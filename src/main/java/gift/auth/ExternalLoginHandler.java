package gift.auth;

public interface ExternalLoginHandler {
    ExternalLoginResult login(String authorizationCode);
}
