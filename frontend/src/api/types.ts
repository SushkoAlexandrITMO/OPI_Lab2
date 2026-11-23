export const COLORS = ['GREEN', 'WHITE', 'BROWN'] as const;
export type Color = (typeof COLORS)[number];

export const COUNTRIES = ['USA', 'FRANCE', 'SPAIN', 'VATICAN', 'THAILAND'] as const;
export type Country = (typeof COUNTRIES)[number];

export const FORMS_OF_EDUCATION = ['DISTANCE_EDUCATION', 'FULL_TIME_EDUCATION', 'EVENING_CLASSES'] as const;
export type FormOfEducation = (typeof FORMS_OF_EDUCATION)[number];

export const SEMESTERS = ['THIRD', 'FOURTH', 'SIXTH', 'SEVENTH', 'EIGHTH'] as const;
export type Semester = (typeof SEMESTERS)[number];

export interface CoordinatesDto {
  id: number;
  x: number;
  y: number;
  version: number;
}

export interface LocationDto {
  id: number;
  x: number;
  y: number;
  name: string;
  version: number;
}

export interface PersonDto {
  id: number;
  name: string;
  eyeColor: Color | null;
  hairColor: Color;
  location: LocationDto | null;
  weight: number;
  passportId: string | null;
  nationality: Country | null;
  version: number;
}

export interface StudyGroupDto {
  id: number;
  name: string;
  coordinates: CoordinatesDto;
  creationDate: string;
  studentsCount: number;
  expelledStudents: number;
  transferredStudents: number;
  formOfEducation: FormOfEducation;
  shouldBeExpelled: number;
  averageMark: number;
  semesterEnum: Semester;
  groupAdmin: PersonDto | null;
  version: number;
}

export interface CoordinatesRequest {
  x: number;
  y: number;
  version?: number;
}

export interface LocationRequest {
  x: number;
  y: number;
  name: string;
  version?: number;
}

export interface PersonRequest {
  name: string;
  eyeColor: Color | null;
  hairColor: Color;
  locationId: number | null;
  location: LocationRequest | null;
  weight: number;
  passportId: string | null;
  nationality: Country | null;
  version?: number;
}

export interface StudyGroupRequest {
  name: string;
  coordinatesId: number | null;
  coordinates: CoordinatesRequest | null;
  studentsCount: number;
  expelledStudents: number;
  transferredStudents: number;
  formOfEducation: FormOfEducation;
  shouldBeExpelled: number;
  averageMark: number;
  semesterEnum: Semester;
  groupAdminId: number | null;
  groupAdmin: PersonRequest | null;
  version?: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface IdCount {
  id: number;
  count: number;
}

export interface EntityChangedEvent {
  entity: 'STUDY_GROUP' | 'PERSON' | 'COORDINATES' | 'LOCATION';
  action: 'CREATED' | 'UPDATED' | 'DELETED';
  id: number | null;
}

export const LABELS: Record<string, string> = {
  GREEN: 'Зелёный',
  WHITE: 'Белый',
  BROWN: 'Коричневый',
  USA: 'США',
  FRANCE: 'Франция',
  SPAIN: 'Испания',
  VATICAN: 'Ватикан',
  THAILAND: 'Таиланд',
  DISTANCE_EDUCATION: 'Дистанционная',
  FULL_TIME_EDUCATION: 'Очная',
  EVENING_CLASSES: 'Вечерняя',
  THIRD: '3',
  FOURTH: '4',
  SIXTH: '6',
  SEVENTH: '7',
  EIGHTH: '8',
};

export function label(value: string | null | undefined): string {
  if (value == null) return '—';
  return LABELS[value] ? `${LABELS[value]} (${value})` : value;
}
