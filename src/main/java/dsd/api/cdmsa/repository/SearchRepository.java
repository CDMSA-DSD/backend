package dsd.api.cdmsa.repository;

import dsd.api.cdmsa.dto.SearchResultProjection;
import dsd.api.cdmsa.model.RFC;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SearchRepository extends JpaRepository<RFC, Long> {

    @Query(value = """
        SELECT * FROM (
            SELECT 
                a.id, 
                a.title, 
                LEFT(a.context, 200) AS snippet, 
                CONCAT(u.firstname, ' ', u.lastname) AS authorName, 
                a.created_at AS date, 
                'ADR' AS type, 
                ts_rank(
                    to_tsvector('english', CONCAT(a.title, ' ', a.context, ' ', a.decision)), 
                    to_tsquery('english', CAST(:query AS text))
                ) AS score
            FROM adrs a
            JOIN rfc r ON a.rfc_id = r.id      
            JOIN app_users u ON r.user_id = u.id
            WHERE 
                to_tsvector('english', CONCAT(a.title, ' ', a.context, ' ', a.decision)) 
                @@ to_tsquery('english', CAST(:query AS text))
                
                AND r.org_id = CAST(:orgId AS bigint)
                
                AND (CAST(:authorId AS bigint) IS NULL OR r.user_id = CAST(:authorId AS bigint))
                AND (CAST(:dateFrom AS timestamp) IS NULL OR a.created_at >= CAST(:dateFrom AS timestamp))
                AND (CAST(:dateTo AS timestamp) IS NULL OR a.created_at <= CAST(:dateTo AS timestamp))

            UNION ALL

            SELECT 
                r.id, 
                r.title, 
                LEFT(r.description, 200) AS snippet, 
                CONCAT(u.firstname, ' ', u.lastname) AS authorName, 
                r.created_at AS date, 
                'RFC' AS type,
                ts_rank(
                    to_tsvector('english', CONCAT(r.title, ' ', r.description)), 
                    to_tsquery('english', CAST(:query AS text))
                ) AS score
            FROM rfc r
            JOIN app_users u ON r.user_id = u.id
            WHERE 
                to_tsvector('english', CONCAT(r.title, ' ', r.description)) 
                @@ to_tsquery('english', CAST(:query AS text))
                
                AND r.org_id = CAST(:orgId AS bigint)
                
                AND (CAST(:authorId AS bigint) IS NULL OR r.user_id = CAST(:authorId AS bigint))
                AND (CAST(:dateFrom AS timestamp) IS NULL OR r.created_at >= CAST(:dateFrom AS timestamp))
                AND (CAST(:dateTo AS timestamp) IS NULL OR r.created_at <= CAST(:dateTo AS timestamp))
        ) AS combined
        
        ORDER BY 
            CASE WHEN CAST(:sort AS text) = 'date_desc' THEN date END DESC,
            CASE WHEN CAST(:sort AS text) = 'date_asc'  THEN date END ASC,
            CASE WHEN CAST(:sort AS text) = 'relevance' THEN score END DESC
        LIMIT 50
    """, nativeQuery = true)
    List<SearchResultProjection> searchSimple(
            @Param("query") String query,
            @Param("sort") String sort,
            @Param("authorId") Long authorId,
            @Param("orgId") Long orgId,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo
    );
}