import { useState, type FormEvent } from 'react';
import { ApiError, errorMessage } from '../api/client';
import { FORMS_OF_EDUCATION, SEMESTERS, type StudyGroupRequest } from '../api/types';
import { buildGroup, hasErrors, type Errors, type GroupForm as GroupFormState } from '../forms/model';
import {
  CoordinatesFields,
  ExistingSelect,
  PersonFields,
  RefModeSwitch,
  coordinatesLabel,
  personLabel,
  useCoordinatesList,
  usePersonsList,
} from './EntityFields';
import { EnumSelect, ErrorBanner, Field, TextInput } from './ui';

/**
 * Форма группы — общая для создания и редактирования.
 * Связанные объекты (координаты, администратор, его локация) можно выбрать из существующих или создать новые.
 */
export function GroupForm(props: {
  initial: GroupFormState;
  submitLabel: string;
  onSubmit: (request: StudyGroupRequest) => Promise<void>;
  onCancel?: () => void;
}) {
  const [form, setForm] = useState<GroupFormState>(props.initial);
  const [errors, setErrors] = useState<Errors>({});
  const [message, setMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const coordinates = useCoordinatesList();
  const persons = usePersonsList();

  const set = <K extends keyof GroupFormState>(key: K, value: GroupFormState[K]) =>
    setForm((f) => ({ ...f, [key]: value }));

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const { request, errors: clientErrors } = buildGroup(form);
    setErrors(clientErrors);
    if (hasErrors(clientErrors)) {
      setMessage('Исправьте ошибки в отмеченных полях');
      return;
    }
    setMessage(null);
    setSubmitting(true);
    try {
      await props.onSubmit(request);
    } catch (err) {
      if (err instanceof ApiError) setErrors(err.fieldErrors);
      setMessage(errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="stack" onSubmit={handleSubmit} noValidate>
      <ErrorBanner message={message} />

      <fieldset className="subform">
        <legend>Группа</legend>
        <div className="grid grid--3">
          <Field label="Название *" error={errors.name}>
            <TextInput value={form.name} onChange={(v) => set('name', v)} />
          </Field>
          <Field label="Форма обучения *" error={errors.formOfEducation}>
            <EnumSelect value={form.formOfEducation} options={FORMS_OF_EDUCATION} onChange={(v) => set('formOfEducation', v)} />
          </Field>
          <Field label="Семестр *" error={errors.semesterEnum}>
            <EnumSelect value={form.semesterEnum} options={SEMESTERS} onChange={(v) => set('semesterEnum', v)} />
          </Field>
          <Field label="Кол-во студентов (int > 0) *" error={errors.studentsCount}>
            <TextInput inputMode="numeric" value={form.studentsCount} onChange={(v) => set('studentsCount', v)} />
          </Field>
          <Field label="Отчислено (int > 0) *" error={errors.expelledStudents}>
            <TextInput inputMode="numeric" value={form.expelledStudents} onChange={(v) => set('expelledStudents', v)} />
          </Field>
          <Field label="Переведено (long > 0) *" error={errors.transferredStudents}>
            <TextInput inputMode="numeric" value={form.transferredStudents} onChange={(v) => set('transferredStudents', v)} />
          </Field>
          <Field label="Должны быть отчислены (long > 0) *" error={errors.shouldBeExpelled}>
            <TextInput inputMode="numeric" value={form.shouldBeExpelled} onChange={(v) => set('shouldBeExpelled', v)} />
          </Field>
          <Field label="Средняя оценка (long > 0) *" error={errors.averageMark}>
            <TextInput inputMode="numeric" value={form.averageMark} onChange={(v) => set('averageMark', v)} />
          </Field>
        </div>
      </fieldset>

      <fieldset className="subform">
        <legend>Координаты *</legend>
        <RefModeSwitch
          name="coordinates-mode"
          value={form.coordinatesMode}
          allowNone={false}
          onChange={(m) => set('coordinatesMode', m === 'existing' ? 'existing' : 'new')}
        />
        {errors.coordinates && <div className="field__error">{errors.coordinates}</div>}
        {form.coordinatesMode === 'existing' ? (
          <Field label="Координаты" error={errors.coordinatesId}>
            <ExistingSelect
              items={coordinates.data}
              value={form.coordinatesId}
              format={coordinatesLabel}
              onChange={(v) => set('coordinatesId', v)}
            />
          </Field>
        ) : (
          <CoordinatesFields
            value={form.coordinates}
            onChange={(v) => set('coordinates', v)}
            errors={errors}
            prefix="coordinates"
          />
        )}
      </fieldset>

      <fieldset className="subform">
        <legend>Администратор группы</legend>
        <RefModeSwitch name="admin-mode" value={form.adminMode} allowNone onChange={(m) => set('adminMode', m)} />
        {errors.groupAdmin && <div className="field__error">{errors.groupAdmin}</div>}
        {form.adminMode === 'existing' && (
          <Field label="Человек" error={errors.groupAdminId}>
            <ExistingSelect
              items={persons.data}
              value={form.groupAdminId}
              format={personLabel}
              onChange={(v) => set('groupAdminId', v)}
            />
          </Field>
        )}
        {form.adminMode === 'new' && (
          <PersonFields
            value={form.groupAdmin}
            onChange={(v) => set('groupAdmin', v)}
            errors={errors}
            prefix="groupAdmin"
          />
        )}
      </fieldset>

      <div className="actions">
        {props.onCancel && (
          <button type="button" className="btn" onClick={props.onCancel}>
            Отмена
          </button>
        )}
        <button type="submit" className="btn btn--primary" disabled={submitting}>
          {submitting ? 'Сохранение…' : props.submitLabel}
        </button>
      </div>
    </form>
  );
}
