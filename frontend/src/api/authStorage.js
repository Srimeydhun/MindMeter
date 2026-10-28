const AUTH_KEYS = [
  'mindmeter_token',
  'mindmeter_role',
  'mindmeter_user_id',
  'mindmeter_user_code',
  'mindmeter_user_name',
  'mindmeter_user_email',
];

export function getAuthValue(key) {
  return window.sessionStorage.getItem(key);
}

export function setAuthSession(data, role) {
  clearAuthSession();
  window.sessionStorage.setItem('mindmeter_token', data.token);
  window.sessionStorage.setItem('mindmeter_role', role);
  window.sessionStorage.setItem(
    'mindmeter_user_id',
    data.id != null ? String(data.id) : ''
  );
  window.sessionStorage.setItem(
    'mindmeter_user_code',
    data.userCode || ''
  );
  window.sessionStorage.setItem('mindmeter_user_name', data.name || '');
  window.sessionStorage.setItem('mindmeter_user_email', data.email || '');
}

export function clearAuthSession() {
  AUTH_KEYS.forEach((key) => window.sessionStorage.removeItem(key));
  window.sessionStorage.removeItem('mindmeter_start_quiz');
}

export function clearLegacyAuthStorage() {
  AUTH_KEYS.forEach((key) => window.localStorage.removeItem(key));
}
