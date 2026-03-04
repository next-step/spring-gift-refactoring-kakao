package gift.option;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

/*
 * 각 상품은 항상 최소 하나의 옵션을 가져야 한다.
 * 옵션명은 허용된 문자와 길이 제약에 따라 검증된다.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/products/{productId}/options")
public class OptionController {
    private final OptionService optionService;

    @GetMapping
    public ResponseEntity<List<OptionResponse>> getOptions(@PathVariable Long productId) {
        try {
            List<OptionResponse> options = optionService.findByProductId(productId).stream()
                    .map(OptionResponse::from)
                    .toList();
            return ResponseEntity.ok(options);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<OptionResponse> createOption(
            @PathVariable Long productId,
            @Valid @RequestBody OptionRequest request
    ) {
        try {
            Option saved = optionService.create(productId, request.name(), request.quantity());
            URI location = URI.create("/api/products/" + productId + "/options/" + saved.getId());
            return ResponseEntity.created(location)
                    .body(OptionResponse.from(saved));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping(path = "/{optionId}")
    public ResponseEntity<Void> deleteOption(
            @PathVariable Long productId,
            @PathVariable Long optionId
    ) {
        try {
            optionService.delete(productId, optionId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
