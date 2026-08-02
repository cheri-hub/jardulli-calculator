# GitHub Actions — Secrets necessários

Configure em **Settings → Secrets and variables → Actions** do(s) repositório(s).

## Workflow `android.yml` (CI Android)
| Secret | Valor de exemplo | Uso |
|---|---|---|
| `SUPABASE_URL` | `https://ljeroorpbqnlggiyyttb.supabase.co` | `local.properties` em build CI |
| `SUPABASE_ANON_KEY` | `sb_publishable_...` | idem |
| `WHATSAPP_SUPORTE` | `5519999429110` | idem |

## Triggers
- **android.yml**: roda em PRs e push para `main` quando arquivos do app mudam.

> Os workflows de CI/deploy do backend (`backend.yml`, `backend-deploy.yml`) foram
> removidos deste repositório — `backend/` é ignorado aqui (`.gitignore`), então
> seus triggers por `paths:` nunca disparavam. Eles agora vivem no repositório
> `cheri-hub/jardulli-subscription` como `supabase-ci.yml` e `supabase-deploy.yml`.
