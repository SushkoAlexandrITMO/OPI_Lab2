import { useQueryClient } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { specialApi } from '../api/endpoints';
import { label, type IdCount, type StudyGroupDto } from '../api/types';
import { ExistingSelect, personLabel, usePersonsList } from '../components/EntityFields';
import { ErrorBanner, useToast } from '../components/ui';

function Block(props: { title: string; description: string; children: ReactNode }) {
  return (
    <section className="card">
      <h3>{props.title}</h3>
      <p className="muted">{props.description}</p>
      {props.children}
    </section>
  );
}

function GroupsTable({ groups }: { groups: StudyGroupDto[] }) {
  if (groups.length === 0) return <p className="muted">Нет подходящих групп</p>;
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Название</th>
            <th>Студентов</th>
            <th>Отчислено</th>
            <th>Форма обучения</th>
            <th>Админ</th>
          </tr>
        </thead>
        <tbody>
          {groups.map((g) => (
            <tr key={g.id}>
              <td>
                <Link to={`/groups/${g.id}`}>{g.id}</Link>
              </td>
              <td>{g.name}</td>
              <td>{g.studentsCount}</td>
              <td>{g.expelledStudents}</td>
              <td>{label(g.formOfEducation)}</td>
              <td>{g.groupAdmin ? `${g.groupAdmin.name} (вес ${g.groupAdmin.weight})` : '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function useOperation<T>() {
  const [result, setResult] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  async function run(action: () => Promise<T>) {
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      setResult(await action());
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }
  return { result, error, busy, run };
}

function parseGroupId(raw: string): number | null {
  const v = raw.trim();
  return /^\d+$/.test(v) && Number(v) > 0 && Number.isSafeInteger(Number(v)) ? Number(v) : null;
}

function GroupIdAction(props: {
  button: string;
  action: (id: number) => Promise<StudyGroupDto>;
  success: (g: StudyGroupDto) => string;
}) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [input, setInput] = useState('');
  const [inputError, setInputError] = useState<string | null>(null);
  const op = useOperation<StudyGroupDto>();

  return (
    <div className="stack">
      <div className="toolbar">
        <input className="input" placeholder="ID группы" value={input} onChange={(e) => setInput(e.target.value)} />
        <button
          className="btn btn--primary"
          disabled={op.busy}
          onClick={() => {
            const id = parseGroupId(input);
            if (id === null) {
              setInputError('ID должен быть положительным целым числом');
              return;
            }
            setInputError(null);
            op.run(async () => {
              const g = await props.action(id);
              await queryClient.invalidateQueries();
              toast(props.success(g));
              return g;
            });
          }}
        >
          {props.button}
        </button>
      </div>
      {inputError && <div className="field__error">{inputError}</div>}
      <ErrorBanner message={op.error} />
      {op.result && <GroupsTable groups={[op.result]} />}
    </div>
  );
}


export function SpecialPage() {
  const persons = usePersonsList();
  const minExpelled = useOperation<StudyGroupDto>();
  const countById = useOperation<IdCount[]>();
  const adminGreater = useOperation<StudyGroupDto[]>();
  const [personId, setPersonId] = useState('');
  const [personError, setPersonError] = useState<string | null>(null);

  return (
    <div className="stack">
      <h1>Специальные операции</h1>

      <Block title="Минимальное число отчисленных" description="Вернуть одну (любую) группу с минимальным количеством испепелённых.">
        <button className="btn btn--primary" disabled={minExpelled.busy} onClick={() => minExpelled.run(specialApi.minExpelled)}>
          Выполнить
        </button>
        <ErrorBanner message={minExpelled.error} />
        {minExpelled.result && <GroupsTable groups={[minExpelled.result]} />}
      </Block>

      <Block title="Группировка по id" description="Сгруппировать группы по id и вернуть количество элементов в каждой группе.">
        <button className="btn btn--primary" disabled={countById.busy} onClick={() => countById.run(specialApi.countById)}>
          Выполнить
        </button>
        <ErrorBanner message={countById.error} />
        {countById.result && (
          <div className="table-wrap">
            <table className="table table--narrow">
              <thead>
                <tr>
                  <th>id</th>
                  <th>Количество</th>
                </tr>
              </thead>
              <tbody>
                {countById.result.map((r) => (
                  <tr key={r.id}>
                    <td>{r.id}</td>
                    <td>{r.count}</td>
                  </tr>
                ))}
                {countById.result.length === 0 && (
                  <tr>
                    <td colSpan={2} className="muted center">
                      Групп нет
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </Block>

      <Block
        title="groupAdmin больше заданного"
        description="Вернуть массив объектов, значение поля groupAdmin которых больше заданного."
      >
        <div className="toolbar">
          <ExistingSelect items={persons.data} value={personId} format={personLabel} onChange={setPersonId} />
          <button
            className="btn btn--primary"
            disabled={adminGreater.busy}
            onClick={() => {
              if (!personId) {
                setPersonError('Выберите человека для сравнения');
                return;
              }
              setPersonError(null);
              adminGreater.run(() => specialApi.adminGreaterThan(Number(personId)));
            }}
          >
            Выполнить
          </button>
        </div>
        {personError && <div className="field__error">{personError}</div>}
        <ErrorBanner message={adminGreater.error} />
        {adminGreater.result && <GroupsTable groups={adminGreater.result} />}
      </Block>

      <Block
        title="Отчислить всех студентов"
        description="Все студенты группы добавляются к отчисленным; количество студентов и переведённых становится 0."
      >
        <GroupIdAction
          button="Отчислить всех"
          action={specialApi.expelAll}
          success={(g) => `Группа «${g.name}»: студентов 0, всего отчислено ${g.expelledStudents}`}
        />
      </Block>

      <Block title="Добавить студента" description="Добавить свежее мясо">
        <GroupIdAction
          button="Добавить студента"
          action={specialApi.addStudent}
          success={(g) => `В группе «${g.name}» теперь ${g.studentsCount} студентов`}
        />
      </Block>
    </div>
  );
}
