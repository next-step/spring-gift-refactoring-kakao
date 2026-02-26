package gift.ui.seed;

import gift.application.CreateOptionRequest;
import gift.application.OptionService;
import gift.model.Option;
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
    public Option create(@RequestBody final CreateOptionRequest request) {
        return optionService.create(request);
    }
}
