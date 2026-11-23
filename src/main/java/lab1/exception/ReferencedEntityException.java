package ru.itmo.lab1.exception;

/**
 Удаляемый объект используется другими объектами. Клиент должен выбрать объект-замену
 (или отвязать ссылки, если поле допускает null) и повторить удаление.
 */
public class ReferencedEntityException extends RuntimeException {
    private final long references;
    private final boolean unlinkAllowed;

    public ReferencedEntityException(String message, long references, boolean unlinkAllowed) {
        super(message);
        this.references = references;
        this.unlinkAllowed = unlinkAllowed;
    }

    public long getReferences() {
        return references;
    }

    public boolean isUnlinkAllowed() {
        return unlinkAllowed;
    }
}
