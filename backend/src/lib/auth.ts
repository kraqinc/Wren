import { NextRequest } from "next/server";
import { verifyJwt } from "./jwt";

export async function getAuthenticatedUser(req: NextRequest) {
  const authHeader = req.headers.get("authorization") || "";
  const token = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : null;
  const secret = process.env.JWT_SECRET;
  if (!token || !secret) return null;
  return verifyJwt(token, secret);
}
