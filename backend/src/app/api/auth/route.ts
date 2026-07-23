import { NextRequest, NextResponse } from "next/server";
import { createHash } from "crypto";
import { prisma } from "@/lib/prisma";
import { signJwt } from "@/lib/jwt";

export const runtime = "nodejs";

function hashPassword(password: string): string {
  return createHash("sha256").update(password + "wren-salt").digest("hex");
}

function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

export async function POST(req: NextRequest) {
  try {
    const { email, password, mode } = (await req.json()) as {
      email: string;
      password: string;
      mode?: "login" | "register";
    };

    if (!isValidEmail(email)) {
      return NextResponse.json({ error: "Email inválido" }, { status: 400 });
    }
    if (!password || password.length < 8) {
      return NextResponse.json({ error: "La contraseña debe tener al menos 8 caracteres" }, { status: 400 });
    }

    const secret = process.env.JWT_SECRET;
    if (!secret) {
      return NextResponse.json({ error: "JWT_SECRET no configurado en el servidor" }, { status: 500 });
    }

    let user = await prisma.user.findUnique({ where: { email } });

    if (mode === "register") {
      if (user) {
        return NextResponse.json({ error: "El usuario ya existe" }, { status: 409 });
      }
      user = await prisma.user.create({
        data: {
          email,
          passwordHash: hashPassword(password),
          credits: { create: { balance: 50 } },
        },
      });
    } else {
      if (!user || user.passwordHash !== hashPassword(password)) {
        return NextResponse.json({ error: "Credenciales inválidas" }, { status: 401 });
      }
    }

    const credits = await prisma.credits.findUnique({ where: { userId: user.id } });

    const token = await signJwt(
      { sub: user.id, email: user.email, role: user.role, tier: user.tier },
      secret
    );

    return NextResponse.json({
      message: mode === "register" ? "Cuenta creada" : "Sesión iniciada",
      token,
      user: {
        id: user.id,
        email: user.email,
        role: user.role,
        tier: user.tier,
        balance: credits?.balance ?? 0,
      },
    });
  } catch (err) {
    console.error("Error en /api/auth:", err);
    return NextResponse.json({ error: "Error interno de autenticación" }, { status: 500 });
  }
}
