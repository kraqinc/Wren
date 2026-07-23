import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getAuthenticatedUser } from "@/lib/auth";

export const runtime = "nodejs";

async function ownsProject(userId: string, projectId: string) {
  const project = await prisma.project.findUnique({ where: { id: projectId } });
  return project?.userId === userId;
}

export async function GET(req: NextRequest) {
  const payload = await getAuthenticatedUser(req);
  if (!payload) return NextResponse.json({ error: "No autorizado" }, { status: 401 });

  const { searchParams } = new URL(req.url);
  const projectId = searchParams.get("projectId");
  if (!projectId) return NextResponse.json({ error: "projectId requerido" }, { status: 400 });

  if (!(await ownsProject(payload.sub, projectId))) {
    return NextResponse.json({ error: "No autorizado para este proyecto" }, { status: 403 });
  }

  const files = await prisma.projectFile.findMany({ where: { projectId } });
  return NextResponse.json({ files });
}

export async function POST(req: NextRequest) {
  const payload = await getAuthenticatedUser(req);
  if (!payload) return NextResponse.json({ error: "No autorizado" }, { status: 401 });

  const body = (await req.json()) as {
    projectId: string;
    name: string;
    path: string;
    isDirectory?: boolean;
    content?: string;
    parentId?: string | null;
  };

  if (!(await ownsProject(payload.sub, body.projectId))) {
    return NextResponse.json({ error: "No autorizado para este proyecto" }, { status: 403 });
  }

  const file = await prisma.projectFile.create({
    data: {
      projectId: body.projectId,
      name: body.name,
      path: body.path,
      isDirectory: !!body.isDirectory,
      content: body.isDirectory ? null : (body.content ?? ""),
      parentId: body.parentId ?? null,
    },
  });

  return NextResponse.json({ file }, { status: 201 });
}

export async function PUT(req: NextRequest) {
  const payload = await getAuthenticatedUser(req);
  if (!payload) return NextResponse.json({ error: "No autorizado" }, { status: 401 });

  const { fileId, content } = (await req.json()) as { fileId: string; content: string };

  const file = await prisma.projectFile.findUnique({ where: { id: fileId } });
  if (!file || !(await ownsProject(payload.sub, file.projectId))) {
    return NextResponse.json({ error: "No autorizado" }, { status: 403 });
  }

  const updated = await prisma.projectFile.update({ where: { id: fileId }, data: { content } });
  return NextResponse.json({ file: updated });
}

export async function DELETE(req: NextRequest) {
  const payload = await getAuthenticatedUser(req);
  if (!payload) return NextResponse.json({ error: "No autorizado" }, { status: 401 });

  const { searchParams } = new URL(req.url);
  const fileId = searchParams.get("fileId");
  if (!fileId) return NextResponse.json({ error: "fileId requerido" }, { status: 400 });

  const file = await prisma.projectFile.findUnique({ where: { id: fileId } });
  if (!file || !(await ownsProject(payload.sub, file.projectId))) {
    return NextResponse.json({ error: "No autorizado" }, { status: 403 });
  }

  await prisma.projectFile.delete({ where: { id: fileId } });
  return NextResponse.json({ message: "Archivo eliminado" });
}
