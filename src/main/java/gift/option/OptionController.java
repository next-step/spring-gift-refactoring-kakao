package gift.option;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/*
 * Each product must have at least one option at all times.
 * Option names are validated against allowed characters and length constraints.
 */
@RestController
@RequestMapping("/api/products/{productId}/options")
public class OptionController {
    private final OptionService optionService;

    public OptionController(OptionService optionService) {
        this.optionService = optionService;
    }

    @GetMapping
    public ResponseEntity<List<OptionResponse>> getOptions(@PathVariable Long productId) {
        try {
            List<OptionResponse> options = optionService.getOptions(productId).stream()
                .map(OptionResponse::from)
                .collect(Collectors.toList());
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
            Option saved = optionService.createOption(productId, request);
            URI location = URI.create("/api/products/" + productId + "/options/" + saved.getId());
            return ResponseEntity.created(location)
                .body(OptionResponse.from(saved));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{optionId}")
    public ResponseEntity<OptionResponse> updateOption(
        @PathVariable Long productId,
        @PathVariable Long optionId,
        @Valid @RequestBody OptionRequest request
    ) {
        try {
            Option updated = optionService.updateOption(productId, optionId, request);
            return ResponseEntity.ok(OptionResponse.from(updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{optionId}")
    public ResponseEntity<Void> deleteOption(
        @PathVariable Long productId,
        @PathVariable Long optionId
    ) {
        try {
            optionService.deleteOption(productId, optionId);
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
