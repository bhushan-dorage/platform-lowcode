import React, { useEffect, useState } from 'react';
import { api } from '../../api/client';

interface Role {
  id: string;
  name: string;
  displayName: string;
  parentRoleId: string | null;
  permissions: Permission[];
}

interface Permission {
  id: string;
  name: string;
  description: string;
}

const inputStyle: React.CSSProperties = {
  padding: '6px 10px', border: '1px solid var(--border)',
  borderRadius: 6, fontSize: 13,
};

export default function RoleManagerPage() {
  const [roles, setRoles] = useState<Role[]>([]);
  const [selectedRole, setSelectedRole] = useState<Role | null>(null);
  const [newRoleName, setNewRoleName] = useState('');
  const [newPermission, setNewPermission] = useState('');
  const [userId, setUserId] = useState('');
  const [assignRoleName, setAssignRoleName] = useState('');
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadRoles();
  }, []);

  const loadRoles = () => {
    setLoading(true);
    api.get<Role[]>('/rbac/roles')
      .then(setRoles)
      .catch(() => setStatus('Failed to load roles'))
      .finally(() => setLoading(false));
  };

  const createRole = async () => {
    if (!newRoleName) return;
    try {
      await api.post('/rbac/roles', { name: newRoleName, displayName: newRoleName });
      setNewRoleName('');
      setStatus('Role created');
      setTimeout(() => setStatus(''), 2000);
      loadRoles();
    } catch {
      setStatus('Failed to create role');
    }
  };

  const grantPermission = async () => {
    if (!selectedRole || !newPermission) return;
    try {
      await api.post(`/rbac/roles/${selectedRole.name}/permissions`, { permission: newPermission });
      setNewPermission('');
      setStatus('Permission granted');
      setTimeout(() => setStatus(''), 2000);
      loadRoles();
    } catch {
      setStatus('Failed to grant permission');
    }
  };

  const assignRole = async () => {
    if (!userId || !assignRoleName) return;
    try {
      await api.post(`/rbac/users/${userId}/roles`, { role: assignRoleName });
      setStatus(`Role "${assignRoleName}" assigned to ${userId}`);
      setTimeout(() => setStatus(''), 3000);
    } catch {
      setStatus('Failed to assign role');
    }
  };

  return (
    <div style={{ maxWidth: 900 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <h2 style={{ fontSize: 18, fontWeight: 700 }}>Role Manager</h2>
        {status && <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>{status}</span>}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, marginBottom: 20 }}>
        {/* Role list */}
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 10, padding: 16 }}>
          <h3 style={{ fontWeight: 600, marginBottom: 12, fontSize: 14 }}>Roles</h3>
          {loading && <div style={{ color: 'var(--text-muted)', fontSize: 13 }}>Loading...</div>}
          {roles.map((role) => (
            <div
              key={role.id}
              onClick={() => setSelectedRole(role)}
              style={{
                padding: '8px 12px', marginBottom: 4, borderRadius: 6,
                cursor: 'pointer', fontSize: 13,
                background: selectedRole?.id === role.id ? '#ede9fe' : '#f9fafb',
                border: `1px solid ${selectedRole?.id === role.id ? '#7c3aed' : 'transparent'}`,
              }}
            >
              <div style={{ fontWeight: 500 }}>{role.displayName || role.name}</div>
              <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>
                {role.permissions?.length ?? 0} permission(s)
              </div>
            </div>
          ))}
          <div style={{ marginTop: 12, display: 'flex', gap: 6 }}>
            <input style={{ ...inputStyle, flex: 1 }} value={newRoleName}
              onChange={(e) => setNewRoleName(e.target.value)}
              placeholder="New role name" />
            <button className="btn-primary" onClick={createRole} style={{ whiteSpace: 'nowrap' }}>+ Role</button>
          </div>
        </div>

        {/* Selected role permissions */}
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 10, padding: 16 }}>
          <h3 style={{ fontWeight: 600, marginBottom: 12, fontSize: 14 }}>
            {selectedRole ? `Permissions — ${selectedRole.name}` : 'Select a role'}
          </h3>
          {selectedRole && (
            <>
              {(selectedRole.permissions ?? []).length === 0 && (
                <div style={{ color: 'var(--text-muted)', fontSize: 13, marginBottom: 12 }}>No permissions assigned</div>
              )}
              {(selectedRole.permissions ?? []).map((p) => (
                <div key={p.id} style={{
                  padding: '6px 10px', marginBottom: 4, background: '#f0fdf4',
                  border: '1px solid #bbf7d0', borderRadius: 6, fontSize: 12, fontFamily: 'monospace',
                }}>
                  {p.name}
                </div>
              ))}
              <div style={{ marginTop: 12, display: 'flex', gap: 6 }}>
                <input style={{ ...inputStyle, flex: 1, fontSize: 12 }} value={newPermission}
                  onChange={(e) => setNewPermission(e.target.value)}
                  placeholder="platform:resource:action" />
                <button className="btn-primary" onClick={grantPermission}>Grant</button>
              </div>
            </>
          )}
        </div>
      </div>

      {/* User → Role assignment */}
      <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 10, padding: 16 }}>
        <h3 style={{ fontWeight: 600, marginBottom: 12, fontSize: 14 }}>Assign Role to User</h3>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <input style={{ ...inputStyle, width: 220 }} value={userId}
            onChange={(e) => setUserId(e.target.value)} placeholder="User ID" />
          <select style={{ ...inputStyle, flex: 1 }} value={assignRoleName}
            onChange={(e) => setAssignRoleName(e.target.value)}>
            <option value="">Select role…</option>
            {roles.map((r) => <option key={r.id} value={r.name}>{r.displayName || r.name}</option>)}
          </select>
          <button className="btn-primary" onClick={assignRole}>Assign</button>
        </div>
      </div>
    </div>
  );
}
