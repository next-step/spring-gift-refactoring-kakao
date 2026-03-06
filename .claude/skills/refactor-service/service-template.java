@Service
public class {Domain}Service {

    private final {Domain}Repository {domain}Repository;

    public {Domain}Service({Domain}Repository {domain}Repository) {
        this.{domain}Repository = {domain}Repository;
    }

    @Transactional(readOnly = true)
    public List<{Domain}> findAll() {
        return {domain}Repository.findAll();
    }

    @Transactional(readOnly = true)
    public {Domain} findById(Long id) {
        return {domain}Repository.findById(id)
            .orElseThrow(() -> new {Domain}Exception({Domain}ErrorCode.{DOMAIN}_NOT_FOUND));
    }

    @Transactional
    public {Domain} save({Domain}Request request) {
        return {domain}Repository.save(request.toEntity());
    }

    @Transactional
    public void deleteById(Long id) {
        {domain}Repository.deleteById(id);
    }
}
