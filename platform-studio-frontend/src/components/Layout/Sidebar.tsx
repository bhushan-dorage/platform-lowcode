import React from 'react';
import { NavLink } from 'react-router-dom';

interface NavItem { label: string; path: string; icon: string }

const navItems: NavItem[] = [
  { label: 'Artifacts', path: '/artifacts', icon: '📦' },
  { label: 'BPMN Modeler', path: '/bpmn', icon: '⚙️' },
  { label: 'DMN Editor', path: '/dmn', icon: '📊' },
  { label: 'Form Designer', path: '/forms', icon: '📝' },
  { label: 'Data Modeler', path: '/data', icon: '🗄️' },
  { label: 'Role Manager', path: '/roles', icon: '🔐' },
  { label: 'Page Builder', path: '/pages', icon: '⊞' },
];

const sidebarStyle: React.CSSProperties = {
  width: 'var(--sidebar-width)',
  background: '#1e1b4b',
  color: '#c7d2fe',
  display: 'flex',
  flexDirection: 'column',
  flexShrink: 0,
  padding: '12px 0',
};

const logoStyle: React.CSSProperties = {
  padding: '8px 16px 20px',
  fontSize: 13,
  fontWeight: 600,
  color: '#818cf8',
  letterSpacing: '0.05em',
  textTransform: 'uppercase',
};

export default function Sidebar() {
  return (
    <nav style={sidebarStyle}>
      <div style={logoStyle}>Studio</div>
      {navItems.map((item) => (
        <NavLink
          key={item.path}
          to={item.path}
          style={({ isActive }) => ({
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            padding: '9px 16px',
            color: isActive ? '#ffffff' : '#a5b4fc',
            background: isActive ? 'rgba(99,102,241,0.3)' : 'transparent',
            textDecoration: 'none',
            fontSize: 13,
            fontWeight: isActive ? 600 : 400,
            borderLeft: isActive ? '3px solid #818cf8' : '3px solid transparent',
            transition: 'all 0.15s',
          })}
        >
          <span>{item.icon}</span>
          <span>{item.label}</span>
        </NavLink>
      ))}
    </nav>
  );
}
