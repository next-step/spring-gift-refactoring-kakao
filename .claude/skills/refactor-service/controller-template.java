@RestController
@RequestMapping("/api/{domains}")
public class {Domain}Controller {

    private final {Domain}Service {domain}Service;

    public {Domain}Controller({Domain}Service {domain}Service) {
        this.{domain}Service = {domain}Service;
    }

    @GetMapping
    public ResponseEntity<List<{Domain}Response>> findAll() {
        return ResponseEntity.ok({domain}Service.findAll().stream()
            .map({Domain}Response::from)
            .toList());
    }

    @PostMapping
    public ResponseEntity<{Domain}Response> create(@Valid @RequestBody {Domain}Request request) {
        {Domain} saved = {domain}Service.save(request);
        return ResponseEntity.created(URI.create("/api/{domains}/" + saved.getId()))
            .body({Domain}Response.from(saved));
    }
}
