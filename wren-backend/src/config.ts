/**
 * Centralized runtime configuration and safety checks.
 * Fails fast in production when insecure defaults are left in place.
 */

const DEFAULT_JWT_SECRET = 'wren-super-secret-key-2026-mobile-ide';
const DEFAULT_OWNER_PASSWORD = 'WrenOwner2026!';

const isProduction = process.env.NODE_ENV === 'production';

export const JWT_SECRET = process.env.JWT_SECRET || DEFAULT_JWT_SECRET;
export const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '30d';
export const PORT = Number(process.env.PORT) || 3000;
export const OWNER_EMAIL = process.env.OWNER_EMAIL || 'owner@wren.ide';
export const OWNER_PASSWORD = process.env.OWNER_PASSWORD || DEFAULT_OWNER_PASSWORD;

export function assertSecureConfig(): void {
  const problems: string[] = [];
  if (JWT_SECRET === DEFAULT_JWT_SECRET) {
    problems.push('JWT_SECRET is using the built-in default. Set a strong JWT_SECRET env var.');
  }
  if (OWNER_PASSWORD === DEFAULT_OWNER_PASSWORD) {
    problems.push('OWNER_PASSWORD is using the built-in default. Set OWNER_PASSWORD env var.');
  }

  if (problems.length === 0) return;

  if (isProduction) {
    console.error('[SECURITY] Refusing to start in production with insecure defaults:');
    problems.forEach((p) => console.error(`  - ${p}`));
    process.exit(1);
  } else {
    console.warn('[SECURITY] Insecure defaults detected (allowed in non-production):');
    problems.forEach((p) => console.warn(`  - ${p}`));
  }
}
