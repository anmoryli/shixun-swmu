// 鉴权信息的内存态持有者。
// 出于安全考虑（G.WST.01），token 不再由前端持有：改用 httpOnly cookie 由浏览器自动携带，
// 前端 JS 读不到。此处仅保留登录态标志与用户信息（UI 展示用），刷新后由 /api/session 恢复。
let loggedIn = false;
let userInfo = null;
let permissionCodes = [];
let roles = [];
let allowedRoutePaths = [];
let allowedRouteNames = [];

export function isLoggedIn() {
  return loggedIn;
}

export function setLoggedIn(value) {
  loggedIn = Boolean(value);
}

export function getUserInfo() {
  return userInfo;
}

export function setUserInfo(value) {
  userInfo = value || null;
  loggedIn = userInfo != null;
  // A refreshed profile without explicit access metadata must not inherit
  // permissions from the previous profile.
  permissionCodes = userInfo && Array.isArray(userInfo.permissionCodes)
    ? userInfo.permissionCodes.slice() : [];
  roles = userInfo && Array.isArray(userInfo.roles) ? userInfo.roles.slice() : [];
  allowedRoutePaths = [];
  allowedRouteNames = [];
}

export function setAccess({
  permissionCodes: nextPermissionCodes = [],
  roles: nextRoles = [],
  allowedRoutePaths: nextRoutePaths = [],
  allowedRouteNames: nextRouteNames = [],
} = {}) {
  permissionCodes = Array.isArray(nextPermissionCodes) ? nextPermissionCodes.slice() : [];
  roles = Array.isArray(nextRoles) ? nextRoles.slice() : [];
  allowedRoutePaths = Array.isArray(nextRoutePaths) ? nextRoutePaths.slice() : [];
  allowedRouteNames = Array.isArray(nextRouteNames) ? nextRouteNames.slice() : [];
}

export function getPermissionCodes() {
  return permissionCodes.slice();
}

export function getRoles() {
  return roles.slice();
}

export function getAllowedRoutePaths() {
  return allowedRoutePaths.slice();
}

export function getAllowedRouteNames() {
  return allowedRouteNames.slice();
}

export function clearAuth() {
  loggedIn = false;
  userInfo = null;
  permissionCodes = [];
  roles = [];
  allowedRoutePaths = [];
  allowedRouteNames = [];
}
