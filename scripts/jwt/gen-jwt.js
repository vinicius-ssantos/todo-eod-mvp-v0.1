#!/usr/bin/env node
// Minimal HS256 JWT generator without external deps
// Usage:
//   node scripts/jwt/gen-jwt.js --secret change-me \
//        --sub dev --scope "tasks:* webhooks:ingest" --exp 3600

const crypto = require('crypto');

function b64url(input) {
  return Buffer.from(input)
    .toString('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
}

function parseArgs() {
  const args = process.argv.slice(2);
  const out = {};
  for (let i = 0; i < args.length; i++) {
    if (args[i].startsWith('--')) {
      const key = args[i].substring(2);
      const val = args[i + 1] && !args[i + 1].startsWith('--') ? args[++i] : 'true';
      out[key] = val;
    }
  }
  return out;
}

const a = parseArgs();
const secret = a.secret || process.env.APP_JWT_SECRET || 'change-me';
const sub = a.sub || 'dev';
const scope = a.scope || 'tasks:* webhooks:ingest';
const expSec = parseInt(a.exp || a.expSeconds || '3600', 10);
const now = Math.floor(Date.now() / 1000);

const header = { alg: 'HS256', typ: 'JWT' };
const payload = {
  sub,
  scope,
  iat: now,
  exp: now + expSec,
};

const encHeader = b64url(JSON.stringify(header));
const encPayload = b64url(JSON.stringify(payload));
const toSign = `${encHeader}.${encPayload}`;
const sig = crypto.createHmac('sha256', Buffer.from(secret, 'utf8'))
  .update(toSign)
  .digest('base64')
  .replace(/=/g, '')
  .replace(/\+/g, '-')
  .replace(/\//g, '_');

const token = `${toSign}.${sig}`;
console.log(token);

