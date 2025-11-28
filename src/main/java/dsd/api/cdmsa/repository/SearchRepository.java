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
            -- searching in ADRs
            SELECT 
                a.id, 
                a.title, 
                LEFT(a.context, 200) AS snippet, 
                CONCAT(u.firstname, ' ', u.lastname) AS authorName, 
                a.created_at AS date, 
                'ADR' AS type, 
                MATCH(a.title, a.context, a.decision) AGAINST(:query IN BOOLEAN MODE) AS score
            FROM adrs a
            JOIN rfc r ON a.rfc_id = r.id
            JOIN app_users u ON r.user_id = u.id
            WHERE 
                MATCH(a.title, a.context, a.decision) AGAINST(:query IN BOOLEAN MODE)
                
                -- author filter (Optional)
                AND (:authorId IS NULL OR r.user_id = :authorId)
                
                -- date filter (optional)
                AND (:dateFrom IS NULL OR a.created_at >= :dateFrom)
                AND (:dateTo IS NULL OR a.created_at <= :dateTo)

            UNION ALL

            -- searching in RFCs
            SELECT 
                r.id, 
                r.title, 
                LEFT(r.description, 200) AS snippet, 
                CONCAT(u.firstname, ' ', u.lastname) AS authorName, 
                r.created_at AS date, 
                'RFC' AS type, 
                MATCH(r.title, r.description) AGAINST(:query IN BOOLEAN MODE) AS score
            FROM rfc r
            JOIN app_users u ON r.user_id = u.id
            WHERE 
                MATCH(r.title, r.description) AGAINST(:query IN BOOLEAN MODE)
                
                -- same filters as ADRs
                AND (:authorId IS NULL OR r.user_id = :authorId)
                AND (:dateFrom IS NULL OR r.created_at >= :dateFrom)
                AND (:dateTo IS NULL OR r.created_at <= :dateTo)
        ) AS combined
        
        -- sorting
        ORDER BY 
            CASE WHEN :sort = 'date_desc' THEN date END DESC,
            CASE WHEN :sort = 'date_asc'  THEN date END ASC,
            CASE WHEN :sort = 'relevance' THEN score END DESC
        LIMIT 50
    """, nativeQuery = true)
    List<SearchResultProjection> searchSimple(
            @Param("query") String query,
            @Param("sort") String sort,
            @Param("authorId") Long authorId,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo
    );
}