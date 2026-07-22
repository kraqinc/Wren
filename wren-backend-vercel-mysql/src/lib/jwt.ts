import { SignJWT, jwtVerify } from "jose";

interface WrenJwtPayload {
  sub: string;
  email: string;
  role: string;
  tier: string;
  [key: string]: unknown;
}

const encoder = new TextEncoder();

function getSecret(secret: string) {
  return encoder.encode(secret);
}

export async function signJwt(
  payload: WrenJwtPayload,
  secret: string,
  expiresInSeconds = 60 * 60 * 24 * 30
): Promise<string> {
  return await new SignJWT(payload)
    .setProtectedHeader({ alg: "HS256" })
    .setIssuedAt()
    .setExpirationTime(`${expiresInSeconds}s`)
    .sign(getSecret(secret));
}

export async function verifyJwt(
  token: string,
  secret: string
): Promise<WrenJwtPayload | null> {
  try {
    const { payload } = await jwtVerify(token, getSecret(secret));

    return payload as unknown as WrenJwtPayload;
  } catch {
    return null;
  }
}