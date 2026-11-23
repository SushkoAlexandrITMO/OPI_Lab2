package ru.itmo.lab1.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import ru.itmo.lab1.dto.EntityChangedEvent;
import ru.itmo.lab1.dto.EntityChangedEvent.Action;
import ru.itmo.lab1.dto.EntityChangedEvent.EntityType;

@Component
public class ChangeNotifier {

    private final ApplicationEventPublisher publisher;

    public ChangeNotifier(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(EntityType entity, Action action, Long id) {
        publisher.publishEvent(new EntityChangedEvent(entity, action, id));
    }
}
