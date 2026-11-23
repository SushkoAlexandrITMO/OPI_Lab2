import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { ApiError, errorMessage } from '../api/client';
import { groupsApi, queryKeys } from '../api/endpoints';
import { groupToForm } from '../forms/model';
import { GroupForm } from './GroupForm';
import { ErrorBanner, Modal, useToast } from './ui';

/**
 * Отдельное диалоговое окно редактирования группы. Открывается из таблицы и из карточки объекта.
 * Если во время редактирования объект изменил или удалил другой пользователь, диалог сообщает об этом.
 */
export function EditGroupDialog({ id, onClose }: { id: number; onClose: () => void }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const query = useQuery({ queryKey: queryKeys.group(id), queryFn: () => groupsApi.get(id), retry: false });
  // Версия, с которой пользователь начал редактирование; меняется только по кнопке «Загрузить»
  const [baseVersion, setBaseVersion] = useState<number | null>(null);
  const [formKey, setFormKey] = useState(0);

  const group = query.data;
  if (group && baseVersion === null) setBaseVersion(group.version);

  const deleted = query.error instanceof ApiError && query.error.status === 404;
  const changedByOther = group && baseVersion !== null && group.version !== baseVersion;

  return (
    <Modal title={`Редактирование группы #${id}`} onClose={onClose} wide>
      {query.isLoading && <p>Загрузка…</p>}
      {deleted && <ErrorBanner message="Группа была удалена другим пользователем" />}
      {query.error && !deleted && <ErrorBanner message={errorMessage(query.error)} />}
      {changedByOther && (
        <div className="banner banner--warn">
          Группа была изменена другим пользователем. Сохранение ваших изменений будет отклонено.{' '}
          <button
            className="btn btn--small"
            onClick={() => {
              setBaseVersion(group.version);
              setFormKey((k) => k + 1);
            }}
          >
            Загрузить актуальные данные
          </button>
        </div>
      )}
      {group && !deleted && baseVersion !== null && (
        <GroupForm
          key={formKey}
          initial={groupToForm(group)}
          submitLabel="Сохранить"
          onCancel={onClose}
          onSubmit={async (request) => {
            await groupsApi.update(id, { ...request, version: baseVersion });
            await queryClient.invalidateQueries();
            toast('Группа обновлена');
            onClose();
          }}
        />
      )}
    </Modal>
  );
}
