import type {
  Color,
  CoordinatesDto,
  CoordinatesRequest,
  Country,
  FormOfEducation,
  LocationDto,
  LocationRequest,
  PersonDto,
  PersonRequest,
  Semester,
  StudyGroupDto,
  StudyGroupRequest,
} from '../api/types';

export type Errors = Record<string, string>;

export type RefMode = 'existing' | 'new' | 'none';

export interface CoordinatesForm {
  x: string;
  y: string;
}

export interface LocationForm {
  x: string;
  y: string;
  name: string;
}

export interface PersonForm {
  name: string;
  eyeColor: Color | '';
  hairColor: Color | '';
  locationMode: RefMode;
  locationId: string;
  location: LocationForm;
  weight: string;
  passportId: string;
  nationality: Country | '';
}

export interface GroupForm {
  name: string;
  coordinatesMode: 'existing' | 'new';
  coordinatesId: string;
  coordinates: CoordinatesForm;
  studentsCount: string;
  expelledStudents: string;
  transferredStudents: string;
  formOfEducation: FormOfEducation | '';
  shouldBeExpelled: string;
  averageMark: string;
  semesterEnum: Semester | '';
  adminMode: RefMode;
  groupAdminId: string;
  groupAdmin: PersonForm;
}

// ---------- пустые формы и заполнение из DTO ----------

export const emptyCoordinates = (): CoordinatesForm => ({ x: '', y: '' });
export const emptyLocation = (): LocationForm => ({ x: '', y: '', name: '' });

export const emptyPerson = (): PersonForm => ({
  name: '',
  eyeColor: '',
  hairColor: '',
  locationMode: 'none',
  locationId: '',
  location: emptyLocation(),
  weight: '',
  passportId: '',
  nationality: '',
});

export const emptyGroup = (): GroupForm => ({
  name: '',
  coordinatesMode: 'new',
  coordinatesId: '',
  coordinates: emptyCoordinates(),
  studentsCount: '',
  expelledStudents: '',
  transferredStudents: '',
  formOfEducation: '',
  shouldBeExpelled: '',
  averageMark: '',
  semesterEnum: '',
  adminMode: 'none',
  groupAdminId: '',
  groupAdmin: emptyPerson(),
});

export const coordinatesToForm = (c: CoordinatesDto): CoordinatesForm => ({ x: String(c.x), y: String(c.y) });

export const locationToForm = (l: LocationDto): LocationForm => ({ x: String(l.x), y: String(l.y), name: l.name });

export const personToForm = (p: PersonDto): PersonForm => ({
  name: p.name,
  eyeColor: p.eyeColor ?? '',
  hairColor: p.hairColor,
  locationMode: p.location ? 'existing' : 'none',
  locationId: p.location ? String(p.location.id) : '',
  location: emptyLocation(),
  weight: String(p.weight),
  passportId: p.passportId ?? '',
  nationality: p.nationality ?? '',
});

export const groupToForm = (g: StudyGroupDto): GroupForm => ({
  name: g.name,
  coordinatesMode: 'existing',
  coordinatesId: String(g.coordinates.id),
  coordinates: emptyCoordinates(),
  studentsCount: String(g.studentsCount),
  expelledStudents: String(g.expelledStudents),
  transferredStudents: String(g.transferredStudents),
  formOfEducation: g.formOfEducation,
  shouldBeExpelled: String(g.shouldBeExpelled),
  averageMark: String(g.averageMark),
  semesterEnum: g.semesterEnum,
  adminMode: g.groupAdmin ? 'existing' : 'none',
  groupAdminId: g.groupAdmin ? String(g.groupAdmin.id) : '',
  groupAdmin: emptyPerson(),
});

const INT_MAX = 2147483647;
const INT_MIN = -2147483648;
const FLOAT_MAX = 3.4028234663852886e38;

type NumKind = 'int' | 'long' | 'float' | 'double';

