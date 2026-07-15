/*
 * Front-end RBAC helpers.
 *
 * The server remains the authority for API access.  These helpers only decide
 * which routes and controls should be visible in the UI.  All values crossing
 * the API boundary are normalised and unknown values fail closed.
 */

export const PERMISSIONS = Object.freeze({
  COMPANY_WRITE: 'company:write',
  SALE_WRITE: 'sale:write',
  CITY_WRITE: 'city:write',
  DRUG_WRITE: 'drug:write',
  MEDICAL_POLICY_WRITE: 'medical-policy:write',
  COMPANY_POLICY_WRITE: 'company-policy:write',
  DOCTOR_WRITE: 'doctor:write',
  DOCTOR_RESET_PASSWORD: 'doctor:reset-password',
  MATERIAL_WRITE: 'material:write',
  FILE_UPLOAD: 'file:upload',
});

const ADMIN_ROLES = new Set(['ADMIN', 'ROLE_1', '1']);
const WRITE_CODES = Object.freeze([
  PERMISSIONS.COMPANY_WRITE,
  PERMISSIONS.SALE_WRITE,
  PERMISSIONS.CITY_WRITE,
  PERMISSIONS.DRUG_WRITE,
  PERMISSIONS.MEDICAL_POLICY_WRITE,
  PERMISSIONS.COMPANY_POLICY_WRITE,
  PERMISSIONS.DOCTOR_WRITE,
  PERMISSIONS.DOCTOR_RESET_PASSWORD,
  PERMISSIONS.MATERIAL_WRITE,
  PERMISSIONS.FILE_UPLOAD,
]);

function asList(value) {
  if (Array.isArray(value)) {
    return value;
  }
  if (typeof value === 'string') {
    return value.split(/[\s,]+/);
  }
  return [];
}

function unique(values) {
  return [...new Set(values)];
}

export function normalizePermissionCodes(value) {
  return unique(asList(value)
    .filter((item) => typeof item === 'string' || typeof item === 'number')
    .map((item) => String(item).trim().toLowerCase())
    .filter(Boolean));
}

export function normalizeRoles(value) {
  return unique(asList(value)
    .filter((item) => typeof item === 'string' || typeof item === 'number')
    .map((item) => {
      const role = String(item).trim();
      if (!role) {
        return '';
      }
      if (/^\d+$/.test(role)) {
        return `ROLE_${role}`;
      }
      return role.toUpperCase();
    })
    .filter(Boolean));
}

export function userRoles(userInfo) {
  if (!userInfo || typeof userInfo !== 'object') {
    return [];
  }
  const declared = normalizeRoles(userInfo.roles);
  if (declared.length) {
    return declared;
  }
  if (userInfo.role !== undefined && userInfo.role !== null) {
    return normalizeRoles([userInfo.role]);
  }
  if (userInfo.utype !== undefined && userInfo.utype !== null && userInfo.utype !== '') {
    return normalizeRoles([userInfo.utype]);
  }
  return [];
}

export function isAdminRole(roles) {
  return normalizeRoles(roles).some((role) => ADMIN_ROLES.has(role));
}

/**
 * Build a safe access snapshot.  Explicit permissionCodes always win, even
 * when the server explicitly returns an empty list.  The role fallback is
 * only for older backends which do not send action permission codes yet.
 */
export function normalizeAccess({
  permissionCodes,
  roles,
  userInfo,
  allowedRoutePaths,
  allowedRouteNames,
} = {}) {
  const explicitCodes = permissionCodes !== undefined && permissionCodes !== null;
  const normalizedRoles = normalizeRoles(
    roles !== undefined && roles !== null ? roles : userRoles(userInfo),
  );
  const codes = normalizePermissionCodes(permissionCodes);
  const effectiveCodes = explicitCodes
    ? codes
    : (isAdminRole(normalizedRoles) ? WRITE_CODES.slice() : []);
  return {
    permissionCodes: effectiveCodes,
    roles: normalizedRoles,
    allowedRoutePaths: normalizePaths(allowedRoutePaths),
    allowedRouteNames: normalizeStringList(allowedRouteNames),
  };
}

export function normalizeStringList(value) {
  return unique(asList(value)
    .filter((item) => typeof item === 'string' || typeof item === 'number')
    .map((item) => String(item).trim())
    .filter(Boolean));
}

export function normalizePaths(value) {
  return normalizeStringList(value).map((path) => {
    if (path.length > 1) {
      return path.replace(/\/+$/, '');
    }
    return path || '/';
  });
}

function contextFrom(value) {
  if (!value || typeof value !== 'object') {
    return normalizeAccess();
  }
  if (Array.isArray(value)) {
    return normalizeAccess({ permissionCodes: value });
  }
  return normalizeAccess(value);
}

/**
 * Check one action code.  `:write` is a deliberate shorthand used by legacy
 * pages and means "at least one resource write capability".  New code should
 * prefer a concrete code such as `drug:write`.
 */
export function can(code, access) {
  if (typeof code !== 'string' || !code.trim()) {
    return false;
  }
  const requested = code.trim().toLowerCase();
  const context = contextFrom(access);
  const codes = new Set(context.permissionCodes);
  if (codes.has('*') || codes.has(requested)) {
    return true;
  }
  if (requested === ':write' || requested === 'write') {
    return [...codes].some((candidate) => candidate.endsWith(':write'));
  }
  return false;
}

/** Compatibility alias for the former role-only template checks. */
export function hasRole(userInfoOrAccess, expectedRole = 1) {
  const value = userInfoOrAccess && userInfoOrAccess.userInfo
    ? userInfoOrAccess.userInfo
    : userInfoOrAccess;
  const expected = normalizeRoles([expectedRole])[0];
  return Boolean(userRoles(value).includes(expected)
    || (value && Number(value.utype) === Number(expectedRole)));
}

export function allowedPath(path, allowedRoutePaths) {
  if (typeof path !== 'string' || !path) {
    return false;
  }
  const paths = normalizePaths(allowedRoutePaths);
  if (!paths.length) {
    return false;
  }
  const normalized = path.length > 1 ? path.replace(/\/+$/, '') : path;
  return paths.some((candidate) => {
    if (normalized === candidate) {
      return true;
    }
    // Match only declared dynamic segments; a broad prefix check would let a
    // user with `/manage/drug` reach an undeclared sibling such as
    // `/manage/doctor`.
    if (candidate === '/' || candidate.endsWith('/*')) {
      const prefix = candidate.slice(0, -2).replace(/\/+$/, '');
      return prefix && normalized.startsWith(`${prefix}/`);
    }
    const candidateParts = candidate.split('/').filter(Boolean);
    const actualParts = normalized.split('/').filter(Boolean);
    return candidateParts.length === actualParts.length
      && candidateParts.every((part, index) => part.startsWith(':') || part === actualParts[index]);
  });
}

export { WRITE_CODES };
