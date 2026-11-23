import { request } from './client';
import type {
  CoordinatesDto,
  CoordinatesRequest,
  IdCount,
  LocationDto,
  LocationRequest,
  PageResponse,
  PersonDto,
  PersonRequest,
  StudyGroupDto,
  StudyGroupRequest,
} from './types';

export interface GroupListParams {
  page: number;
  size: number;
  sort: string;
  direction: 'asc' | 'desc';
  filterField?: string;
  filterValue?: string;
}

/** Параметры удаления вспомогательного объекта, на который есть ссылки. */
export interface DeleteOptions {
  replacementId?: number;
  unlink?: boolean;
}

function deleteQuery(options?: DeleteOptions): string {
  const params = new URLSearchParams();
  if (options?.replacementId != null) params.set('replacementId', String(options.replacementId));
  if (options?.unlink) params.set('unlink', 'true');
  const query = params.toString();
  return query ? `?${query}` : '';
}

export const groupsApi = {
  list(p: GroupListParams) {
    const params = new URLSearchParams({
      page: String(p.page),
      size: String(p.size),
      sort: p.sort,
      direction: p.direction,
    });
    if (p.filterField && p.filterValue) {
      params.set('filterField', p.filterField);
      params.set('filterValue', p.filterValue);
    }
    return request<PageResponse<StudyGroupDto>>('GET', `/api/study-groups?${params}`);
  },
  get: (id: number) => request<StudyGroupDto>('GET', `/api/study-groups/${id}`),
  create: (body: StudyGroupRequest) => request<StudyGroupDto>('POST', '/api/study-groups', body),
  update: (id: number, body: StudyGroupRequest) => request<StudyGroupDto>('PUT', `/api/study-groups/${id}`, body),
  remove: (id: number) => request<void>('DELETE', `/api/study-groups/${id}`),
};

export const personsApi = {
  list: () => request<PersonDto[]>('GET', '/api/persons'),
  create: (body: PersonRequest) => request<PersonDto>('POST', '/api/persons', body),
  update: (id: number, body: PersonRequest) => request<PersonDto>('PUT', `/api/persons/${id}`, body),
  remove: (id: number, options?: DeleteOptions) => request<void>('DELETE', `/api/persons/${id}${deleteQuery(options)}`),
};

export const coordinatesApi = {
  list: () => request<CoordinatesDto[]>('GET', '/api/coordinates'),
  create: (body: CoordinatesRequest) => request<CoordinatesDto>('POST', '/api/coordinates', body),
  update: (id: number, body: CoordinatesRequest) => request<CoordinatesDto>('PUT', `/api/coordinates/${id}`, body),
  remove: (id: number, options?: DeleteOptions) =>
    request<void>('DELETE', `/api/coordinates/${id}${deleteQuery(options)}`),
};

export const locationsApi = {
  list: () => request<LocationDto[]>('GET', '/api/locations'),
  create: (body: LocationRequest) => request<LocationDto>('POST', '/api/locations', body),
  update: (id: number, body: LocationRequest) => request<LocationDto>('PUT', `/api/locations/${id}`, body),
  remove: (id: number, options?: DeleteOptions) =>
    request<void>('DELETE', `/api/locations/${id}${deleteQuery(options)}`),
};

export const specialApi = {
  minExpelled: () => request<StudyGroupDto>('GET', '/api/special/min-expelled'),
  countById: () => request<IdCount[]>('GET', '/api/special/count-by-id'),
  adminGreaterThan: (personId: number) =>
    request<StudyGroupDto[]>('GET', `/api/special/admin-greater-than?personId=${personId}`),
  expelAll: (groupId: number) => request<StudyGroupDto>('POST', `/api/special/groups/${groupId}/expel-all`),
  addStudent: (groupId: number) => request<StudyGroupDto>('POST', `/api/special/groups/${groupId}/add-student`),
};

/** Ключи кэша TanStack Query. */
export const queryKeys = {
  groups: ['groups'] as const,
  group: (id: number) => ['group', id] as const,
  persons: ['persons'] as const,
  coordinates: ['coordinates'] as const,
  locations: ['locations'] as const,
};
