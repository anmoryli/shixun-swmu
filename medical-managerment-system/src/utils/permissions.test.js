import { describe, expect, it } from 'vitest';
import {
  PERMISSIONS,
  WRITE_CODES,
  allowedPath,
  can,
  hasRole,
  isAdminRole,
  normalizeAccess,
  normalizePaths,
  normalizePermissionCodes,
  normalizeRoles,
  normalizeStringList,
  userRoles,
} from './permissions';

describe('permission normalisation and capability checks', () => {
  it('normalises safe code and role lists', () => {
    expect(normalizePermissionCodes([' Drug:WRITE ', 2, null, '', 'drug:write'])).toEqual([
      'drug:write', '2',
    ]);
    expect(normalizePermissionCodes('drug:read, drug:write')).toEqual([
      'drug:read', 'drug:write',
    ]);
    expect(normalizePermissionCodes({ code: 'drug:write' })).toEqual([]);

    expect(normalizeRoles([' admin ', 1, 'ROLE_1', null, ''])).toEqual(['ADMIN', 'ROLE_1']);
    expect(normalizeRoles('doctor, 2')).toEqual(['DOCTOR', 'ROLE_2']);
    expect(normalizeRoles({ role: 'ADMIN' })).toEqual([]);
    expect(userRoles(null)).toEqual([]);
    expect(userRoles({ roles: ['doctor'] })).toEqual(['DOCTOR']);
    expect(userRoles({ role: 'ROLE_2' })).toEqual(['ROLE_2']);
    expect(userRoles({ utype: 1 })).toEqual(['ROLE_1']);
    expect(userRoles({})).toEqual([]);
    expect(isAdminRole(['ROLE_1'])).toBe(true);
    expect(isAdminRole(['ROLE_2'])).toBe(false);
  });

  it('uses explicit capabilities and fails closed for unknown roles', () => {
    const adminFallback = normalizeAccess({ userInfo: { utype: 1 } });
    expect(adminFallback.permissionCodes).toEqual([...WRITE_CODES]);
    expect(normalizeAccess({ userInfo: { utype: 2 } }).permissionCodes).toEqual([]);
    expect(normalizeAccess({ roles: ['ADMIN'], permissionCodes: [] }).permissionCodes).toEqual([]);
    expect(normalizeAccess({ permissionCodes: ['Drug:WRITE'], roles: ['doctor'] })).toMatchObject({
      permissionCodes: ['drug:write'], roles: ['DOCTOR'],
    });
    expect(normalizeAccess({ allowedRoutePaths: ['/home///', '/'], allowedRouteNames: [' Home '] }))
      .toMatchObject({ allowedRoutePaths: ['/home', '/'], allowedRouteNames: ['Home'] });
    expect(normalizePaths(['/a///', '/', '', 2])).toEqual(['/a', '/', '2']);
    expect(normalizeStringList([' a ', 2, '', null, 'a'])).toEqual(['a', '2']);
  });

  it('checks exact, wildcard, write shorthand, and invalid capabilities', () => {
    const access = { permissionCodes: ['drug:write', 'menu:drug'] };
    expect(can(PERMISSIONS.DRUG_WRITE, access)).toBe(true);
    expect(can('menu:drug', access)).toBe(true);
    expect(can(':write', access)).toBe(true);
    expect(can('write', access)).toBe(true);
    expect(can('company:write', access)).toBe(false);
    expect(can('', access)).toBe(false);
    expect(can(null, access)).toBe(false);
    expect(can('drug:write', ['drug:write'])).toBe(true);
    expect(can('drug:write')).toBe(false);
    expect(can('drug:write', { permissionCodes: ['*'] })).toBe(true);
  });

  it('keeps the legacy role alias while route checks fail closed', () => {
    expect(hasRole({ userInfo: { utype: 1 } })).toBe(true);
    expect(hasRole({ utype: '1' })).toBe(true);
    expect(hasRole({ utype: 2 })).toBe(false);
    expect(hasRole(null)).toBe(false);
    expect(allowedPath(null, ['/home'])).toBe(false);
    expect(allowedPath('/home', [])).toBe(false);
    expect(allowedPath('/home', ['/home'])).toBe(true);
    expect(allowedPath('/home/detail', ['/home'])).toBe(false);
    expect(allowedPath('/home/detail', ['/home/*'])).toBe(true);
    expect(allowedPath('/home/42', ['/home/:id'])).toBe(true);
    expect(allowedPath('/company', ['/home'])).toBe(false);
  });
});
