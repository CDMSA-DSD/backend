package dsd.api.cdmsa.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConfigIndexes implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public ConfigIndexes(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {

        String rfcExpression = "to_tsvector('english', coalesce(title, '') || ' ' || coalesce(description, ''))";
        recreateIndex("rfc", "ft_idx_rfc_combined", rfcExpression);

        String adrExpression = "to_tsvector('english', coalesce(title, '') || ' ' || coalesce(context, '') || ' ' || coalesce(decision, ''))";
        recreateIndex("adrs", "ft_idx_adr_combined", adrExpression);
    }

    private void recreateIndex(String tableName, String indexName, String expression) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM pg_indexes WHERE tablename = ? AND indexname = ?",
                    Integer.class, tableName, indexName
            );

            if (count != null && count > 0) {
                System.out.println("Index '" + indexName + "' exists on " + tableName + ". Dropping to regenerate...");
                jdbcTemplate.execute("DROP INDEX " + indexName);
            }

            System.out.println("Creating GIN Index '" + indexName + "' on " + tableName + "...");
            String sql = "CREATE INDEX " + indexName + " ON " + tableName + " USING GIN (" + expression + ")";
            jdbcTemplate.execute(sql);

            System.out.println("Index '" + indexName + "' created successfully.");

        } catch (Exception e) {
            System.err.println("Impossible configure index on '" + tableName + "'. Motivation: " + e.getMessage());
        }
    }
}