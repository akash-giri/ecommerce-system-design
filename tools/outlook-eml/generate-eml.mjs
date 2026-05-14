import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";
import crypto from "node:crypto";

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function normalizeBody(text) {
  return String(text ?? "")
    .replaceAll("[cite_start]", "")
    .replaceAll("\r\n", "\n")
    .replaceAll("\r", "\n");
}

function escapeHtml(text) {
  return String(text)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function formatPeopleLine(people) {
  return (people ?? [])
    .map((p) => {
      const name = String(p?.name ?? "").trim();
      const email = String(p?.email ?? "").trim();
      if (!email) return name || "";
      return `${name || email} <${email}>`;
    })
    .filter(Boolean)
    .join("; ");
}

function formatAddressHeader(people, joiner = ", ") {
  return (people ?? [])
    .map((p) => {
      const name = String(p?.name ?? "").trim();
      const email = String(p?.email ?? "").trim();
      assert(email, "Person.email is required.");
      const display = name ? `"${name.replaceAll('"', '\\"')}" ` : "";
      return `${display}<${email}>`;
    })
    .join(joiner);
}

function parseDateWithOffset(input, fallbackOffsetMinutes = 0) {
  if (!input) return { ms: Date.now(), offsetMinutes: fallbackOffsetMinutes };

  const s = String(input).trim();
  const m = s.match(/^(.*?)(Z|[+-]\d{2}:?\d{2})$/);
  let offsetMinutes = fallbackOffsetMinutes;
  if (m) {
    const suffix = m[2];
    if (suffix === "Z") offsetMinutes = 0;
    else {
      const sign = suffix.startsWith("-") ? -1 : 1;
      const digits = suffix.slice(1).replace(":", "");
      const hh = Number(digits.slice(0, 2));
      const mm = Number(digits.slice(2, 4));
      offsetMinutes = sign * (hh * 60 + mm);
    }
  }

  const dt = new Date(s);
  assert(!Number.isNaN(dt.getTime()), `Invalid date: ${input}`);
  return { ms: dt.getTime(), offsetMinutes };
}

function formatRfc2822({ ms, offsetMinutes }) {
  const days = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
  const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

  const local = new Date(ms + offsetMinutes * 60_000);
  const d = days[local.getUTCDay()];
  const dd = String(local.getUTCDate()).padStart(2, "0");
  const m = months[local.getUTCMonth()];
  const yyyy = local.getUTCFullYear();
  const hh = String(local.getUTCHours()).padStart(2, "0");
  const mm = String(local.getUTCMinutes()).padStart(2, "0");
  const ss = String(local.getUTCSeconds()).padStart(2, "0");

  const sign = offsetMinutes < 0 ? "-" : "+";
  const abs = Math.abs(offsetMinutes);
  const offH = String(Math.floor(abs / 60)).padStart(2, "0");
  const offM = String(abs % 60).padStart(2, "0");
  return `${d}, ${dd} ${m} ${yyyy} ${hh}:${mm}:${ss} ${sign}${offH}${offM}`;
}

function formatOutlookSent({ ms, offsetMinutes }) {
  const weekdays = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
  const months = [
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December",
  ];

  const local = new Date(ms + offsetMinutes * 60_000);
  const weekday = weekdays[local.getUTCDay()];
  const month = months[local.getUTCMonth()];
  const day = local.getUTCDate();
  const year = local.getUTCFullYear();

  const h24 = local.getUTCHours();
  const minutes = String(local.getUTCMinutes()).padStart(2, "0");
  const ampm = h24 >= 12 ? "PM" : "AM";
  const h12 = h24 % 12 || 12;

  return `${weekday}, ${month} ${day}, ${year} ${h12}:${minutes} ${ampm}`;
}

function qpEncode(input) {
  const bytes = Buffer.isBuffer(input) ? input : Buffer.from(String(input), "utf8");
  let out = "";
  let line = "";

  const push = (chunk) => {
    if (line.length + chunk.length > 75) {
      out += `${line}=\r\n`;
      line = "";
    }
    line += chunk;
  };

  for (const b of bytes) {
    if (b === 0x0a) {
      out += `${line}\r\n`;
      line = "";
      continue;
    }
    if (b === 0x0d) continue;

    const isTabOrSpace = b === 0x09 || b === 0x20;
    const isPrintable = b >= 33 && b <= 126 && b !== 61;

    if (isPrintable) push(String.fromCharCode(b));
    else if (isTabOrSpace) push(String.fromCharCode(b));
    else push(`=${b.toString(16).toUpperCase().padStart(2, "0")}`);
  }

  out += line;
  return out;
}

let defaultOffsetMinutes = 0;

function sentLineFromMessage(msg) {
  const iso = String(msg?.sent_iso ?? "").trim();
  if (iso) return formatOutlookSent(parseDateWithOffset(iso, defaultOffsetMinutes));
  return String(msg?.sent ?? "").trim();
}

function makePlaintextThread(body, previousMessages) {
  const lines = [];
  lines.push(normalizeBody(body).trimEnd());
  lines.push("");

  const separator = "_".repeat(93);
  for (const msg of previousMessages ?? []) {
    lines.push(separator);

    const from = msg?.from ?? {};
    const fromName = String(from?.name ?? "").trim() || String(from?.email ?? "").trim();
    const fromEmail = String(from?.email ?? "").trim();
    const sent = sentLineFromMessage(msg);
    const to = Array.isArray(msg?.to) ? msg.to : [];
    const cc = Array.isArray(msg?.cc) ? msg.cc : [];
    const subject = String(msg?.subject ?? "").trim();
    const msgBody = normalizeBody(msg?.body ?? "").trimEnd();

    lines.push(`From: ${fromName}${fromEmail ? ` <${fromEmail}>` : ""} `);
    if (sent) lines.push(`Sent: ${sent} `);
    if (to.length) lines.push(`To: ${formatPeopleLine(to)} `);
    if (cc.length) lines.push(`Cc: ${formatPeopleLine(cc)} `);
    if (subject) lines.push(`Subject: ${subject} `);
    lines.push("");
    if (msgBody) {
      lines.push(...msgBody.split("\n"));
      lines.push("");
    }
  }

  return `${lines.join("\n").trimEnd()}\n`;
}

function makeHtmlThread(body, previousMessages) {
  const baseStyle = "font-family:Calibri,Arial,sans-serif;font-size:11pt;color:#000;";
  const headerLabelStyle = "font-weight:700;";
  const hrStyle = "border:none;border-top:1px solid #BFBFBF;margin:18px 0;";

  const p = (t) => escapeHtml(String(t)).replaceAll("\n", "<br>");

  let html = "";
  html += "<html><head>";
  html += '<meta http-equiv="Content-Type" content="text/html; charset=utf-8">';
  html += "</head>";
  html += `<body style="${baseStyle}">`;

  const top = normalizeBody(body).trim();
  html += `<div>${p(top)}</div>`;

  for (const msg of previousMessages ?? []) {
    const from = msg?.from ?? {};
    const fromName = String(from?.name ?? "").trim() || String(from?.email ?? "").trim();
    const fromEmail = String(from?.email ?? "").trim();
    const sent = sentLineFromMessage(msg);
    const to = Array.isArray(msg?.to) ? msg.to : [];
    const cc = Array.isArray(msg?.cc) ? msg.cc : [];
    const subject = String(msg?.subject ?? "").trim();
    const msgBody = normalizeBody(msg?.body ?? "").trimEnd();

    html += `<hr style="${hrStyle}">`;
    html += '<div style="margin-top:6px;">';

    html += `<div><span style="${headerLabelStyle}">From:</span> ${p(
      `${fromName}${fromEmail ? ` <${fromEmail}>` : ""}`,
    )}</div>`;
    if (sent) html += `<div><span style="${headerLabelStyle}">Sent:</span> ${p(sent)}</div>`;
    if (to.length) html += `<div><span style="${headerLabelStyle}">To:</span> ${p(formatPeopleLine(to))}</div>`;
    if (cc.length) html += `<div><span style="${headerLabelStyle}">Cc:</span> ${p(formatPeopleLine(cc))}</div>`;
    if (subject) html += `<div><span style="${headerLabelStyle}">Subject:</span> ${p(subject)}</div>`;
    html += "</div>";

    if (msgBody) {
      html += '<div style="margin-top:12px;">';
      html += p(msgBody);
      html += "</div>";
    }
  }

  html += "</body></html>";
  return html;
}

function randomThreadIndexBase64() {
  return crypto.randomBytes(22).toString("base64");
}

function makeMessageId(fromEmail) {
  const domain = String(fromEmail || "").split("@")[1] || "local";
  return `<${crypto.randomUUID()}@${domain}>`;
}

function buildEml(payload) {
  const subject = String(payload?.subject ?? "").trim();
  assert(subject, "subject is required.");

  defaultOffsetMinutes = Number(payload?.timezone_offset_minutes ?? 0);
  if (!Number.isFinite(defaultOffsetMinutes)) defaultOffsetMinutes = 0;

  const from = payload?.from ?? {};
  const fromName = String(from?.name ?? "").trim();
  const fromEmail = String(from?.email ?? "").trim();
  assert(fromEmail, "from.email is required.");

  const to = Array.isArray(payload?.to) ? payload.to : [];
  const cc = Array.isArray(payload?.cc) ? payload.cc : [];

  const date = formatRfc2822(parseDateWithOffset(payload?.date, defaultOffsetMinutes));
  const threadTopic = String(payload?.thread_topic ?? "").trim();
  const body = String(payload?.body ?? "");
  const previousMessages = Array.isArray(payload?.previous_messages) ? payload.previous_messages : [];

  const boundary = String(payload?.boundary ?? "").trim() || `_000_${crypto.randomBytes(16).toString("hex").toUpperCase()}`;

  const plain = makePlaintextThread(body, previousMessages);
  const html = makeHtmlThread(body, previousMessages);

  const lines = [];
  lines.push(`From: "${fromName || fromEmail}" <${fromEmail}>`);
  if (to.length) lines.push(`To: ${formatAddressHeader(to)}`);
  if (cc.length) lines.push(`CC: ${formatAddressHeader(cc)}`);
  lines.push(`Subject: ${subject}`);
  if (threadTopic) {
    lines.push(`Thread-Topic: ${threadTopic}`);
    lines.push(`Thread-Index: ${randomThreadIndexBase64()}`);
  }
  lines.push(`Date: ${date}`);
  lines.push(`Message-ID: ${makeMessageId(fromEmail)}`);
  lines.push("MIME-Version: 1.0");
  lines.push("Content-Type: multipart/alternative;");
  lines.push(`\tboundary="${boundary}"`);
  lines.push("");

  lines.push(`--${boundary}`);
  lines.push('Content-Type: text/plain; charset="utf-8"');
  lines.push("Content-Transfer-Encoding: quoted-printable");
  lines.push("");
  lines.push(qpEncode(plain));
  lines.push("");

  lines.push(`--${boundary}`);
  lines.push('Content-Type: text/html; charset="utf-8"');
  lines.push("Content-Transfer-Encoding: quoted-printable");
  lines.push("");
  lines.push(qpEncode(html));
  lines.push("");

  lines.push(`--${boundary}--`);
  lines.push("");

  return lines.join("\r\n");
}

function parseArgs(argv) {
  const args = { input: null, output: null };
  for (let i = 0; i < argv.length; i += 1) {
    const a = argv[i];
    if (a === "--input") args.input = argv[i + 1];
    else if (a === "--output") args.output = argv[i + 1];
  }
  assert(args.input, "Missing --input <path>");
  assert(args.output, "Missing --output <path>");
  return args;
}

function main() {
  const args = parseArgs(process.argv.slice(2));
  const inputPath = resolve(args.input);
  const outputPath = resolve(args.output);

  const payload = JSON.parse(readFileSync(inputPath, "utf8"));
  const eml = buildEml(payload);

  mkdirSync(dirname(outputPath), { recursive: true });
  writeFileSync(outputPath, eml, "utf8");
}

main();

