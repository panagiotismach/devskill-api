package com.devskill.devskill_api.services;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class MaterializedViewService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    public void createMaterializedView() {
        transactionTemplate.execute(status -> {

            entityManager.createNativeQuery("""
            CREATE MATERIALIZED VIEW IF NOT EXISTS extension_co_occurrence AS
            WITH exploded AS (
                SELECT r.id AS repo_id, ext1 AS extension
                FROM repositories r
                CROSS JOIN LATERAL jsonb_array_elements_text(r.extensions::jsonb) AS ext1
            )
            SELECT e1.extension AS ext_a, e2.extension AS ext_b,
                   COUNT(DISTINCT e1.repo_id) AS co_count
            FROM exploded e1
            JOIN exploded e2 ON e1.repo_id = e2.repo_id AND e1.extension < e2.extension
            GROUP BY e1.extension, e2.extension
            ORDER BY co_count DESC
        """).executeUpdate();

            entityManager.createNativeQuery(
                    "CREATE INDEX IF NOT EXISTS idx_co_occurrence ON extension_co_occurrence(co_count DESC)"
            ).executeUpdate();

            return null;
        });
    }
}