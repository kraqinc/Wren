const API_BASE = window.WREN_API_BASE || "https://YOUR-BACKEND.vercel.app/api";
const state = { email: "" };

const i18n = {
  es: {
    headline: "Crea. Construye. <em>Lanza.</em>",
    emailLabel: "Correo",
    continue: "Continuar",
    or: "o",
    continueGoogle: "Continuar con Google",
    continueGithub: "Continuar con GitHub",
    codeLabel: "Código de 6 dígitos",
    verify: "Verificar",
    changeEmail: "Cambiar correo",
    terms: "Al continuar aceptas los Términos del Consumidor, la Política de Uso y la Política de Privacidad de Wren.",
    errorEmail: "Ingresa un correo válido.",
    errorRequestCode: "No se pudo enviar el código. Inténtalo de nuevo.",
    errorVerify: "Código inválido o expirado.",
    loadingRequest: "Enviando código...",
    loadingVerify: "Verificando...",
    codeStepCopyExisting: (email) => `Enviamos un código a ${email}. Ya tienes cuenta — este código inicia tu sesión.`,
    codeStepCopyNew: (email) => `Enviamos un código a ${email}. Es tu primera vez — este código crea tu cuenta.`,
    oauthNotConnected: (provider) => `${provider} aún no está conectado.`
  },
  en: {
    headline: "Create. Build. <em>Launch.</em>",
    emailLabel: "Email",
    continue: "Continue",
    or: "or",
    continueGoogle: "Continue with Google",
    continueGithub: "Continue with GitHub",
    codeLabel: "6-digit code",
    verify: "Verify",
    changeEmail: "Change email",
    terms: "By continuing, you accept the Consumer Terms, Usage Policy, and Privacy Policy of Wren.",
    errorEmail: "Enter a valid email.",
    errorRequestCode: "Could not send the code. Please try again.",
    errorVerify: "Invalid or expired code.",
    loadingRequest: "Sending code...",
    loadingVerify: "Verifying...",
    codeStepCopyExisting: (email) => `We sent a code to ${email}. You already have an account — this code signs you in.`,
    codeStepCopyNew: (email) => `We sent a code to ${email}. First time here — this code creates your account.`,
    oauthNotConnected: (provider) => `${provider} is not connected yet.`
  }
};

const el = (id) => document.getElementById(id);

function langKey() {
  const raw = (navigator.languages && navigator.languages[0]) || navigator.language || "en";
  const short = raw.toLowerCase().split("-")[0];
  return i18n[short] ? short : "en";
}

function t(key) {
  const dict = i18n[langKey()];
  return (dict && dict[key] !== undefined) ? dict[key] : (i18n.en[key] !== undefined ? i18n.en[key] : key);
}

function paint() {
  document.documentElement.lang = langKey();
  document.querySelectorAll("[data-i18n]").forEach((node) => {
    const value = t(node.getAttribute("data-i18n"));
    if (typeof value === "string") node.innerHTML = value;
  });
}

function showError(target, message) {
  const node = el(target);
  node.textContent = message;
  node.hidden = false;
}
function clearError(target) {
  const node = el(target);
  node.hidden = true;
  node.textContent = "";
}

function bridgeError(message) {
  if (window.WrenAuthBridge && typeof window.WrenAuthBridge.onLoginError === "function") {
    window.WrenAuthBridge.onLoginError(message);
    return true;
  }
  return false;
}

function bridgeSuccess(data) {
  localStorage.setItem("wren_token", data.token || "");
  if (data.user) localStorage.setItem("wren_user", JSON.stringify(data.user));
  if (window.WrenAuthBridge && typeof window.WrenAuthBridge.onLoginSuccess === "function") {
    window.WrenAuthBridge.onLoginSuccess(JSON.stringify(data));
    return;
  }
  document.body.innerHTML = `
    <div style="min-height:100vh;display:grid;place-items:center;background:#000;color:#F2F2F0;font-family:Inter,system-ui,sans-serif;padding:24px">
      <div style="max-width:380px;width:100%;text-align:center">
        <div style="color:#4C8DFF;font-size:28px;margin-bottom:14px">◆</div>
        <h1 style="margin:0 0 10px;font-size:22px;font-family:Newsreader,serif;font-weight:500">Wren</h1>
        <p style="margin:0;color:#8E8E8E;font-size:14px">${data.message || "OK"}</p>
      </div>
    </div>`;
}

