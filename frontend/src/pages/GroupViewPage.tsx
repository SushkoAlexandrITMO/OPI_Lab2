import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ApiError, errorMessage } from '../api/client';
import { groupsApi, queryKeys } from '../api/endpoints';
import { ConfirmDialog } from '../components/DeleteDialog';
import { EditGroupDialog } from '../components/EditGroupDialog';
import { GroupDetails } from '../components/GroupDetails';
import { ErrorBanner, useToast } from '../components/ui';

/** Получение информации об объекте по ID (вместе со связанными объектами). */
export function GroupViewPage() {
  const params = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const id = params.id ? Number(params.id) : null;
  const validId = id !== null && Number.isSafeInteger(id) && id > 0;

  const [input, setInput] = useState(params.id ?? '');
  const [inputError, setInputError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const query = useQuery({
    queryKey: queryKeys.group(validId ? id : 0),
    queryFn: () => groupsApi.get(id as number),
    enabled: validId,
    retry: false,
  });

  function search(e: FormEvent) {
    e.preventDefault();
    const value = input.trim();
    if (!/^\d+$/.test(value) || Number(value) <= 0 || !Number.isSafeInteger(Number(value))) {
      setInputError('ID должен быть положительным целым числом');
      return;
    }
    setInputError(null);
    navigate(`/groups/${value}`);
  }

  const notFound = query.error instanceof ApiError && query.error.status === 404;

  return (
    <div className="stack">
      <h1>Группа по ID</h1>
      <form className="toolbar" onSubmit={search} noValidate>
        <input className="input" placeholder="ID группы" value={input} onChange={(e) => setInput(e.target.value)} />
        <button className="btn btn--primary" type="submit">
          Найти
        </button>
      </form>
      {inputError && <div className="field__error">{inputError}</div>}
      {params.id && !validId && <ErrorBanner message="Некорректный ID в адресе" />}

      {query.isLoading && <p>Загрузка…</p>}
      {notFound && <ErrorBanner message={`Группа #${id} не найдена (возможно, удалена)`} />}
      {query.error && !notFound && <ErrorBanner message={errorMessage(query.error)} />}

      {query.data && (
        <>
          <div className="actions actions--left">
            <button className="btn btn--primary" onClick={() => setEditing(true)}>
              Изменить
            </button>
            <button className="btn btn--danger" onClick={() => setDeleting(true)}>
              Удалить
            </button>
          </div>
          <GroupDetails group={query.data} />
        </>
      )}

      {editing && id !== null && <EditGroupDialog id={id} onClose={() => setEditing(false)} />}
      {deleting && query.data && (
        <ConfirmDialog
          title="Удаление группы"
          text={`Удалить группу «${query.data.name}» (#${query.data.id})?`}
          onClose={() => setDeleting(false)}
          onConfirm={async () => {
            await groupsApi.remove(query.data.id);
            await queryClient.invalidateQueries({ queryKey: queryKeys.groups });
            toast('Группа удалена');
            navigate('/');
          }}
        />
      )}
    </div>
  );
}
