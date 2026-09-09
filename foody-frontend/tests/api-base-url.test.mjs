import test from 'node:test';
import assert from 'node:assert/strict';
import { apiBaseUrl } from '../src/lib/apiBaseUrl.ts';

test('production API origin is required and validated', () => {
  for (const value of [undefined, '', 'http://api.example.com', 'https://localhost', 'https://api.example.com/api', 'https://user:password@api.example.com', 'https://api.example.com?token=x']) {
    assert.throws(() => apiBaseUrl(value, true));
  }
  assert.equal(apiBaseUrl(' https://api.example.com/ ', true), 'https://api.example.com');
  assert.equal(apiBaseUrl(undefined, false), 'http://localhost:8080');
});
