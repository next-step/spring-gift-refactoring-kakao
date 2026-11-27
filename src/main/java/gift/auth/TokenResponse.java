package gift.auth;

/**
 * Response containing a login token (JWT).
 *
 * @author brian.kim
 * @since 1.0
 */
public record TokenResponse(String token) {}
