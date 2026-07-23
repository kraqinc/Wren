import { NextRequest, NextResponse } from "next/server";
import { randomBytes } from "crypto";
import { prisma } from "@/lib/prisma";
import { getAuthenticatedUser } from "@/lib/auth";

export const runtime = "nodejs";

// Catálogo fijo del lado del servidor -- nunca confíes en un monto que
// mande el cliente (ver skill `billing`).
const PACKAGE_CATALOG: Record<string, { credits: number; priceLabel: string }> = {
  starter_100: { credits: 100, priceLabel: "$1.99" },
  basic_300: { credits: 300, priceLabel: "$4.99" },
  premium_500: { credits: 500, priceLabel: "$7.99" },
  pro_1500: { credits: 1500, priceLabel: "$19.99" },
  ultra_5000: { credits: 5000, priceLabel: "$59.99" },
};

function generateReferenceCode(): string {
  return `WREN-${randomBytes(3).toString("hex").toUpperCase()}`;
}

export async function GET(req: NextRequest) {
  const payload = await getAuthenticatedUser(req);
  if (!payload) return NextResponse.json({ error: "No autorizado" }, { status: 401 });

  const credits = await prisma.credits.findUnique({ where: { userId: payload.sub } });
  const pending = await prisma.pendingRecharge.findMany({
    where: { userId: payload.sub, status: "PENDING" },
    orderBy: { createdAt: "desc" },
  });

  return NextResponse.json({ balance: credits?.balance ?? 0, pending });
}

export async function POST(req: NextRequest) {
  const payload = await getAuthenticatedUser(req);
  if (!payload) return NextResponse.json({ error: "No autorizado" }, { status: 401 });

  const { packageId } = (await req.json()) as { packageId: string };
  const pkg = PACKAGE_CATALOG[packageId];
  if (!pkg) {
    return NextResponse.json({ error: "Paquete inválido" }, { status: 400 });
  }

  // Regla de oro: nunca se acredita aquí. Solo se crea la solicitud
  // pendiente -- el OWNER la aprueba a mano después de confirmar el pago
  // real (ver skill `billing`).
  const recharge = await prisma.pendingRecharge.create({
    data: {
      userId: payload.sub,
      packageId,
      creditAmount: pkg.credits,
      priceLabel: pkg.priceLabel,
      referenceCode: generateReferenceCode(),
      status: "PENDING",
    },
  });

  return NextResponse.json({
    message: "Solicitud creada. Incluye el código de referencia en la nota de tu pago de PayPal.",
    requestId: recharge.id,
    referenceCode: recharge.referenceCode,
    credits: pkg.credits,
    priceLabel: pkg.priceLabel,
    status: "PENDING",
  });
}
