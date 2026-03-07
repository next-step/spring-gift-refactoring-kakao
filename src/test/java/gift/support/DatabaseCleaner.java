package gift.support;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCleaner {

    private static final List<String> EXCLUDED_TABLES = List.of("flyway_schema_history");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void clear() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'",
                String.class);

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        tables.stream()
                .filter(table -> !EXCLUDED_TABLES.contains(table))
                .forEach(table -> jdbcTemplate.execute("TRUNCATE TABLE " + table));
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
