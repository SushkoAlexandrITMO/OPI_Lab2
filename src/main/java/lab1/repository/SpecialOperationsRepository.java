package ru.itmo.lab1.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Вызов функций БД
 * Функции, возвращающие строки study_group, используются только для получения id —
 * сами объекты затем загружаются через JPA, чтобы маппинг сущности был в одном месте.
 */
@Repository
public class SpecialOperationsRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<Long> findMinExpelledId() {
        List<?> ids = em.createNativeQuery("SELECT g.id FROM sg_min_expelled() g").getResultList();
        return ids.stream().findFirst().map(id -> ((Number) id).longValue());
    }

    public List<Object[]> countById() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("SELECT group_id, cnt FROM sg_count_by_id()").getResultList();
        return rows;
    }

    public List<Long> findIdsWithAdminGreaterThan(long personId) {
        List<?> ids = em.createNativeQuery("SELECT g.id FROM sg_admin_greater_than(?1) g")
                .setParameter(1, personId)
                .getResultList();
        return ids.stream().map(id -> ((Number) id).longValue()).toList();
    }

    public int expelAll(long groupId) {
        Object result = em.createNativeQuery("SELECT sg_expel_all(?1)")
                .setParameter(1, groupId)
                .getSingleResult();
        return ((Number) result).intValue();
    }

    public int addStudent(long groupId) {
        Object result = em.createNativeQuery("SELECT sg_add_student(?1)")
                .setParameter(1, groupId)
                .getSingleResult();
        return ((Number) result).intValue();
    }
}
