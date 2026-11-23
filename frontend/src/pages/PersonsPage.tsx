import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { errorMessage } from '../api/client';
import { personsApi } from '../api/endpoints';
import { label, type PersonDto } from '../api/types';
import { DeleteDialog } from '../components/DeleteDialog';
import { EditorModal } from '../components/EditorModal';
import { PersonFields, personLabel, usePersonsList } from '../components/EntityFields';
import { ErrorBanner, useToast } from '../components/ui';
import { buildPerson, emptyPerson, personToForm, type PersonForm } from '../forms/model';

export function PersonsPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const persons = usePersonsList();
  const [editing, setEditing] = useState<PersonDto | 'new' | null>(null);
  const [deleting, setDeleting] = useState<PersonDto | null>(null);

  return (
    <div className="stack">
      <div className="toolbar">
        <h1>Люди (администраторы групп)</h1>
        <button className="btn btn--primary" onClick={() => setEditing('new')}>
          + Добавить
        </button>
      </div>
      {persons.error && <ErrorBanner message={errorMessage(persons.error)} />}

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Имя</th>
              <th>Цвет глаз</th>
              <th>Цвет волос</th>
              <th>Вес</th>
              <th>Паспорт</th>
              <th>Национальность</th>
              <th>Локация</th>
              <th>Действия</th>
            </tr>
          </thead>
          <tbody>
            {persons.data?.map((p) => (
              <tr key={p.id}>
                <td>{p.id}</td>
                <td>{p.name}</td>
                <td>{label(p.eyeColor)}</td>
                <td>{label(p.hairColor)}</td>
                <td>{p.weight}</td>
                <td>{p.passportId ?? '—'}</td>
                <td>{label(p.nationality)}</td>
                <td>{p.location ? `#${p.location.id} ${p.location.name}` : '—'}</td>
                <td className="row-actions">
                  <button className="btn btn--small" onClick={() => setEditing(p)}>
                    Изменить
                  </button>
                  <button className="btn btn--small btn--danger" onClick={() => setDeleting(p)}>
                    Удалить
                  </button>
                </td>
              </tr>
            ))}
            {persons.data?.length === 0 && (
              <tr>
                <td colSpan={9} className="muted center">
                  Пока никого нет
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {editing && (
        <EditorModal<PersonForm, ReturnType<typeof buildPerson>>
          title={editing === 'new' ? 'Новый человек' : `Редактирование #${editing.id}`}
          initial={editing === 'new' ? emptyPerson() : personToForm(editing)}
          build={(form, errors) => buildPerson(errors, '', form)}
          submit={async (request) => {
            if (editing === 'new') await personsApi.create(request);
            else await personsApi.update(editing.id, { ...request, version: editing.version });
            await queryClient.invalidateQueries();
            toast('Сохранено');
          }}
          onClose={() => setEditing(null)}
        >
          {(form, setForm, errors) => <PersonFields value={form} onChange={setForm} errors={errors} prefix="" />}
        </EditorModal>
      )}

      {deleting && (
        <DeleteDialog
          title="Удаление человека"
          target={deleting}
          describe={personLabel}
          candidates={persons.data ?? []}
          onClose={() => setDeleting(null)}
          onDelete={async (options) => {
            await personsApi.remove(deleting.id, options);
            await queryClient.invalidateQueries();
            toast('Удалено');
          }}
        />
      )}
    </div>
  );
}
