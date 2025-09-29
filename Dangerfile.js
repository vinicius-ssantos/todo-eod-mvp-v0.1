// Dangerfile.js
const modified = [...danger.git.modified_files, ...danger.git.created_files];
const touchedCode = modified.some(
  (f) => f.startsWith("src") || f.endsWith(".java") || f.endsWith(".kt")
);
const touchedDocs = modified.some(
  (f) => f.startsWith("docs") || f.startsWith("openapi") || f === "README.md"
);

if (touchedCode && !touchedDocs) {
  warn(
    "Mudanças de código sem atualizar docs (README/openapi/docs). Considere rodar `make docs`."
  );
}

if (
  modified.some((f) => f.startsWith("src/main") && f.includes("controller")) &&
  !modified.some((f) => f.startsWith("openapi"))
)
  fail("Controller alterado, mas OpenAPI não foi atualizado.");

