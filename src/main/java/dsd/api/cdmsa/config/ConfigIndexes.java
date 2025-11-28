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

        // Index for the search, on table RFC
        // on title and description
        recreateIndex("rfc", "ft_idx_rfc_combined", "title, description");

        // Index for the search, on table ADR
        // on title, context and decision
        recreateIndex("adrs", "ft_idx_adr_combined", "title, context, decision");
    }

    private void recreateIndex(String tableName, String indexName, String columns) {
        try {
            // Check if index exists already
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM information_schema.statistics " +
                            "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                    Integer.class, tableName, indexName
            );

            // If it exists, we delete to update every time
            if (count != null && count > 0) {
                System.out.println("Index doesn't exist '" + indexName + "' on " + tableName + ". Regenerating...");
                try {
                    jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP INDEX " + indexName);
                } catch (Exception e) {
                    System.out.println();
                }
            }

            // Creating the correct indexes
            System.out.println("Creating index Full-Text '" + indexName + "' on " + tableName + " (" + columns + ")...");

            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD FULLTEXT INDEX " + indexName + " (" + columns + ")");

            System.out.println("Index '" + indexName + "' created/updated with success");

        } catch (Exception e) {
            System.err.println("Impossible configure index on '" + tableName + "'. Motivation: " + e.getMessage());
        }
    }
}