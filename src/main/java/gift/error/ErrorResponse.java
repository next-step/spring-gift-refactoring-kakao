package gift.error;

/* API 에러 응답 */
public record ErrorResponse(ErrorCode code, String message) {
}
