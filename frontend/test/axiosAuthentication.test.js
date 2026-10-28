import assert from 'node:assert/strict';
import test from 'node:test';
import api from '../src/api/axiosConfig.js';

test('attaches the current tab session token to quiz API requests', async () => {
  const storedValues = new Map([
    ['mindmeter_token', 'test-session-token'],
    ['mindmeter_role', 'STUDENT'],
  ]);
  const originalWindow = globalThis.window;
  globalThis.window = {
    sessionStorage: {
      getItem: (key) => storedValues.get(key) ?? null,
      removeItem: (key) => storedValues.delete(key),
    },
    location: {
      pathname: '/student/quiz/12',
      assign: () => {},
    },
  };

  try {
    const response = await api.get('/questions/student/quiz/12', {
      adapter: async (config) => ({
        data: [],
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      }),
    });

    assert.equal(
      response.config.headers.get('Authorization'),
      'Bearer test-session-token'
    );
    assert.equal(response.config.url, '/questions/student/quiz/12');
  } finally {
    if (originalWindow === undefined) {
      delete globalThis.window;
    } else {
      globalThis.window = originalWindow;
    }
  }
});
