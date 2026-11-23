import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { errorMessage } from '../api/client';
import { locationsApi } from '../api/endpoints';
import type { LocationDto } from '../api/types';
import { DeleteDialog } from '../components/DeleteDialog';
import { EditorModal } from '../components/EditorModal';
import { LocationFields, locationLabel, useLocationsList } from '../components/EntityFields';
import { ErrorBanner, useToast } from '../components/ui';
import { buildLocation, emptyLocation, locationToForm, type LocationForm } from '../forms/model';

export function LocationsPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const list = useLocationsList();
  const [editing, setEditing] = useState<LocationDto | 'new' | null>(null);
  const [deleting, setDeleting] = useState<LocationDto | null>(null);

  return (
    <div className="stack">
      <div className="toolbar">
        <h1>Локации</h1>
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
              <th>Название</th>
              <th>X</th>
              <th>Y</th>
              <th>Действия</th>
            </tr>
          </thead>
          <tbody>
            {list.data?.map((l) => (
              <tr key={l.id}>
                <td>{l.id}</td>
                <td>{l.name}</td>
                <td>{l.x}</td>
                <td>{l.y}</td>
                <td className="row-actions">
                  <button className="btn btn--small" onClick={() => setEditing(l)}>
                    Изменить
                  </button>
                  <button className="btn btn--small btn--danger" onClick={() => setDeleting(l)}>
                    Удалить
                  </button>
                </td>
              </tr>
            ))}
            {list.data?.length === 0 && (
              <tr>
                <td colSpan={5} className="muted center">
                  Пока пусто
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {editing && (
        <EditorModal<LocationForm, ReturnType<typeof buildLocation>>
          title={editing === 'new' ? 'Новая локация' : `Редактирование #${editing.id}`}
          initial={editing === 'new' ? emptyLocation() : locationToForm(editing)}
          build={(form, errors) => buildLocation(errors, '', form)}
          submit={async (request) => {
            if (editing === 'new') await locationsApi.create(request);
            else await locationsApi.update(editing.id, { ...request, version: editing.version });
            await queryClient.invalidateQueries();
            toast('Сохранено');
          }}
          onClose={() => setEditing(null)}
        >
          {(form, setForm, errors) => <LocationFields value={form} onChange={setForm} errors={errors} prefix="" />}
        </EditorModal>
      )}

      {deleting && (
        <DeleteDialog
          title="Удаление локации"
          target={deleting}
          describe={locationLabel}
          candidates={list.data ?? []}
          onClose={() => setDeleting(null)}
          onDelete={async (options) => {
            await locationsApi.remove(deleting.id, options);
            await queryClient.invalidateQueries();
            toast('Удалено');
          }}
        />
      )}
    </div>
  );
}
