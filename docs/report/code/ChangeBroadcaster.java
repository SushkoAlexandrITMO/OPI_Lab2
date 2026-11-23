package ru.itmo.lab1.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.itmo.lab1.config.WebSocketConfig;
import ru.itmo.lab1.dto.EntityChangedEvent;

/**
 Рассылает события об изменениях всем подключённым клиентам.
 Отправка строго после коммита
 */
@Component
public class ChangeBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public ChangeBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChange(EntityChangedEvent event) {
        messagingTemplate.convertAndSend(WebSocketConfig.CHANGES_TOPIC, event);
    }
}
