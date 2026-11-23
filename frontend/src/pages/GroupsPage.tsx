import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { groupsApi, queryKeys, type GroupListParams } from '../api/endpoints';
import { label, type StudyGroupDto } from '../api/types';
import { ConfirmDialog } from '../components/DeleteDialog';
import { EditGroupDialog } from '../components/EditGroupDialog';
import { formatDate } from '../components/GroupDetails';
import { ErrorBanner, useToast } from '../components/ui';

interface Column {
  key: string; // ключ колонки на сервере (StudyGroupColumn)
  title: string;
  filterable: boolean;
  render: (g: StudyGroupDto) => React.ReactNode;
}

const COLUMNS: Column[] = [
  { key: 'id', title: 'ID', filterable: false, render: (g) => g.id },
  { key: 'name', title: 'Название', filterable: true, render: (g) => g.name },
  { key: 'coordinates.x', title: 'X', filterable: false, render: (g) => g.coordinates.x },
  { key: 'coordinates.y', title: 'Y', filterable: false, render: (g) => g.coordinates.y },
  { key: 'creationDate', title: 'Создана', filterable: false, render: (g) => formatDate(g.creationDate) },
  { key: 'studentsCount', title: 'Студентов', filterable: false, render: (g) => g.studentsCount },
  { key: 'expelledStudents', title: 'Отчислено', filterable: false, render: (g) => g.expelledStudents },
  { key: 'transferredStudents', title: 'Переведено', filterable: false, render: (g) => g.transferredStudents },
  { key: 'formOfEducation', title: 'Форма обучения', filterable: true, render: (g) => label(g.formOfEducation) },
  { key: 'shouldBeExpelled', title: 'К отчислению', filterable: false, render: (g) => g.shouldBeExpelled },
  { key: 'averageMark', title: 'Ср. оценка', filterable: false, render: (g) => g.averageMark },
  { key: 'semesterEnum', title: 'Семестр', filterable: true, render: (g) => label(g.semesterEnum) },
  { key: 'groupAdmin.name', title: 'Админ', filterable: true, render: (g) => g.groupAdmin?.name ?? '—' },
  { key: 'groupAdmin.eyeColor', title: 'Цвет глаз', filterable: true, render: (g) => label(g.groupAdmin?.eyeColor) },
  { key: 'groupAdmin.hairColor', title: 'Цвет волос', filterable: true, render: (g) => label(g.groupAdmin?.hairColor) },
  { key: 'groupAdmin.weight', title: 'Вес', filterable: false, render: (g) => g.groupAdmin?.weight ?? '—' },
  { key: 'groupAdmin.passportId', title: 'Паспорт', filterable: true, render: (g) => g.groupAdmin?.passportId ?? '—' },
  { key: 'groupAdmin.nationality', title: 'Национальность', filterable: true, render: (g) => label(g.groupAdmin?.nationality) },
  { key: 'groupAdmin.location.name', title: 'Локация', filterable: true, render: (g) => g.groupAdmin?.location?.name ?? '—' },
];

const PAGE_SIZES = [5, 10, 20, 50];

