package ru.itmo.lab1.dto;

/**
 Событие об изменении данных
 */
public record EntityChangedEvent(EntityType entity, Action action, Long id) {

    public enum EntityType { STUDY_GROUP, PERSON, COORDINATES, LOCATION }

    public enum Action { CREATED, UPDATED, DELETED }
}
