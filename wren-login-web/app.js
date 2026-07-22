const API_BASE = window.WREN_API_BASE || "https://wren-backend-vercel.vercel.app/api";
const state = { mode: "login" };

const i18n = {
  es: {
    subtitle: "AI that builds the future",
    headline: "Create. Build. Launch.",
    copy: "Inicia sesión para entrar al workspace, crear proyectos y abrir tu terminal real.",
    login: "Iniciar sesión",
    register: "Crear cuenta",
    emailLabel: "Correo",
    passwordLabel: "Contraseña",
    terms: "Al continuar aceptas los Términos del Consumidor, la Política de Uso y la Política de Privacidad.",
    errorLogin: "No se pudo autenticar. Inténtalo de nuevo.",
    errorRegister: "No se pudo crear la cuenta. Inténtalo de nuevo.",
    loadingLogin: "Iniciando sesión...",
    loadingRegister: "Creando cuenta...",
    successLogin: "Sesión iniciada",
    successRegister: "Cuenta creada"
  },
  en: {
    subtitle: "AI that builds the future",
    headline: "Create. Build. Launch.",
    copy: "Sign in to enter the workspace, create projects, and open your real terminal.",
    login: "Sign in",
    register: "Create account",
    emailLabel: "Email",
    passwordLabel: "Password",
    terms: "By continuing, you accept the Consumer Terms, Usage Policy, and Privacy Policy.",
    errorLogin: "Authentication failed. Please try again.",
    errorRegister: "Could not create the account. Please try again.",
    loadingLogin: "Signing in...",
    loadingRegister: "Creating account...",
    successLogin: "Signed in",
    successRegister: "Account created"
  },
  pt: {
    subtitle: "AI that builds the future",
    headline: "Create. Build. Launch.",
    copy: "Entre para acessar o workspace, criar projetos e abrir o terminal real.",
    login: "Entrar",
    register: "Criar conta",
    emailLabel: "Email",
    passwordLabel: "Senha",
    terms: "Ao continuar, você aceita os Termos do Consumidor, a Política de Uso e a Política de Privacidade.",
    errorLogin: "Falha na autenticação. Tente novamente.",
    errorRegister: "Não foi possível criar a conta. Tente novamente.",
    loadingLogin: "Entrando...",
    loadingRegister: "Criando conta...",
    successLogin: "Entrou",
    successRegister: "Conta criada"
  }
};

const el = (id) => document.getElementById(id);

function langKey() {
  const raw = (navigator.languages && navigator.languages[0]) || navigator.language || "en";
  const short = raw.toLowerCase().split("-")[0];
  return i18n[short] ? short : "en";
}

function t(key) {
  return i18n[langKey()][key] || i18n.en[key] || key;
}

function paint() {
  document.documentElement.lang = langKey() === "pt" ? "pt-BR" : langKey() === "es" ? "es" : "en";
  document.querySelectorAll("[data-i18n]").forEach((node) => {
    const key = node.getAttribute("data-i18n");
    node.textContent = t(key);
  });
  el("submitBtn").textContent = t(state.mode);
  el("submitBtn").setAttribute("data-i18n", state.mode);
  el("loginTab").classList.toggle("active", state.mode === "login");
  el("registerTab").classList.toggle("active", state.mode === "register");
}

function showError(message) {
  const error = el("error");
  error.textContent = message;
  error.hidden = false;
}

function bridgeError(message) {
  if (window.WrenAuthBridge && typeof window.WrenAuthBridge.onLoginError === "function") {
    window.WrenAuthBridge.onLoginError(message);
    return true;
  }
  return false;
}

function clearError() {
  const error = el("error");
  error.hidden = true;
  error.textContent = "";
}

function switchMode(mode) {
  state.mode = mode;
  paint();
  clearError();
  el("submitBtn").textContent = t(state.mode);
}

function bridgeSuccess(data) {
  localStorage.setItem("wren_token", data.token || "");
  if (data.user) localStorage.setItem("wren_user", JSON.stringify(data.user));
  if (window.WrenAuthBridge && typeof window.WrenAuthBridge.onLoginSuccess === "function") {
    window.WrenAuthBridge.onLoginSuccess(JSON.stringify(data));
    return;
  }
  document.body.innerHTML = `
    <div style="min-height:100vh;display:grid;place-items:center;background:#050607;color:#f3f5f7;font-family:Inter,system-ui,sans-serif;padding:24px">
      <div style="max-width:520px;width:100%;background:rgba(16,18,21,.95);border:1px solid rgba(255,255,255,.09);border-radius:28px;padding:28px;text-align:center;box-shadow:0 30px 80px rgba(0,0,0,.48)">
        <div style="width:72px;height:72px;border-radius:22px;background:rgba(255,255,255,.04);margin:0 auto 18px;display:grid;place-items:center;color:#26e6ff;font-size:30px">〈〉</div>
        <h1 style="margin:0 0 10px;font-size:32px;letter-spacing:.18em">WREN</h1>
        <p style="margin:0 0 18px;color:#9ca3af">${data.message || "OK"}</p>
        <p style="margin:0;color:#9ca3af">Token guardado correctamente.</p>
      </div>
    </div>`;
}

async function submit() {
  const email = el("email").value.trim();
  const password = el("password").value;
  const submitBtn = el("submitBtn");

  if (!email || !password) {
    showError(langKey() === "es" ? "Completa tu correo y tu contraseña." : "Fill in email and password.");
    return;
  }

  clearError();
  submitBtn.disabled = true;
  submitBtn.textContent = state.mode === "login" ? t("loadingLogin") : t("loadingRegister");

  try {
    const res = await fetch(`${API_BASE}/auth`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        mode: state.mode,
        email,
        password
      })
    });

    const data = await res.json();

    if (!res.ok) {
      const message = data.error || (state.mode === "login" ? t("errorLogin") : t("errorRegister"));
      if (!bridgeError(message)) showError(message);
      return;
    }

    bridgeSuccess(data);
  } catch (err) {
    const message = err.message || (state.mode === "login" ? t("errorLogin") : t("errorRegister"));
    if (!bridgeError(message)) showError(message);
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = t(state.mode);
  }
}

document.addEventListener("DOMContentLoaded", () => {
  paint();
  el("loginTab").addEventListener("click", () => switchMode("login"));
  el("registerTab").addEventListener("click", () => switchMode("register"));
  el("authForm").addEventListener("submit", (e) => {
    e.preventDefault();
    submit();
  });
  document.querySelectorAll("[data-provider]").forEach((btn) => {
    btn.addEventListener("click", () => {
      showError(`${btn.getAttribute("data-provider")} ${langKey() === "es" ? "aún no está conectado." : "is not connected yet."}`);
    });
  });
});
