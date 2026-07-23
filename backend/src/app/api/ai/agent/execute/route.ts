import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getAuthenticatedUser } from "@/lib/auth";

export const runtime = "nodejs";

export async function POST(req: NextRequest) {
  const payload = await getAuthenticatedUser(req);
  if (!payload) return NextResponse.json({ error: "No autorizado" }, { status: 401 });

  const { action, path, content, command } = (await req.json()) as {
    action: string;
    path?: string | null;
    content?: string | null;
    command?: string | null;
  };

  const credits = await prisma.credits.findUnique({ where: { userId: payload.sub } });
  if (!credits || credits.balance <= 0) {
    return NextResponse.json({ error: "Créditos insuficientes" }, { status: 402 });
  }

  const updated = await prisma.credits.update({
    where: { userId: payload.sub },
    data: { balance: { decrement: 1 } },
  });

  await prisma.creditLog.create({
    data: {
      userId: payload.sub,
      amount: -1,
      reason: `AI_AGENT_${action.toUpperCase()}`,
    },
  });

  await prisma.auditLog.create({
    data: {
      actorId: payload.sub,
      action: "AGENT_EXECUTE",
      details: `Acción ${action}${path ? ` en ${path}` : ""}${command ? ` comando=${command}` : ""}${content ? ` contentLen=${content.length}` : ""}`,
    },
  });

  return NextResponse.json({
    success: true,
    mode: "agent_execute",
    response: `Acción ${action} registrada correctamente.`,
    actions: [],
    remainingCredits: updated.balance,
    timestamp: new Date().toISOString(),
  });
}
