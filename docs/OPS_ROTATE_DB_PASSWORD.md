# Rotacionar senha do banco Supabase

> Use sempre que a senha for exposta (chat, log, screenshot) ou trimestralmente como rotina.

---

## 1. Gerar nova senha no dashboard

1. Acesse <https://supabase.com/dashboard/project/ljeroorpbqnlggiyyttb/settings/database>.
2. Em **Database password** → **Reset database password**.
3. Copie a nova senha imediatamente — não é exibida novamente.

---

## 2. Atualizar arquivos locais

Edite `backend/.secrets` (não versionado):

```ini
SUPABASE_DB_PASS=<nova-senha>
SUPABASE_CONNECTION_STRING=postgresql://postgres:<nova-senha>@db.ljeroorpbqnlggiyyttb.supabase.co:5432/postgres
```

> Se a senha contiver `@`, `:`, `/` ou `#`, faça URL-encode na connection string (ex.: `@` → `%40`).

---

## 3. Atualizar GitHub Actions

1. **Settings → Environments → production → Secrets** do repo `jardulli-subscription`.
2. Edite `SUPABASE_DB_PASSWORD` → cole a nova senha.

> O environment `production` exige aprovação manual; nenhum deploy roda enquanto não atualizado.

---

## 4. Validar conexão

```powershell
cd c:\repo\JardulliCalculadora\backend
supabase db push --include-all --password '<nova-senha>'
```

Saída esperada: `Remote database is up to date.` (sem migrations pendentes).

---

## 5. Re-link do CLI (se necessário)

Se algum colega tiver a senha antiga em cache:

```powershell
supabase link --project-ref ljeroorpbqnlggiyyttb --password '<nova-senha>'
```

---

## 6. Checklist final

- [X] Senha resetada no dashboard
- [X] `backend/.secrets` atualizado localmente
- [X] Secret `SUPABASE_DB_PASSWORD` no GitHub atualizado
- [X] `supabase db push` validado com a nova senha
- [X] Senha antiga descartada de qualquer chat/log