const NUMBER_RE = /^[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?$/;

function readNumber(
  errors: Errors,
  field: string,
  raw: string,
  kind: NumKind,
  opts: { positive?: boolean } = {},
): number | null {
  const s = raw.trim().replace(',', '.');
  if (s === '') {
    errors[field] = 'Поле обязательно';
    return null;
  }
  if (!NUMBER_RE.test(s)) {
    errors[field] = kind === 'int' || kind === 'long' ? 'Введите целое число' : 'Введите число';
    return null;
  }
  const v = Number(s);
  if (!Number.isFinite(v)) {
    errors[field] = 'Слишком большое число';
    return null;
  }
  if ((kind === 'int' || kind === 'long') && !Number.isInteger(v)) {
    errors[field] = 'Введите целое число';
    return null;
  }
  if (kind === 'int' && (v > INT_MAX || v < INT_MIN)) {
    errors[field] = `Число должно быть в диапазоне ${INT_MIN}..${INT_MAX}`;
    return null;
  }
  if (kind === 'long' && !Number.isSafeInteger(v)) {
    errors[field] = `Число должно быть не больше ${Number.MAX_SAFE_INTEGER}`;
    return null;
  }
  if (kind === 'float' && Math.abs(v) > FLOAT_MAX) {
    errors[field] = 'Число выходит за диапазон float';
    return null;
  }
  if (opts.positive && v <= 0) {
    errors[field] = 'Значение должно быть больше 0';
    return null;
  }
  return v;
}

function readId(errors: Errors, field: string, raw: string, what: string): number | null {
  if (raw === '') {
    errors[field] = `Выберите ${what}`;
    return null;
  }
  return Number(raw);
}

const join = (prefix: string, field: string) => (prefix ? `${prefix}.${field}` : field);

// ---------- сборка запросов ----------

export function buildCoordinates(errors: Errors, prefix: string, f: CoordinatesForm): CoordinatesRequest {
  return {
    x: readNumber(errors, join(prefix, 'x'), f.x, 'float') ?? 0,
    y: readNumber(errors, join(prefix, 'y'), f.y, 'double') ?? 0,
  };
}

export function buildLocation(errors: Errors, prefix: string, f: LocationForm): LocationRequest {
  return {
    x: readNumber(errors, join(prefix, 'x'), f.x, 'float') ?? 0,
    y: readNumber(errors, join(prefix, 'y'), f.y, 'double') ?? 0,
    name: f.name,
  };
}

export function buildPerson(errors: Errors, prefix: string, f: PersonForm): PersonRequest {
  if (f.name.trim() === '') errors[join(prefix, 'name')] = 'Имя не может быть пустым';
  if (f.hairColor === '') errors[join(prefix, 'hairColor')] = 'Выберите цвет волос';

  let locationId: number | null = null;
  let location: LocationRequest | null = null;
  if (f.locationMode === 'existing') {
    locationId = readId(errors, join(prefix, 'locationId'), f.locationId, 'локацию');
  } else if (f.locationMode === 'new') {
    location = buildLocation(errors, join(prefix, 'location'), f.location);
  }

  return {
    name: f.name.trim(),
    eyeColor: f.eyeColor || null,
    hairColor: (f.hairColor || 'GREEN') as Color,
    locationId,
    location,
    weight: readNumber(errors, join(prefix, 'weight'), f.weight, 'float', { positive: true }) ?? 0,
    passportId: f.passportId.trim() || null,
    nationality: f.nationality || null,
  };
}

export function buildGroup(f: GroupForm): { request: StudyGroupRequest; errors: Errors } {
  const errors: Errors = {};
  if (f.name.trim() === '') errors.name = 'Название не может быть пустым';
  if (f.formOfEducation === '') errors.formOfEducation = 'Выберите форму обучения';
  if (f.semesterEnum === '') errors.semesterEnum = 'Выберите семестр';

  let coordinatesId: number | null = null;
  let coordinates: CoordinatesRequest | null = null;
  if (f.coordinatesMode === 'existing') {
    coordinatesId = readId(errors, 'coordinatesId', f.coordinatesId, 'координаты');
  } else {
    coordinates = buildCoordinates(errors, 'coordinates', f.coordinates);
  }

  let groupAdminId: number | null = null;
  let groupAdmin: PersonRequest | null = null;
  if (f.adminMode === 'existing') {
    groupAdminId = readId(errors, 'groupAdminId', f.groupAdminId, 'администратора');
  } else if (f.adminMode === 'new') {
    groupAdmin = buildPerson(errors, 'groupAdmin', f.groupAdmin);
  }

  const request: StudyGroupRequest = {
    name: f.name.trim(),
    coordinatesId,
    coordinates,
    studentsCount: readNumber(errors, 'studentsCount', f.studentsCount, 'int', { positive: true }) ?? 0,
    expelledStudents: readNumber(errors, 'expelledStudents', f.expelledStudents, 'int', { positive: true }) ?? 0,
    transferredStudents:
      readNumber(errors, 'transferredStudents', f.transferredStudents, 'long', { positive: true }) ?? 0,
    formOfEducation: (f.formOfEducation || 'FULL_TIME_EDUCATION') as FormOfEducation,
    shouldBeExpelled: readNumber(errors, 'shouldBeExpelled', f.shouldBeExpelled, 'long', { positive: true }) ?? 0,
    averageMark: readNumber(errors, 'averageMark', f.averageMark, 'long', { positive: true }) ?? 0,
    semesterEnum: (f.semesterEnum || 'THIRD') as Semester,
    groupAdminId,
    groupAdmin,
  };
  return { request, errors };
}

export function hasErrors(errors: Errors): boolean {
  return Object.keys(errors).length > 0;
}
