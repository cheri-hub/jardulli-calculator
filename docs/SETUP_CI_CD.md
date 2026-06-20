# Setup CI/CD — GitHub Actions

> Passo a passo para habilitar os workflows de CI e Deploy adicionados em
> `.github/workflows/`. Execute **uma vez** após clonar o repositório no GitHub.

---

## 1. Workflows incluídos

| Arquivo | Quando roda | O que faz |
|---|---|---|
| [`.github/workflows/android.yml`](../.github/workflows/android.yml) | PR ou push em `main` que toque `app/`, `gradle/`, `*.gradle.kts` | `lintDebug`, `testDebugUnitTest`, `assembleDebug`, sobe APK e relatórios como artifacts |
| [`.github/workflows/backend.yml`](../.github/workflows/backend.yml) | PR ou push em `main` que toque `backend/` | `deno fmt --check`, `deno lint`, `deno check` em cada Edge Function, `deno test` se houver |
| [`.github/workflows/backend-deploy.yml`](../.github/workflows/backend-deploy.yml) | Push em `main` que toque `backend/supabase/migrations/`, `functions/` ou `config.toml`, ou disparo manual (`workflow_dispatch`) | `supabase link` → `supabase db push` → `supabase functions deploy` para todas as funções (exceto `_shared`) |

> O deploy roda no environment `production`, então é possível exigir aprovação manual em **Settings → Environments → production → Required reviewers**.

---

## 2. Criar o environment `production`

1. No GitHub do repositório → **Settings → Environments → New environment**.
2. Nome: `production`.
3. (Opcional, recomendado) marcar **Required reviewers** e adicionar você mesmo. Cada deploy ficará pendente até aprovação manual.

---

## 3. Configurar secrets

**Settings → Secrets and variables → Actions → New repository secret**.

### 3.1 Secrets para `android.yml`
Esses cobrem o build do app (precisam estar disponíveis no escopo do repositório):

| Secret | Valor | Origem |
|---|---|---|
| `SUPABASE_URL` | `https://ljeroorpbqnlggiyyttb.supabase.co` | `backend/.secrets` → `SUPABASE_PROJECT_URL` |
| `SUPABASE_ANON_KEY` | `sb_publishable_dKqXfOxDyfyotLmd1s6S8g_RapT-SNY` | `backend/.secrets` → `SUPABASE_PUBLISHABLE_KEY` |
| `WHATSAPP_SUPORTE` | `5519999429110` | configurado em `local.properties` |

### 3.2 Secrets para `backend-deploy.yml`
Defina **dentro do environment `production`** (Settings → Environments → production → Add secret):

| Secret | Onde obter |
|---|---|
| `SUPABASE_ACCESS_TOKEN` | <https://supabase.com/dashboard/account/tokens> → **Generate new token** |
| `SUPABASE_PROJECT_ID` | `ljeroorpbqnlggiyyttb` |
| `SUPABASE_DB_PASSWORD` | Dashboard → Settings → Database → Database password (use o atual `sO3WHlP0Dc9eLDBF` ou rotacione) |

> **Backend (Supabase) também precisa dos secrets de runtime** já configurados:
> `ABACATEPAY_API_KEY`, `ABACATEPAY_WEBHOOK_SECRET`. Eles ficam no Supabase
> (`supabase secrets set …`), **não** no GitHub.

### 3.3 (Opcional) backend.yml
Sem secrets — apenas validação estática.

---

## 4. Passo a passo no Supabase (uma vez)

```bash
# Gere o access token e armazene como SUPABASE_ACCESS_TOKEN no GitHub
supabase login

# Confira que o project ref está correto
supabase projects list

# Garanta que os secrets de runtime do AbacatePay estão setados no projeto
supabase secrets set ABACATEPAY_API_KEY=abc_dev_xxxx --project-ref ljeroorpbqnlggiyyttb
supabase secrets set ABACATEPAY_WEBHOOK_SECRET=xxxxxxxx --project-ref ljeroorpbqnlggiyyttb
```

---

## 5. Verificar

1. Faça um PR pequeno tocando `app/` → o workflow **Android CI** deve rodar.
2. Faça um PR tocando `backend/supabase/functions/` → **Backend CI** deve rodar.
3. Merge na `main` com mudanças em `backend/` → **Backend Deploy** entra em fila aguardando aprovação no environment `production` (se reviewers exigidos).
4. Aprovar → migrations aplicadas e functions republicadas automaticamente.

---

## 6. Rollback de deploy

Se um deploy quebrar produção:

```bash
# Reverter Edge Function para a versão anterior pelo dashboard:
# https://supabase.com/dashboard/project/ljeroorpbqnlggiyyttb/functions/<nome>/versions

# Reverter migration: criar nova migration que faça o DROP / UPDATE inverso
# (NUNCA editar uma migration já aplicada — Supabase rastreia por hash).
supabase migration new rollback_<descricao>
# escreva o SQL inverso
git commit -am "fix: rollback <descricao>"
git push  # workflow aplica automaticamente
```

---

## 7. Notas de segurança

- A senha `SUPABASE_DB_PASSWORD` está exposta neste repo em `backend/.secrets`. **Adicione `.secrets` ao `.gitignore`** se ainda não estiver, e considere rotacioná-la pelo dashboard.
- O `SUPABASE_ACCESS_TOKEN` permite ações administrativas em **todos** os projetos da conta. Use um token dedicado para CI e revogue se vazar.
- O `ABACATEPAY_WEBHOOK_SECRET` deve coincidir com o configurado no painel da AbacatePay.
