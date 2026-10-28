import assert from 'node:assert/strict';
import test from 'node:test';
import { formatPracticeDuration } from '../src/utils/practiceDuration.js';

test('formats whole lifetime practice minutes from seconds', () => {
  const cases = [
    [0, '0 min'],
    [3 * 60, '3 min'],
    [45 * 60, '45 min'],
    [59 * 60, '59 min'],
    [60 * 60, '1 hr'],
    [90 * 60, '1 hr 30 min'],
    [120 * 60, '2 hrs'],
  ];

  for (const [seconds, expected] of cases) {
    assert.equal(formatPracticeDuration(seconds), expected);
  }
});

test('floors partial minutes and treats invalid or negative totals as zero', () => {
  assert.equal(formatPracticeDuration(3 * 60 + 59), '3 min');
  assert.equal(formatPracticeDuration(-1), '0 min');
  assert.equal(formatPracticeDuration(Number.NaN), '0 min');
});
