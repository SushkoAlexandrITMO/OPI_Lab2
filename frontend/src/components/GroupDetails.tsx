import { label, type StudyGroupDto } from '../api/types';

export const formatDate = (iso: string) => new Date(iso).toLocaleString('ru-RU');

function Row({ name, value }: { name: string; value: React.ReactNode }) {
  return (
    <>
      <dt>{name}</dt>
      <dd>{value ?? '—'}</dd>
    </>
  );
}

/** Карточка группы вместе со связанными объектами (координаты, администратор, его локация). */
export function GroupDetails({ group }: { group: StudyGroupDto }) {
  const admin = group.groupAdmin;
  return (
    <div className="cards">
      <section className="card">
        <h3>Группа #{group.id}</h3>
        <dl className="props">
          <Row name="Название" value={group.name} />
          <Row name="Дата создания" value={formatDate(group.creationDate)} />
          <Row name="Студентов" value={group.studentsCount} />
          <Row name="Отчислено" value={group.expelledStudents} />
          <Row name="Переведено" value={group.transferredStudents} />
          <Row name="Должны быть отчислены" value={group.shouldBeExpelled} />
          <Row name="Средняя оценка" value={group.averageMark} />
          <Row name="Форма обучения" value={label(group.formOfEducation)} />
          <Row name="Семестр" value={label(group.semesterEnum)} />
        </dl>
      </section>

      <section className="card">
        <h3>Координаты #{group.coordinates.id}</h3>
        <dl className="props">
          <Row name="X" value={group.coordinates.x} />
          <Row name="Y" value={group.coordinates.y} />
        </dl>
      </section>

      <section className="card">
        <h3>Администратор{admin ? ` #${admin.id}` : ''}</h3>
        {admin ? (
          <dl className="props">
            <Row name="Имя" value={admin.name} />
            <Row name="Цвет глаз" value={label(admin.eyeColor)} />
            <Row name="Цвет волос" value={label(admin.hairColor)} />
            <Row name="Вес" value={admin.weight} />
            <Row name="Паспорт" value={admin.passportId} />
            <Row name="Национальность" value={label(admin.nationality)} />
          </dl>
        ) : (
          <p className="muted">Не назначен</p>
        )}
      </section>

      {admin?.location && (
        <section className="card">
          <h3>Локация администратора #{admin.location.id}</h3>
          <dl className="props">
            <Row name="Название" value={admin.location.name} />
            <Row name="X" value={admin.location.x} />
            <Row name="Y" value={admin.location.y} />
          </dl>
        </section>
      )}
    </div>
  );
}
