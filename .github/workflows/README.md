# GitHub Actions — Secrets necessários

Configure em **Settings → Secrets and variables → Actions** do(s) repositório(s).

## Workflow `android.yml` (CI Android)
| Secret | Valor de exemplo | Uso |
|---|---|---|
| `SUPABASE_URL` | `https://ljeroorpbqnlggiyyttb.supabase.co` | `local.properties` em build CI |
| `SUPABASE_ANON_KEY` | `sb_publishable_...` | idem |
| `WHATSAPP_SUPORTE` | `5519999429110` | idem |

## Workflow `backend.yml` (CI Backend)
Sem secrets — apenas `deno fmt/lint/check` + `deno test`.

## Workflow `backend-deploy.yml` (Deploy Backend)
> Cria também um **environment** chamado `production` no GitHub para exigir aprovação manual antes de cada deploy.

| Secret | Onde obter | Uso |
|---|---|---|
| `SUPABASE_ACCESS_TOKEN` | https://supabase.com/dashboard/account/tokens | autentica o CLI |
| `SUPABASE_PROJECT_ID` | `ljeroorpbqnlggiyyttb` | linka projeto |
| `SUPABASE_DB_PASSWORD` | dashboard → Database → Connection | aplica migrations |

## Triggers
- **android.yml** / **backend.yml**: roda em PRs e push para `main` quando arquivos relevantes mudam.
- **backend-deploy.yml**: roda apenas em push para `main` (após CI verde) quando `migrations/`, `functions/` ou `config.toml` mudam, ou via `workflow_dispatch` manual.