function showCodeStep(accountExists) {
  el("emailStep").hidden = true;
  el("codeStep").hidden = false;
  el("codeStepCopy").textContent = accountExists
    ? t("codeStepCopyExisting")(state.email)
    : t("codeStepCopyNew")(state.email);
  el("code").value = "";
  el("code").focus();
}

function showEmailStep() {
  el("codeStep").hidden = true;
  el("emailStep").hidden = false;
  clearError("codeError");
}

async function requestCode(e) {
  e.preventDefault();
  const email = el("email").value.trim();
  if (!email) {
    showError("error", t("errorEmail"));
    return;
  }

  clearError("error");
  state.email = email;
  const btn = el("requestCodeBtn");
  btn.disabled = true;
  const originalText = btn.textContent;
  btn.textContent = t("loadingRequest");

  try {
    const res = await fetch(`${API_BASE}/auth/request-code`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email })
    });
    const data = await res.json();

    if (!res.ok) {
      const message = data.error || t("errorRequestCode");
      if (!bridgeError(message)) showError("error", message);
      return;
    }

    showCodeStep(data.accountExists);
  } catch (err) {
    const message = err.message || t("errorRequestCode");
    if (!bridgeError(message)) showError("error", message);
  } finally {
    btn.disabled = false;
    btn.textContent = originalText;
  }
}

async function verifyCode(e) {
  e.preventDefault();
  const code = el("code").value.trim();
  if (!/^\d{6}$/.test(code)) {
    showError("codeError", t("errorVerify"));
    return;
  }

  clearError("codeError");
  const btn = el("verifyCodeBtn");
  btn.disabled = true;
  const originalText = btn.textContent;
  btn.textContent = t("loadingVerify");

  try {
    const res = await fetch(`${API_BASE}/auth/verify-code`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: state.email, code })
    });
    const data = await res.json();

    if (!res.ok) {
      const message = data.error || t("errorVerify");
      if (!bridgeError(message)) showError("codeError", message);
      return;
    }

    bridgeSuccess(data);
  } catch (err) {
    const message = err.message || t("errorVerify");
    if (!bridgeError(message)) showError("codeError", message);
  } finally {
    btn.disabled = false;
    btn.textContent = originalText;
  }
}

function handleOAuth(provider) {
  // Google -> wren-google.onrender.com, GitHub -> wren-git.onrender.com
  // (ver skill android-webview-login). Si esos backends de OAuth todavia
  // no estan desplegados, muestra el aviso en vez de romper.
  const redirectBase = provider === "Google"
    ? (window.WREN_GOOGLE_OAUTH_URL || null)
    : (window.WREN_GITHUB_OAUTH_URL || null);

  if (!redirectBase) {
    showError("error", t("oauthNotConnected")(provider));
    return;
  }

  window.location.href = `${redirectBase}?redirect_uri=${encodeURIComponent(window.location.origin)}`;
}

document.addEventListener("DOMContentLoaded", () => {
  paint();
  el("emailStep").addEventListener("submit", requestCode);
  el("codeStep").addEventListener("submit", verifyCode);
  el("backToEmailBtn").addEventListener("click", showEmailStep);
  document.querySelectorAll("[data-provider]").forEach((btn) => {
    btn.addEventListener("click", () => handleOAuth(btn.getAttribute("data-provider")));
  });

  // Si volvemos de un redirect de OAuth con ?token=... en la URL
  const params = new URLSearchParams(window.location.search);
  const token = params.get("token");
  if (token) {
    bridgeSuccess({ token, message: "Sesión iniciada" });
  }
});
