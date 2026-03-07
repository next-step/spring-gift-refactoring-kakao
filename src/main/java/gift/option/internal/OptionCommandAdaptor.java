package gift.option.internal;

import gift.global.NotFoundException;
import gift.option.Option;
import gift.option.OptionCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OptionCommandAdaptor implements OptionCommandPort {

    private final OptionRepository optionRepo;

    @Override
    @Transactional
    public void subtractQuantity(Long id, int amount) {
        Option option = findOrThrowNotFound(id);

        option.subtractQuantity(amount);
    }

    private Option findOrThrowNotFound(Long id) {
        return optionRepo.findById(id)
                .orElseThrow(NotFoundException::optionNotFound);
    }
}
