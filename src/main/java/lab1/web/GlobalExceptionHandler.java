package ru.itmo.lab1.web;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.eclipse.persistence.exceptions.EclipseLinkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.itmo.lab1.dto.ErrorResponse;
import ru.itmo.lab1.exception.BadRequestException;
import ru.itmo.lab1.exception.ConflictException;
import ru.itmo.lab1.exception.NotFoundException;
import ru.itmo.lab1.exception.ReferencedEntityException;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Единый формат ошибок API: {status, code, message, fieldErrors, details}.
 * Клиент показывает message пользователю, а fieldErrors — под соответствующими полями формы.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Map<String, String> CONSTRAINT_MESSAGES = Map.ofEntries(
            Map.entry("study_group_name_not_blank", "Название группы не может быть пустым"),
            Map.entry("study_group_students_count_non_negative", "Количество студентов не может быть отрицательным"),
            Map.entry("study_group_expelled_students_positive", "Количество отчисленных должно быть больше 0"),
            Map.entry("study_group_transferred_students_non_negative", "Количество переведённых не может быть отрицательным"),
            Map.entry("study_group_should_be_expelled_positive", "Поле shouldBeExpelled должно быть больше 0"),
            Map.entry("study_group_average_mark_positive", "Средняя оценка должна быть больше 0"),
            Map.entry("person_name_not_blank", "Имя не может быть пустым"),
            Map.entry("person_weight_positive", "Вес должен быть больше 0")
    );

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return validationError("Проверьте правильность заполнения полей", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        Map<String, String> fieldErrors = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(v -> v.getPropertyPath().toString(), v -> v.getMessage(), (a, b) -> a, LinkedHashMap::new));
        return validationError("Проверьте правильность заполнения полей", fieldErrors);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException e) {
        return validationError(e.getMessage(), e.getFieldErrors());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof JsonMappingException jme && !jme.getPath().isEmpty()) {
            String field = jme.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : String.valueOf(ref.getIndex()))
                    .collect(Collectors.joining("."));
            String message = describeFormatError(jme);
            return validationError(message, Map.of(field, message));
        }
        return validationError("Некорректный формат запроса", Map.of());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> handleBadParameter(Exception e) {
        return validationError("Некорректный параметр запроса: " + e.getMessage(), Map.of());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(ReferencedEntityException.class)
    public ResponseEntity<ErrorResponse> handleReferenced(ReferencedEntityException e) {
        ErrorResponse body = new ErrorResponse(HttpStatus.CONFLICT.value(), "REFERENCED", e.getMessage(), Map.of(),
                Map.of("references", e.getReferences(), "unlinkAllowed", e.isUnlinkAllowed()));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler({ConflictException.class, OptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleConflict(Exception e) {
        String message = e instanceof ConflictException
                ? e.getMessage()
                : "Объект был изменён другим пользователем. Обновите данные и повторите попытку";
        return error(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    /** Ошибки БД: нарушение ограничений, исключения из функций PL/pgSQL. */
    @ExceptionHandler({DataAccessException.class, TransactionSystemException.class})
    public ResponseEntity<ErrorResponse> handleDatabase(Exception e) {
        ConstraintViolationException violation = findCause(e, ConstraintViolationException.class);
        if (violation != null) {
            return handleConstraintViolation(violation);
        }
        SQLException sql = findCause(e, SQLException.class);
        if (sql == null) {
            log.error("Ошибка доступа к данным", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "Ошибка при работе с базой данных");
        }
        String state = sql.getSQLState();
        String serverMessage = serverErrorField(sql, "getMessage");
        if (serverMessage == null) {
            serverMessage = sql.getMessage();
        }
        String constraint = serverErrorField(sql, "getConstraint");

        return switch (state == null ? "" : state) {
            case "P0002" -> error(HttpStatus.NOT_FOUND, "NOT_FOUND", serverMessage);
            case "23514", "23502" -> validationError(
                    CONSTRAINT_MESSAGES.getOrDefault(constraint, "Нарушено ограничение базы данных: " + constraint),
                    Map.of());
            case "23503" -> error(HttpStatus.CONFLICT, "REFERENCED",
                    "Связанный объект не существует или используется другими объектами");
            case "22003" -> validationError("Значение выходит за допустимый диапазон", Map.of());
            case "40001", "40P01" -> error(HttpStatus.CONFLICT, "CONFLICT",
                    "Конфликт параллельных изменений, повторите попытку");
            default -> {
                log.error("Ошибка БД", e);
                yield error(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR", serverMessage);
            }
        };
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception e) {
        log.error("Необработанная ошибка", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Внутренняя ошибка сервера");
    }

    private static String describeFormatError(JsonMappingException e) {
        if (e instanceof InvalidFormatException ife && ife.getTargetType() != null) {
            Class<?> type = ife.getTargetType();
            if (type.isEnum()) {
                return "Допустимые значения: " + Arrays.toString(type.getEnumConstants());
            }
            if (type == Integer.class || type == int.class) {
                return "Ожидается целое число от " + Integer.MIN_VALUE + " до " + Integer.MAX_VALUE;
            }
            if (type == Long.class || type == long.class) {
                return "Ожидается целое число";
            }
            if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {
                return "Ожидается число";
            }
        }
        return "Некорректное значение поля";
    }

    /**
     * Поле ServerErrorMessage из PSQLException (имя ограничения, текст ошибки без служебных строк).
     * Через рефлексию: на сервере приложений драйвер PostgreSQL загружен сервером, а не приложением,
     * поэтому класс PSQLException из war с ним не совпадает.
     */
    private static String serverErrorField(SQLException e, String getter) {
        try {
            Object serverError = e.getClass().getMethod("getServerErrorMessage").invoke(e);
            if (serverError == null) {
                return null;
            }
            Object value = serverError.getClass().getMethod(getter).invoke(serverError);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static <T extends Throwable> T findCause(Throwable t, Class<T> type) {
        int depth = 0;
        while (t != null && depth++ < 20) {
            if (type.isInstance(t)) {
                return type.cast(t);
            }
            Throwable next = t.getCause();
            if (next == null && t instanceof EclipseLinkException ele) {
                next = ele.getInternalException();
            }
            if (next == t) {
                break;
            }
            t = next;
        }
        return null;
    }

    private static ResponseEntity<ErrorResponse> validationError(String message, Map<String, String> fieldErrors) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "VALIDATION_ERROR", message, fieldErrors, Map.of()));
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), code, message));
    }
}
