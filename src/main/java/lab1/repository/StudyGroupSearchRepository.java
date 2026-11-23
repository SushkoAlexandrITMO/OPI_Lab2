package ru.itmo.lab1.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;
import ru.itmo.lab1.domain.StudyGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


@Repository
public class StudyGroupSearchRepository {

    private static final char LIKE_ESCAPE = '\\';

    @PersistenceContext
    private EntityManager em;

    public record SearchResult(List<StudyGroup> content, long total) {
    }

    public SearchResult search(StudyGroupColumn filterColumn, String filterValue,
                               StudyGroupColumn sortColumn, boolean ascending,
                               int page, int size) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<StudyGroup> query = cb.createQuery(StudyGroup.class);
        Root<StudyGroup> root = query.from(StudyGroup.class);
        StudyGroupJoins joins = StudyGroupJoins.of(root);
        query.select(root)
                .where(buildFilter(cb, joins, filterColumn, filterValue))
                .orderBy(buildOrder(cb, joins, sortColumn, ascending));

        List<StudyGroup> content = em.createQuery(query)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<StudyGroup> countRoot = countQuery.from(StudyGroup.class);
        StudyGroupJoins countJoins = StudyGroupJoins.of(countRoot);
        countQuery.select(cb.count(countRoot))
                .where(buildFilter(cb, countJoins, filterColumn, filterValue));
        long total = em.createQuery(countQuery).getSingleResult();

        return new SearchResult(content, total);
    }

    private Predicate[] buildFilter(CriteriaBuilder cb, StudyGroupJoins joins,
                                    StudyGroupColumn column, String value) {
        if (column == null || value == null || value.isEmpty()) {
            return new Predicate[0];
        }
        String needle = value.toLowerCase(Locale.ROOT);

        if (column.getKind() == StudyGroupColumn.Kind.ENUM) {
            // Перечисления хранятся строками, но в JPA это enum-атрибут: подбираем подходящие константы в Java
            List<Object> matching = Arrays.stream(column.getEnumType().getEnumConstants())
                    .filter(e -> e.name().toLowerCase(Locale.ROOT).contains(needle))
                    .<Object>map(e -> e)
                    .toList();
            if (matching.isEmpty()) {
                return new Predicate[]{cb.disjunction()};
            }
            return new Predicate[]{column.path(joins).in(matching)};
        }

        @SuppressWarnings("unchecked")
        Expression<String> path = (Expression<String>) column.path(joins);
        String pattern = "%" + escapeLike(needle) + "%";
        return new Predicate[]{cb.like(cb.lower(path), pattern, LIKE_ESCAPE)};
    }

    private List<Order> buildOrder(CriteriaBuilder cb, StudyGroupJoins joins,
                                   StudyGroupColumn column, boolean ascending) {
        List<Order> orders = new ArrayList<>();
        Path<?> path = column.path(joins);
        orders.add(ascending ? cb.asc(path) : cb.desc(path));
        // Дополнительная сортировка по id — стабильный порядок между страницами
        if (column != StudyGroupColumn.ID) {
            orders.add(cb.asc(joins.root().get("id")));
        }
        return orders;
    }

    /** Символы % и _ из пользовательского ввода должны искаться буквально. */
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
