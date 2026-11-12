import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
  formatBytes,
  formatSpeed,
  formatEta,
  applyLowPassFilter,
} from './githubSync';

describe('GitHubSyncHelper speed and progress formatting', () => {
  test('formatBytes formats bytes to clean MB or KB', () => {
    assert.equal(formatBytes(0), '0.0 MB');
    assert.equal(formatBytes(512 * 1024), '512 KB');
    assert.equal(formatBytes(18.4 * 1024 * 1024), '18.4 MB');
    assert.equal(formatBytes(45.2 * 1024 * 1024), '45.2 MB');
  });

  test('formatSpeed displays instantaneous transfer rate with units', () => {
    assert.equal(formatSpeed(0), '0.0 MB/s');
    assert.equal(formatSpeed(480 * 1024), '480 KB/s');
    assert.equal(formatSpeed(3.45 * 1024 * 1024), '3.45 MB/s');
    assert.equal(formatSpeed(12.8 * 1024 * 1024), '12.80 MB/s');
  });

  test('formatEta calculates remaining time in English and Khmer', () => {
    // English format
    assert.equal(formatEta(12, false), '12s left');
    assert.equal(formatEta(60, false), '1m left');
    assert.equal(formatEta(75, false), '1m 15s left');

    // Khmer format
    assert.equal(formatEta(12, true), '12 វិ');
    assert.equal(formatEta(60, true), '1 ន');
    assert.equal(formatEta(75, true), '1 ន 15 វិ');

    // Edge cases
    assert.equal(formatEta(0, false), '—');
    assert.equal(formatEta(-5, true), '—');
  });

  test('applyLowPassFilter stabilizes erratic speed jumps', () => {
    const smoothed = applyLowPassFilter(100, 50, 0.4);
    assert.equal(smoothed, 70); // 0.4 * 100 + 0.6 * 50 = 70
  });
});
