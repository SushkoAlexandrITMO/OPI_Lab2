package ru.itmo.lab1.service;

import ru.itmo.lab1.exception.BadRequestException;
import ru.itmo.lab1.exception.ConflictException;

import java.util.Objects;

final class ServiceUtils {

    private ServiceUtils() {
    }

    static void checkVersion(Long expected, Long actual) {
        if (expected == null) {
            throw BadRequestException.field("version", "Не передана версия объекта");
        }
        if (!Objects.equals(expected, actual)) {
            throw new ConflictException("Объект был изменён другим пользователем. Обновите данные и повторите попытку");
        }
    }

    static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static void checkReplacement(Long id, Long replacementId) {
        if (Objects.equals(id, replacementId)) {
            throw BadRequestException.field("replacementId", "Объект-замена должен отличаться от удаляемого");
        }
    }

    static String prefixed(String prefix, String field) {
        return prefix.isEmpty() ? field : prefix + "." + field;
    }
}
