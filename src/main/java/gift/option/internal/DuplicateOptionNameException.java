package gift.option.internal;

import gift.global.BadRequestException;

public class DuplicateOptionNameException extends BadRequestException {

    public DuplicateOptionNameException() {
        super("이미 존재하는 옵션명입니다.");
    }
}
