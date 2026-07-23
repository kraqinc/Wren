import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getAuthenticatedUser } from "@/lib/auth";

export const runtime = "nodejs";

export async function GET(req: NextRequest) {
  const payload = await getAuthenticatedUser(req);
  if (!payload) return NextResponse.json({ error: "No autorizado" }, { status: 401 });

  const projects = await prisma.project.findMany({
    where: { userId: payload.sub },
    orderBy: { updatedAt: "desc" },
  });

  return NextResponse.json({ projects });
}

export async function POST(req: NextRequest) {
  const payload = await getAuthenticatedUser(req);
  if (!payload) return NextResponse.json({ error: "No autorizado" }, { status: 401 });

  const { name, description } = (await req.json()) as { name: string; description?: string };
  if (!name || !name.trim()) {
    return NextResponse.json({ error: "El nombre del proyecto es requerido" }, { status: 400 });
  }

  const project = await prisma.project.create({
    data: { userId: payload.sub, name: name.trim(), description: description ?? "" },
  });

  return NextResponse.json({ project }, { status: 201 });
}
