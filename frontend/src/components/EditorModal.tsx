import { useState, type FormEvent, type ReactNode } from 'react';
import { ApiError, errorMessage } from '../api/client';
import { hasErrors, type Errors } from '../forms/model';
import { ErrorBanner, Modal } from './ui';

/**
 * Модальное окно создания/редактирования вспомогательного объекта.
 * build — клиентская валидация и сборка запроса; submit — вызов API.
 */
export function EditorModal<F, R>(props: {
  title: string;
  initial: F;
  build: (form: F, errors: Errors) => R;
  submit: (request: R) => Promise<void>;
  onClose: () => void;
  children: (form: F, setForm: (f: F) => void, errors: Errors) => ReactNode;
}) {
  const [form, setForm] = useState<F>(props.initial);
  const [errors, setErrors] = useState<Errors>({});
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const clientErrors: Errors = {};
    const request = props.build(form, clientErrors);
    setErrors(clientErrors);
    if (hasErrors(clientErrors)) {
      setMessage('Исправьте ошибки в отмеченных полях');
      return;
    }
    setBusy(true);
    setMessage(null);
    try {
      await props.submit(request);
      props.onClose();
    } catch (err) {
      if (err instanceof ApiError) setErrors(err.fieldErrors);
      setMessage(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title={props.title} onClose={props.onClose} wide>
      <form className="stack" onSubmit={handleSubmit} noValidate>
        <ErrorBanner message={message} />
        {props.children(form, setForm, errors)}
        <div className="actions">
          <button type="button" className="btn" onClick={props.onClose}>
            Отмена
          </button>
          <button type="submit" className="btn btn--primary" disabled={busy}>
            {busy ? 'Сохранение…' : 'Сохранить'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
