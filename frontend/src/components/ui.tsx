import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { label } from '../api/types';

export function Field({ label: text, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <label className={`field${error ? ' field--error' : ''}`}>
      <span className="field__label">{text}</span>
      {children}
      {error && <span className="field__error">{error}</span>}
    </label>
  );
}

export function TextInput(props: {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  inputMode?: 'text' | 'numeric' | 'decimal';
}) {
  return (
    <input
      className="input"
      value={props.value}
      placeholder={props.placeholder}
      inputMode={props.inputMode}
      onChange={(e) => props.onChange(e.target.value)}
    />
  );
}

/** Выпадающий список значений перечисления; allowEmpty — для полей, допускающих null. */
export function EnumSelect<T extends string>(props: {
  value: T | '';
  options: readonly T[];
  onChange: (value: T | '') => void;
  emptyLabel?: string;
}) {
  return (
    <select className="input" value={props.value} onChange={(e) => props.onChange(e.target.value as T | '')}>
      <option value="">{props.emptyLabel ?? '— выберите —'}</option>
      {props.options.map((o) => (
        <option key={o} value={o}>
          {label(o)}
        </option>
      ))}
    </select>
  );
}

export function ErrorBanner({ message }: { message?: string | null }) {
  if (!message) return null;
  return <div className="banner banner--error">{message}</div>;
}

export function Modal(props: { title: string; onClose: () => void; children: ReactNode; wide?: boolean }) {
  const { onClose } = props;
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  return (
    <div className="modal-backdrop" onMouseDown={props.onClose}>
      <div
        className={`modal${props.wide ? ' modal--wide' : ''}`}
        role="dialog"
        aria-modal="true"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <div className="modal__header">
          <h2>{props.title}</h2>
          <button className="btn btn--ghost" onClick={props.onClose} aria-label="Закрыть">
            ✕
          </button>
        </div>
        <div className="modal__body">{props.children}</div>
      </div>
    </div>
  );
}

// ---------- уведомления ----------

interface Toast {
  id: number;
  text: string;
  kind: 'ok' | 'error';
}

const ToastContext = createContext<(text: string, kind?: 'ok' | 'error') => void>(() => {});

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const push = useCallback((text: string, kind: 'ok' | 'error' = 'ok') => {
    const id = Date.now() + Math.random();
    setToasts((t) => [...t, { id, text, kind }]);
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 4000);
  }, []);

  return (
    <ToastContext.Provider value={push}>
      {children}
      <div className="toasts">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast--${t.kind}`}>
            {t.text}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export const useToast = () => useContext(ToastContext);