/** Главный экран: таблица групп с пагинацией, сортировкой и фильтрацией на сервере. */
export function GroupsPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [params, setParams] = useState<GroupListParams>({ page: 0, size: 10, sort: 'id', direction: 'asc' });
  const [filterField, setFilterField] = useState('name');
  const [filterInput, setFilterInput] = useState('');
  const [editId, setEditId] = useState<number | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<StudyGroupDto | null>(null);

  // Фильтр применяется с небольшой задержкой, чтобы не слать запрос на каждый символ
  useEffect(() => {
    const t = setTimeout(() => {
      setParams((p) => {
        const value = filterInput.trim();
        if (p.filterField === filterField && (p.filterValue ?? '') === value) return p;
        return { ...p, page: 0, filterField, filterValue: value };
      });
    }, 300);
    return () => clearTimeout(t);
  }, [filterField, filterInput]);

  const query = useQuery({
    queryKey: [...queryKeys.groups, params],
    queryFn: () => groupsApi.list(params),
    placeholderData: keepPreviousData,
  });

  const data = query.data;
  // Если после удаления (в т.ч. другим пользователем) текущая страница опустела — переходим на последнюю
  useEffect(() => {
    if (data && data.page > 0 && data.page >= data.totalPages) {
      setParams((p) => ({ ...p, page: Math.max(data.totalPages - 1, 0) }));
    }
  }, [data]);

  function toggleSort(key: string) {
    setParams((p) => ({
      ...p,
      page: 0,
      sort: key,
      direction: p.sort === key && p.direction === 'asc' ? 'desc' : 'asc',
    }));
  }

  return (
    <div className="stack">
      <div className="toolbar">
        <h1>Учебные группы</h1>
        <Link className="btn btn--primary" to="/groups/new">
          + Новая группа
        </Link>
      </div>

      <div className="toolbar toolbar--filters">
        <label className="inline">
          Фильтр по
          <select className="input" value={filterField} onChange={(e) => setFilterField(e.target.value)}>
            {COLUMNS.filter((c) => c.filterable).map((c) => (
              <option key={c.key} value={c.key}>
                {c.title}
              </option>
            ))}
          </select>
        </label>
        <input
          className="input"
          placeholder="Часть значения…"
          value={filterInput}
          onChange={(e) => setFilterInput(e.target.value)}
        />
        {filterInput && (
          <button className="btn btn--ghost" onClick={() => setFilterInput('')}>
            Сбросить
          </button>
        )}
        <span className="spacer" />
        <label className="inline">
          На странице
          <select
            className="input"
            value={params.size}
            onChange={(e) => setParams((p) => ({ ...p, page: 0, size: Number(e.target.value) }))}
          >
            {PAGE_SIZES.map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>
        </label>
      </div>

      {query.error && <ErrorBanner message={errorMessage(query.error)} />}

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              {COLUMNS.map((c) => (
                <th key={c.key} onClick={() => toggleSort(c.key)} className="sortable">
                  {c.title}
                  {params.sort === c.key && <span className="sort-mark">{params.direction === 'asc' ? ' ▲' : ' ▼'}</span>}
                </th>
              ))}
              <th>Действия</th>
            </tr>
          </thead>
          <tbody>
            {data?.content.map((g) => (
              <tr key={g.id}>
                {COLUMNS.map((c) => (
                  <td key={c.key}>{c.render(g)}</td>
                ))}
                <td className="row-actions">
                  <Link className="btn btn--small" to={`/groups/${g.id}`}>
                    Открыть
                  </Link>
                  <button className="btn btn--small" onClick={() => setEditId(g.id)}>
                    Изменить
                  </button>
                  <button className="btn btn--small btn--danger" onClick={() => setDeleteTarget(g)}>
                    Удалить
                  </button>
                </td>
              </tr>
            ))}
            {data && data.content.length === 0 && (
              <tr>
                <td colSpan={COLUMNS.length + 1} className="muted center">
                  Ничего не найдено
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {data && (
        <div className="pagination">
          <button
            className="btn btn--small"
            disabled={params.page === 0}
            onClick={() => setParams((p) => ({ ...p, page: p.page - 1 }))}
          >
            ← Назад
          </button>
          <span>
            Страница {data.totalPages === 0 ? 0 : data.page + 1} из {data.totalPages} · всего {data.totalElements}
          </span>
          <button
            className="btn btn--small"
            disabled={data.page + 1 >= data.totalPages}
            onClick={() => setParams((p) => ({ ...p, page: p.page + 1 }))}
          >
            Вперёд →
          </button>
        </div>
      )}

      {editId !== null && <EditGroupDialog id={editId} onClose={() => setEditId(null)} />}
      {deleteTarget && (
        <ConfirmDialog
          title="Удаление группы"
          text={`Удалить группу «${deleteTarget.name}» (#${deleteTarget.id})? Связанные координаты и администратор останутся в системе.`}
          onClose={() => setDeleteTarget(null)}
          onConfirm={async () => {
            await groupsApi.remove(deleteTarget.id);
            await queryClient.invalidateQueries();
            toast('Группа удалена');
          }}
        />
      )}
    </div>
  );
}
