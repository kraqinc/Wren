import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getAuthenticatedUser } from "@/lib/auth";
import { getRelevantSkillsContent } from "@/lib/skillsLoader";

export const runtime = "nodejs";
export const maxDuration = 60;

const GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

export async function POST(req: NextRequest) {
  const payload = await getAuthenticatedUser(req);
  if (!payload) return NextResponse.json({ error: "No autorizado" }, { status: 401 });

  const { prompt, mode } = (await req.json()) as { prompt: string; mode?: string };
  if (!prompt) return NextResponse.json({ error: "prompt es requerido" }, { status: 400 });

  const credits = await prisma.credits.findUnique({ where: { userId: payload.sub } });
  if (!credits || credits.balance <= 0) {
    return NextResponse.json({ error: "Créditos insuficientes" }, { status: 402 });
  }

  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) return NextResponse.json({ error: "GEMINI_API_KEY no configurado" }, { status: 500 });

  const skillsContent = getRelevantSkillsContent(prompt);
  const systemInstruction = `Eres Wren AI, el copiloto del IDE móvil Wren. Responde en español, claro y directo.${skillsContent}`;

  let responseText: string;
  try {
    const geminiRes = await fetch(`${GEMINI_URL}?key=${apiKey}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        systemInstruction: { parts: [{ text: systemInstruction }] },
        contents: [{ role: "user", parts: [{ text: prompt }] }],
      }),
    });

    if (!geminiRes.ok) throw new Error(`Gemini respondió ${geminiRes.status}`);
    const data = await geminiRes.json();
    responseText = data.candidates?.[0]?.content?.parts?.[0]?.text ?? "";
    if (!responseText) throw new Error("Respuesta vacía de Gemini");
  } catch (err) {
    // Sin crédito descontado -- el fallo de red nunca cobra (ver skill
    // `android-ai-agent-screen`, el mismo bug ya se cometió dos veces antes).
    console.error("Error llamando a Gemini:", err);
    return NextResponse.json(
      { error: "No se pudo contactar al modelo de IA. Inténtalo de nuevo." },
      { status: 503 }
    );
  }

  // Solo ahora, con una respuesta real en mano, se descuenta el crédito.
  const updated = await prisma.credits.update({
    where: { userId: payload.sub },
    data: { balance: { decrement: 1 } },
  });
  await prisma.creditLog.create({
    data: { userId: payload.sub, amount: -1, reason: `AI_${(mode ?? "chat").toUpperCase()}` },
  });

  return NextResponse.json({
    success: true,
    mode: mode ?? "chat",
    response: responseText,
    remainingCredits: updated.balance,
    timestamp: new Date().toISOString(),
  });
}
