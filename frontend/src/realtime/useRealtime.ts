import { Client } from '@stomp/stompjs';
import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import type { EntityChangedEvent } from '../api/types';

/**
 * Подписка на изменения, которые делают другие пользователи.
 * При любом событии кэш запросов инвалидируется — открытые экраны перезапрашивают данные
 * с текущими фильтрами/страницей/сортировкой. Это надёжнее, чем вручную вставлять объект в страницу.
 */
export function useRealtime(): boolean {
  const queryClient = useQueryClient();
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const client = new Client({
      brokerURL: `${protocol}://${window.location.host}/ws`,
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);
        // После переподключения могли быть пропущены события — перечитываем всё
        queryClient.invalidateQueries();
        client.subscribe('/topic/changes', (message) => {
          const event = JSON.parse(message.body) as EntityChangedEvent;
          if (event.entity === 'STUDY_GROUP' && event.action === 'DELETED' && event.id != null) {
            queryClient.removeQueries({ queryKey: ['group', event.id], exact: true, type: 'inactive' });
          }
          queryClient.invalidateQueries();
        });
      },
      onWebSocketClose: () => setConnected(false),
      onStompError: () => setConnected(false),
    });
    client.activate();
    return () => {
      client.deactivate();
    };
  }, [queryClient]);

  return connected;
}
