# ADR-014: 전역 예외 처리 통합

## 상태
- [x] 승인됨 (2026-03)

## 맥락
리뷰어가 예외 처리의 불일치를 지적했다:

- MemberController, OptionController, ProductController: `@ExceptionHandler` 있음
- CategoryController, OrderController, WishController, KakaoAuthController: 없음

동일한 코드가 3곳에 중복되어 있었다:
```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.badRequest().body(e.getMessage());
}
```

## 결정
`@RestControllerAdvice`를 사용하여 전역 예외 처리로 통합한다.

## 변경 사항

### GlobalExceptionHandler.java (신규)
```java
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
```

### 제거된 코드
- MemberController: @ExceptionHandler 메서드 제거
- ProductController: @ExceptionHandler 메서드 제거
- OptionController: @ExceptionHandler 메서드 제거

### 테스트 수정
OrderAcceptanceTest에서 기존에 500을 기대하던 케이스가 이제 400을 반환:
- 재고 부족 시 → 400
- 포인트 부족 시 → 400

## 결과
- 90개 테스트 통과
- 코드 중복 제거 (3곳 → 1곳)
- 모든 Controller에서 일관된 예외 처리
- OrderController 예외 처리 개선 (500 → 400)

## 트레이드오프

| 항목 | 이전 | 이후 |
|------|------|------|
| 예외 처리 위치 | 개별 Controller | 전역 1곳 |
| 일관성 | 불일치 (일부만 처리) | 일관됨 |
| 유지보수 | 3곳 수정 필요 | 1곳만 수정 |
| Controller별 커스터마이징 | 가능 | 전역 우선, 필요시 개별 오버라이드 |
