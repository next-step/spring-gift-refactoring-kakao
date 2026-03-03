package gift.ui.seed;

import gift.option.Option;
import gift.option.OptionService;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("seed")
@RestController
@RequestMapping("/api/seed/options")
public class SeedOptionController {
  private final OptionService optionService;

  public SeedOptionController(OptionService optionService) {
    this.optionService = optionService;
  }

  @PostMapping
  public Map<String, Object> create(@RequestBody final Map<String, Object> request) {
    Long productId = ((Number) request.get("productId")).longValue();
    String name = (String) request.get("name");
    int quantity = ((Number) request.get("quantity")).intValue();
    Option saved = optionService.createRaw(productId, name, quantity);
    return Map.of("id", saved.getId(), "name", saved.getName(), "quantity", saved.getQuantity());
  }
}
