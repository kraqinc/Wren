import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getAuthenticatedUser } from "@/lib/auth";

export const runtime = "nodejs";

async function requireOwner(req: NextRequest) {
  const payload = await getAuthenticatedUser(req);
  if (!payload || payload.role !== "OWNER") return null;
  return payload;
}

export async function GET(req: NextRequest) {
  const owner = await requireOwner(req);
  if (!owner) return NextResponse.json({ error: "No autorizado" }, { status: 401 });

  const { searchParams } = new URL(req.url);
  const view = searchParams.get("view") ?? "stats";

  if (view === "pending-recharges") {
    const pending = await prisma.pendingRecharge.findMany({
      where: { status: "PENDING" },
      include: { user: { select: { email: true } } },
      orderBy: { createdAt: "asc" },
    });
    return NextResponse.json({
      requests: pending.map((r) => ({
        id: r.id,
        package_id: r.packageId,
        credit_amount: r.creditAmount,
        price_label: r.priceLabel,
        reference_code: r.referenceCode,
        status: r.status,
        created_at: r.createdAt,
        user_email: r.user.email,
        user_id: r.userId,
      })),
    });
  }

  if (view === "users") {
    const users = await prisma.user.findMany({
      include: { credits: true },
      orderBy: { createdAt: "desc" },
    });
    return NextResponse.json({
      users: users.map((u) => ({
        id: u.id,
        email: u.email,
        role: u.role,
        tier: u.tier,
        balance: u.credits?.balance ?? 0,
      })),
    });
  }

  if (view === "audit-logs") {
    const logs = await prisma.auditLog.findMany({
      include: { actor: { select: { email: true } } },
      orderBy: { timestamp: "desc" },
    });
    return NextResponse.json({
      logs: logs.map((log) => ({
        id: log.id,
        action: log.action,
        details: log.details,
        timestamp: log.timestamp,
        actor_email: log.actor.email,
      })),
    });
  }

  // stats
  const [totalUsers, totalProjects, creditsAgg, auditLogsCount] = await Promise.all([
    prisma.user.count(),
    prisma.project.count(),
    prisma.credits.aggregate({ _sum: { balance: true } }),
    prisma.auditLog.count(),
  ]);

  return NextResponse.json({
    metrics: {
      totalUsers,
      totalProjects,
      circulatingCredits: creditsAgg._sum.balance ?? 0,
      auditLogsLogged: auditLogsCount,
    },
  });
}

export async function POST(req: NextRequest) {
  const owner = await requireOwner(req);
  if (!owner) return NextResponse.json({ error: "No autorizado" }, { status: 401 });

  const { action, requestId, userId, amount, reason } = (await req.json()) as {
    action: "approve" | "reject" | "adjust-credits";
    requestId?: string;
    userId?: string;
    amount?: number;
    reason?: string;
  };

  if (action === "approve" || action === "reject") {
    if (!requestId) return NextResponse.json({ error: "requestId requerido" }, { status: 400 });

    const recharge = await prisma.pendingRecharge.findUnique({ where: { id: requestId } });
    if (!recharge || recharge.status !== "PENDING") {
      return NextResponse.json({ error: "Solicitud no encontrada o ya resuelta" }, { status: 404 });
    }

    if (action === "approve") {
      await prisma.$transaction([
        prisma.credits.update({
          where: { userId: recharge.userId },
          data: { balance: { increment: recharge.creditAmount } },
        }),
        prisma.pendingRecharge.update({
          where: { id: requestId },
          data: { status: "APPROVED", resolvedAt: new Date(), resolvedBy: owner.sub },
        }),
        prisma.auditLog.create({
          data: {
            actorId: owner.sub,
            action: "RECHARGE_APPROVED",
            details: `Recarga ${recharge.referenceCode} (+${recharge.creditAmount} créditos) para ${recharge.userId}`,
          },
        }),
      ]);
    } else {
      await prisma.$transaction([
        prisma.pendingRecharge.update({
          where: { id: requestId },
          data: { status: "REJECTED", resolvedAt: new Date(), resolvedBy: owner.sub },
        }),
        prisma.auditLog.create({
          data: {
            actorId: owner.sub,
            action: "RECHARGE_REJECTED",
            details: `Recarga ${recharge.referenceCode} rechazada`,
          },
        }),
      ]);
    }

    return NextResponse.json({ message: `Recarga ${action === "approve" ? "aprobada" : "rechazada"}` });
  }

  if (action === "adjust-credits") {
    if (!userId || amount === undefined || !reason) {
      return NextResponse.json({ error: "userId, amount y reason son requeridos" }, { status: 400 });
    }

    await prisma.$transaction([
      prisma.credits.update({ where: { userId }, data: { balance: { increment: amount } } }),
      prisma.creditLog.create({ data: { userId, amount, reason } }),
      prisma.auditLog.create({
        data: {
          actorId: owner.sub,
          action: "MANUAL_CREDIT_ADJUSTMENT",
          details: `${amount >= 0 ? "+" : ""}${amount} créditos para ${userId}. Motivo: ${reason}`,
        },
      }),
    ]);

    return NextResponse.json({ message: "Créditos ajustados" });
  }

  return NextResponse.json({ error: "Acción desconocida" }, { status: 400 });
}
