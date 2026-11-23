import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { errorMessage } from '../api/client';
import { coordinatesApi } from '../api/endpoints';
import type { CoordinatesDto } from '../api/types';
import { DeleteDialog } from '../components/DeleteDialog';
import { EditorModal } from '../components/EditorModal';
import { CoordinatesFields, coordinatesLabel, useCoordinatesList } from '../components/EntityFields';
import { ErrorBanner, useToast } from '../components/ui';
import { buildCoordinates, coordinatesToForm, emptyCoordinates, type CoordinatesForm } from '../forms/model';

export function CoordinatesPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const list = useCoordinatesList();
  const [editing, setEditing] = useState<CoordinatesDto | 'new' | null>(null);
  const [deleting, setDeleting] = useState<CoordinatesDto | null>(null);

  return (
    <div className="stack">
      <div className="toolbar">
        <h1>Координаты</h1>
        <button className="btn btn--primary" onClick={() => setEditing('new')}>
          + Добавить
        </button>
      </div>
      {list.error && <ErrorBanner message={errorMessage(list.error)} />}

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>X</th>
              <th>Y</th>
              <th>Действия</th>
            </tr>
          </thead>
          <tbody>
            {list.data?.map((c) => (
              <tr key={c.id}>
                <td>{c.id}</td>
                <td>{c.x}</td>
                <td>{c.y}</td>
                <td className="row-actions">
                  <button className="btn btn--small" onClick={() => setEditing(c)}>
                    Изменить
                  </button>
                  <button className="btn btn--small btn--danger" onClick={() => setDeleting(c)}>
                    Удалить
                  </button>
                </td>
              </tr>
            ))}
            {list.data?.length === 0 && (
              <tr>
                <td colSpan={4} className="muted center">
                  Пока пусто
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {editing && (
        <EditorModal<CoordinatesForm, ReturnType<typeof buildCoordinates>>
          title={editing === 'new' ? 'Новые координаты' : `Редактирование #${editing.id}`}
          initial={editing === 'new' ? emptyCoordinates() : coordinatesToForm(editing)}
          build={(form, errors) => buildCoordinates(errors, '', form)}
          submit={async (request) => {
            if (editing === 'new') await coordinatesApi.create(request);
            else await coordinatesApi.update(editing.id, { ...request, version: editing.version });
            await queryClient.invalidateQueries();
            toast('Сохранено');
          }}
          onClose={() => setEditing(null)}
        >
          {(form, setForm, errors) => <CoordinatesFields value={form} onChange={setForm} errors={errors} prefix="" />}
        </EditorModal>
      )}

      {deleting && (
        <DeleteDialog
          title="Удаление координат"
          target={deleting}
          describe={coordinatesLabel}
          candidates={list.data ?? []}
          onClose={() => setDeleting(null)}
          onDelete={async (options) => {
            await coordinatesApi.remove(deleting.id, options);
            await queryClient.invalidateQueries();
            toast('Удалено');
          }}
        />
      )}
    </div>
  );
}
