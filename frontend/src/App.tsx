import { NavLink, Route, Routes } from 'react-router-dom';
import { CoordinatesPage } from './pages/CoordinatesPage';
import { GroupCreatePage } from './pages/GroupCreatePage';
import { GroupsPage } from './pages/GroupsPage';
import { GroupViewPage } from './pages/GroupViewPage';
import { LocationsPage } from './pages/LocationsPage';
import { PersonsPage } from './pages/PersonsPage';
import { SpecialPage } from './pages/SpecialPage';
import { useRealtime } from './realtime/useRealtime';

const MENU = [
  { to: '/', text: 'Группы', end: true },
  { to: '/groups/new', text: 'Создать группу', end: true },
  { to: '/groups/view', text: 'Поиск по ID', end: false },
  { to: '/persons', text: 'Люди', end: false },
  { to: '/coordinates', text: 'Координаты', end: false },
  { to: '/locations', text: 'Локации', end: false },
  { to: '/special', text: 'Спецоперации', end: false },
];

export function App() {
  const connected = useRealtime();

  return (
    <div className="layout">
      <header className="header">
        <span className="logo">StudyGroup IS</span>
        <nav className="menu">
          {MENU.map((m) => (
            <NavLink key={m.to} to={m.to} end={m.end} className={({ isActive }) => `menu__link${isActive ? ' is-active' : ''}`}>
              {m.text}
            </NavLink>
          ))}
        </nav>
        <span className={`status${connected ? ' status--on' : ''}`} title="Синхронизация с другими пользователями">
          {connected ? '● онлайн' : '○ нет соединения'}
        </span>
      </header>
      <main className="main">
        <Routes>
          <Route path="/" element={<GroupsPage />} />
          <Route path="/groups/new" element={<GroupCreatePage />} />
          <Route path="/groups/view" element={<GroupViewPage />} />
          <Route path="/groups/:id" element={<GroupViewPage />} />
          <Route path="/persons" element={<PersonsPage />} />
          <Route path="/coordinates" element={<CoordinatesPage />} />
          <Route path="/locations" element={<LocationsPage />} />
          <Route path="/special" element={<SpecialPage />} />
          <Route path="*" element={<p>Страница не найдена</p>} />
        </Routes>
      </main>
    </div>
  );
}
