import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getAuthenticatedUser } from "@/lib/auth";

export const runtime = "nodejs";

export async function GET(req: NextRequest) {
  const payload = await getAuthenticatedUser(req);
  if (!payload) return NextResponse.json({ error: "No autorizado" }, { status: 401 });

  const logs = await prisma.creditLog.findMany({
    where: { userId: payload.sub },
    orderBy: { timestamp: "desc" },
  });

  return NextResponse.json({
    logs: logs.map((log) => ({
      id: log.id,
      user_id: log.userId,
      amount: log.amount,
      reason: log.reason,
      timestamp: log.timestamp,
    })),
  });
}
