import { useState } from 'react';
import { ApiError, errorMessage } from '../api/client';
import type { DeleteOptions } from '../api/endpoints';
import { ErrorBanner, Field, Modal } from './ui';

/**
 * Удаление вспомогательного объекта. Сначала пробуем удалить как есть; если сервер ответил,
 * что объект используется (409 REFERENCED), предлагаем выбрать объект-замену
 * или отвязать ссылки (если связь допускает null).
 */
export function DeleteDialog<T extends { id: number }>(props: {
  title: string;
  target: T;
  describe: (item: T) => string;
  candidates: T[];
  onDelete: (options?: DeleteOptions) => Promise<void>;
  onClose: () => void;
}) {
  const [referenced, setReferenced] = useState<{ message: string; unlinkAllowed: boolean } | null>(null);
  const [choice, setChoice] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const replacements = props.candidates.filter((c) => c.id !== props.target.id);

  async function run(options?: DeleteOptions) {
    setBusy(true);
    setError(null);
    try {
      await props.onDelete(options);
      props.onClose();
    } catch (err) {
      if (err instanceof ApiError && err.code === 'REFERENCED' && err.details.unlinkAllowed !== undefined) {
        setReferenced({ message: err.message, unlinkAllowed: Boolean(err.details.unlinkAllowed) });
      } else {
        setError(errorMessage(err));
      }
    } finally {
      setBusy(false);
    }
  }

  function confirmReassign() {
    if (choice === '') {
      setError('Выберите вариант');
      return;
    }
    run(choice === 'unlink' ? { unlink: true } : { replacementId: Number(choice) });
  }

  return (
    <Modal title={props.title} onClose={props.onClose}>
      <div className="stack">
        <ErrorBanner message={error} />
        {!referenced ? (
          <>
            <p>
              Удалить <b>{props.describe(props.target)}</b>?
            </p>
            <div className="actions">
              <button className="btn" onClick={props.onClose}>
                Отмена
              </button>
              <button className="btn btn--danger" disabled={busy} onClick={() => run()}>
                Удалить
              </button>
            </div>
          </>
        ) : (
          <>
            <div className="banner banner--warn">{referenced.message}</div>
            <Field label="Что сделать со связанными объектами">
              <select className="input" value={choice} onChange={(e) => setChoice(e.target.value)}>
                <option value="">— выберите —</option>
                {referenced.unlinkAllowed && <option value="unlink">Отвязать (оставить поле пустым)</option>}
                {replacements.map((c) => (
                  <option key={c.id} value={c.id}>
                    Перепривязать к {props.describe(c)}
                  </option>
                ))}
              </select>
            </Field>
            {!referenced.unlinkAllowed && replacements.length === 0 && (
              <p className="muted">Нет объектов для перепривязки — сначала создайте ещё один.</p>
            )}
            <div className="actions">
              <button className="btn" onClick={props.onClose}>
                Отмена
              </button>
              <button className="btn btn--danger" disabled={busy} onClick={confirmReassign}>
                Перепривязать и удалить
              </button>
            </div>
          </>
        )}
      </div>
    </Modal>
  );
}

/** Простое подтверждение удаления группы (на группу никто не ссылается). */
export function ConfirmDialog(props: {
  title: string;
  text: string;
  onConfirm: () => Promise<void>;
  onClose: () => void;
}) {
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  return (
    <Modal title={props.title} onClose={props.onClose}>
      <div className="stack">
        <ErrorBanner message={error} />
        <p>{props.text}</p>
        <div className="actions">
          <button className="btn" onClick={props.onClose}>
            Отмена
          </button>
          <button
            className="btn btn--danger"
            disabled={busy}
            onClick={async () => {
              setBusy(true);
              try {
                await props.onConfirm();
                props.onClose();
              } catch (err) {
                setError(errorMessage(err));
              } finally {
                setBusy(false);
              }
            }}
          >
            Удалить
          </button>
        </div>
      </div>
    </Modal>
  );
}
