import { useQuery } from '@tanstack/react-query';
import { coordinatesApi, locationsApi, personsApi, queryKeys } from '../api/endpoints';
import { COLORS, COUNTRIES, type CoordinatesDto, type LocationDto, type PersonDto } from '../api/types';
import type { CoordinatesForm, Errors, LocationForm, PersonForm, RefMode } from '../forms/model';
import { EnumSelect, Field, TextInput } from './ui';

const join = (prefix: string, field: string) => (prefix ? `${prefix}.${field}` : field);

export const coordinatesLabel = (c: CoordinatesDto) => `#${c.id} (${c.x}; ${c.y})`;
export const locationLabel = (l: LocationDto) => `#${l.id} ${l.name || '(без названия)'} (${l.x}; ${l.y})`;
export const personLabel = (p: PersonDto) => `#${p.id} ${p.name}`;

export function CoordinatesFields(props: {
  value: CoordinatesForm;
  onChange: (v: CoordinatesForm) => void;
  errors: Errors;
  prefix: string;
}) {
  const { value, onChange, errors, prefix } = props;
  return (
    <div className="grid grid--2">
      <Field label="X (float)" error={errors[join(prefix, 'x')]}>
        <TextInput inputMode="decimal" value={value.x} onChange={(x) => onChange({ ...value, x })} />
      </Field>
      <Field label="Y (double)" error={errors[join(prefix, 'y')]}>
        <TextInput inputMode="decimal" value={value.y} onChange={(y) => onChange({ ...value, y })} />
      </Field>
    </div>
  );
}

export function LocationFields(props: {
  value: LocationForm;
  onChange: (v: LocationForm) => void;
  errors: Errors;
  prefix: string;
}) {
  const { value, onChange, errors, prefix } = props;
  return (
    <div className="grid grid--3">
      <Field label="Название" error={errors[join(prefix, 'name')]}>
        <TextInput value={value.name} onChange={(name) => onChange({ ...value, name })} />
      </Field>
      <Field label="X (float)" error={errors[join(prefix, 'x')]}>
        <TextInput inputMode="decimal" value={value.x} onChange={(x) => onChange({ ...value, x })} />
      </Field>
      <Field label="Y (double)" error={errors[join(prefix, 'y')]}>
        <TextInput inputMode="decimal" value={value.y} onChange={(y) => onChange({ ...value, y })} />
      </Field>
    </div>
  );
}

/** Переключатель «существующий / новый / не задан» для связанного объекта. */
export function RefModeSwitch(props: {
  value: RefMode;
  onChange: (m: RefMode) => void;
  allowNone: boolean;
  name: string;
}) {
  const modes: { mode: RefMode; text: string }[] = [
    { mode: 'existing', text: 'Выбрать существующий' },
    { mode: 'new', text: 'Создать новый' },
  ];
  if (props.allowNone) modes.push({ mode: 'none', text: 'Не задан' });
  return (
    <div className="segmented" role="radiogroup">
      {modes.map(({ mode, text }) => (
        <label key={mode} className={`segmented__item${props.value === mode ? ' is-active' : ''}`}>
          <input
            type="radio"
            name={props.name}
            checked={props.value === mode}
            onChange={() => props.onChange(mode)}
          />
          {text}
        </label>
      ))}
    </div>
  );
}

export function ExistingSelect<T extends { id: number }>(props: {
  items: T[] | undefined;
  value: string;
  onChange: (id: string) => void;
  format: (item: T) => string;
  excludeId?: number;
}) {
  const items = (props.items ?? []).filter((i) => i.id !== props.excludeId);
  return (
    <select className="input" value={props.value} onChange={(e) => props.onChange(e.target.value)}>
      <option value="">{items.length ? '— выберите —' : '— в системе пока нет объектов —'}</option>
      {items.map((i) => (
        <option key={i.id} value={i.id}>
          {props.format(i)}
        </option>
      ))}
    </select>
  );
}

export function useCoordinatesList() {
  return useQuery({ queryKey: queryKeys.coordinates, queryFn: coordinatesApi.list });
}

export function useLocationsList() {
  return useQuery({ queryKey: queryKeys.locations, queryFn: locationsApi.list });
}

export function usePersonsList() {
  return useQuery({ queryKey: queryKeys.persons, queryFn: personsApi.list });
}

export function PersonFields(props: {
  value: PersonForm;
  onChange: (v: PersonForm) => void;
  errors: Errors;
  prefix: string;
}) {
  const { value, onChange, errors, prefix } = props;
  const locations = useLocationsList();
  const set = <K extends keyof PersonForm>(key: K, v: PersonForm[K]) => onChange({ ...value, [key]: v });

  return (
    <div className="stack">
      <div className="grid grid--3">
        <Field label="Имя *" error={errors[join(prefix, 'name')]}>
          <TextInput value={value.name} onChange={(v) => set('name', v)} />
        </Field>
        <Field label="Вес (> 0) *" error={errors[join(prefix, 'weight')]}>
          <TextInput inputMode="decimal" value={value.weight} onChange={(v) => set('weight', v)} />
        </Field>
        <Field label="Номер паспорта" error={errors[join(prefix, 'passportId')]}>
          <TextInput value={value.passportId} onChange={(v) => set('passportId', v)} />
        </Field>
        <Field label="Цвет глаз" error={errors[join(prefix, 'eyeColor')]}>
          <EnumSelect value={value.eyeColor} options={COLORS} emptyLabel="— не задан —" onChange={(v) => set('eyeColor', v)} />
        </Field>
        <Field label="Цвет волос *" error={errors[join(prefix, 'hairColor')]}>
          <EnumSelect value={value.hairColor} options={COLORS} onChange={(v) => set('hairColor', v)} />
        </Field>
        <Field label="Национальность" error={errors[join(prefix, 'nationality')]}>
          <EnumSelect
            value={value.nationality}
            options={COUNTRIES}
            emptyLabel="— не задана —"
            onChange={(v) => set('nationality', v)}
          />
        </Field>
      </div>

      <fieldset className="subform">
        <legend>Локация</legend>
        <RefModeSwitch
          name={`${prefix || 'person'}-location-mode`}
          value={value.locationMode}
          allowNone
          onChange={(m) => set('locationMode', m)}
        />
        {errors[join(prefix, 'location')] && <div className="field__error">{errors[join(prefix, 'location')]}</div>}
        {value.locationMode === 'existing' && (
          <Field label="Локация" error={errors[join(prefix, 'locationId')]}>
            <ExistingSelect
              items={locations.data}
              value={value.locationId}
              format={locationLabel}
              onChange={(v) => set('locationId', v)}
            />
          </Field>
        )}
        {value.locationMode === 'new' && (
          <LocationFields
            value={value.location}
            onChange={(v) => set('location', v)}
            errors={errors}
            prefix={join(prefix, 'location')}
          />
        )}
      </fieldset>
    </div>
  );
}
