import fs from "fs";
import path from "path";

interface SkillMeta {
  name: string;
  description: string;
  triggers: string[];
}
interface SkillsIndex {
  project: string;
  language: string;
  count: number;
  skills: SkillMeta[];
}

const SKILLS_DIR = path.join(process.cwd(), "skills");
const INDEX_PATH = path.join(SKILLS_DIR, "skills_index.json");

let cachedIndex: SkillsIndex | null = null;
const contentCache = new Map<string, string>();

function loadIndex(): SkillsIndex | null {
  if (cachedIndex) return cachedIndex;
  try {
    cachedIndex = JSON.parse(fs.readFileSync(INDEX_PATH, "utf-8"));
    return cachedIndex;
  } catch (err) {
    console.warn("[skills] No se pudo cargar skills_index.json:", err);
    return null;
  }
}

function loadSkillContent(name: string): string | null {
  if (contentCache.has(name)) return contentCache.get(name)!;
  try {
    const content = fs.readFileSync(path.join(SKILLS_DIR, name, "skill.md"), "utf-8");
    contentCache.set(name, content);
    return content;
  } catch (err) {
    console.warn(`[skills] No se pudo cargar el skill "${name}":`, err);
    return null;
  }
}

export function findRelevantSkills(userPrompt: string): string[] {
  const index = loadIndex();
  if (!index) return [];
  const normalized = userPrompt.toLowerCase();
  return index.skills
    .filter((s) => s.triggers.some((t) => normalized.includes(t.toLowerCase())))
    .map((s) => s.name);
}

export function getRelevantSkillsContent(userPrompt: string): string {
  const names = findRelevantSkills(userPrompt);
  if (names.length === 0) return "";
  const sections = names.map((n) => loadSkillContent(n)).filter((c): c is string => !!c);
  if (sections.length === 0) return "";
  return "\n\n---\nContexto adicional específico de Wren:\n\n" + sections.join("\n\n---\n\n");
}
