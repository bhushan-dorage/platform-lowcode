import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';

const styles: Record<string, React.CSSProperties> = {
  root: { display: 'flex', height: '100vh', overflow: 'hidden' },
  main: { flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' },
  header: {
    height: 'var(--header-height)',
    background: 'var(--surface)',
    borderBottom: '1px solid var(--border)',
    display: 'flex',
    alignItems: 'center',
    padding: '0 20px',
    gap: 12,
    flexShrink: 0,
  },
  title: { fontWeight: 700, fontSize: 16, color: 'var(--primary)' },
  content: { flex: 1, overflow: 'auto', padding: 24 },
};

export default function Shell() {
  return (
    <div style={styles.root}>
      <Sidebar />
      <div style={styles.main}>
        <header style={styles.header}>
          <span style={styles.title}>Platform Studio</span>
        </header>
        <main style={styles.content}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
