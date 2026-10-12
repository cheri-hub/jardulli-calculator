# Pendências — JardulliCalculadora

> **Documento vivo.** Deve ser atualizado a cada task concluída (regra no `CLAUDE.md`):
> mover o item para "Resolvido" com data e PR, e registrar o que for descoberto no caminho.
> **Última atualização:** 12/out/2026 · ordem de execução em [`PROXIMOS_PASSOS.md`](PROXIMOS_PASSOS.md)

Legenda: ⬜ aberto · 🔍 a confirmar · ✅ resolvido

---

## 1. Para ir a produção

| # | Item | Status | Quem |
|---|---|---|---|
| P1 | **Cutover do Asaas sandbox → produção** (chave `$aact_prod_`, `ASAAS_BASE_URL`, novo `ASAAS_WEBHOOK_TOKEN`, webhook na conta de produção, smoke test com cartão real). Runbook: [`OPS_ASAAS_PRODUCAO.md`](OPS_ASAAS_PRODUCAO.md) | 🔍 usuário diz que já foi feito. Indício (12/out): `ASAAS_API_KEY` e `ASAAS_BASE_URL` foram alterados juntos em 02/ago 14:30–14:31. O valor da URL não dá para ver pelo CLI, então **não está provado**. Confirmar: checkout abre em `www.asaas.com`, webhook ativo no painel de produção e ao menos um `webhook_events` com `processed = true` depois de 02/ago | Usuário |
| P2 | **Rotacionar a service-role key** que vazou em texto puro em docs antigos (já redigida no repo, mas a chave segue válida até ser trocada no painel). Hoje ela também está em texto puro no `.secrets` das duas cópias do backend | ⬜ | Usuário |
| P3 | **Testar em aparelho real** o que foi entregue sem teste manual: PDF no Android 10+ (gera e abre), transferência de dispositivo (Transferir / Cancelar), badge "CANCELADA — Ativa até DD/MM" após cancelar plano pago | ⬜ | Usuário |
| P4 | **Release assinado**: keystore (`STORE_FILE`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` no `local.properties`), `versionCode`/`versionName` (hoje 1 / "1.0"), decidir `isMinifyEnabled` (hoje `false`; se ligar, o iText provavelmente precisa de regras no `proguard-rules.pro`) | ⬜ | Usuário + Claude |
| P5 | **Ficha da Play Store**: política de privacidade, screenshots, classificação etária | ⬜ | Usuário |
| P6 | Environment `production` do repo do backend **sem *required reviewers***: todo push em `supabase/` na `main` faz deploy direto em produção | ⬜ opcional | Usuário |
| P7 | Secret `SUPABASE_DB_PASSWORD` do environment `production` é de 26/abr, mas o `.secrets` foi alterado em 02/ago. Se a senha do banco realmente mudou, atualizar o secret (senão o próximo deploy falha em `supabase link`) | 🔍 | Usuário |
| P8 | `asaas.txt` na raiz do app guarda chaves reais em texto puro (gitignored). Após o cutover, mover para um gerenciador de senhas e apagar o arquivo | ⬜ | Usuário |
| P9 | `ASAAS_WEBHOOK_TOKEN` no Supabase não é alterado desde **18/jun** (era o de sandbox); o runbook pede um token novo para produção. Se o `authToken` do webhook de produção no Asaas for diferente do secret, todo webhook volta `401` e assinaturas pagas não ativam. Garantir que os dois são iguais (ou gerar um novo e atualizar os dois) | 🔍 | Usuário |
| P10 | **Recuperação de senha ausente**: o app não tem "esqueci minha senha" (só `signup`, login, `logout` e `refresh` no `SupabaseClient`). Quem esquece a senha perde a conta e o plano pago. Plano: fluxo por código de 6 dígitos (`/auth/v1/recover` → `/auth/v1/verify` → `PUT /auth/v1/user`) + template de e-mail com `{{ .Token }}`. Detalhes em [`PROXIMOS_PASSOS.md`](PROXIMOS_PASSOS.md) (Fase 1) | ⬜ | Claude + Usuário |
| P11 | **SMTP próprio no Supabase**: o e-mail padrão tem limite baixo de envios e atende cadastro (confirmação) e recuperação de senha. Configurar em Authentication → SMTP Settings antes do lançamento | ⬜ | Usuário |

---

## 2. Melhorias e inconsistências conhecidas

- ⬜ **Aparelho desconectado por transferência cai silenciosamente** para TESTDRIVE no próximo sync; falta avisar o usuário no aparelho antigo.
- ⬜ **Single-device é só client-side**: o JWT do aparelho antigo continua válido no servidor até expirar.
- ⬜ **Docs obsoletos** (ainda descrevem o AbacatePay): `OPS_ABACATE_PRODUCTS_SETUP.md`, `OPS_WEBHOOK_ABACATEPAY.md`, `OPS_E2E_TEST.md` (precisa de reescrita para Asaas/cartão) e `backend/README.md`. `PROXIMOS_PASSOS.md` foi reescrito em 12/out (plano real); `PROXIMOS_PASSOS_TESTES_ASAAS.md` segue obsoleto (diz que há 0 testes Deno e sandbox pendente).
- ⬜ **Casos de uso**: `CASOS_DE_USO.md` (UC-6.2) diz que o PDF usa MediaStore — passou a ser verdade só após o fix de 02/ago; reconferir o texto.
- ⬜ Duas cópias do backend (`backend/` e `C:\repo\jardulli-subscription`): hoje sincronizadas, mas podem divergir de novo. Trabalhar só pelo `backend/`.
- ⬜ Supabase CLI local está na 2.51.0 (atual: 2.120.0). O login é global: verificar com `supabase projects list` que `Jardulli Subscriptions` aparece antes de usar o CLI (ver `CLAUDE.md`).

---

## 3. Débitos técnicos (herdados da revisão de 25/abr/2026, ainda abertos)

**Médios**
- ⬜ **M1 — i18n:** `strings.xml` tem só `app_name`; todo texto está hardcoded em PT-BR.
- ⬜ **M4 — `DadosJardulli`:** Parcelable manual com 43+ campos; propenso a erro ao adicionar campo. Sugestão: quebrar em sub-DTOs por etapa do wizard.
- ⬜ **M5 — `DBHelper` sem `onDowngrade()`:** instalar build com `DATABASE_VERSION` menor crasha o app.
- ⬜ **M6 — `HistoricoActivity.mesclar()`:** dedup por `(minuto + modelo + peso)` descarta 2 balanços iguais no mesmo minuto; preferir `remoteId`.

**Baixos**
- ⬜ **L1 — Acessibilidade:** quase nenhum `contentDescription` nos layouts.
- ⬜ **L2 — Offline:** checar `ConnectivityManager` antes de chamar o Supabase (hoje falha por timeout de 15s).
- ⬜ **L3 — Logs** não estruturados.
- ⬜ **L4 — Planos legados `pro`/`premium`** continuam como `is_active = false`; remover em migration futura, depois de confirmar zero assinaturas apontando para eles.

**Funcionalidade e infra**
- ⬜ **F1 — Patinagem/Avanço:** `AvancoRatioActivity` valida `velocidadeTeorica`; conferir `velocidadeReal` e integração com o Histórico.
- ⬜ **DT1 — `security-crypto:1.1.0-alpha06`** em produção (risco aceito; sem GA).
- ⬜ **DT2 — Cobertura de testes:** app com 27 testes (`CalculoUtil` 15 + `DocumentValidator` 12); backend com 29 testes Deno (webhook, client, validador). Faltam: `PlanoManager.podeFazerBalanco()`, `mesclar()`, Espresso (Auth → Perfil → Balanço) e testes das functions `subscribe`/`cancel-subscription`.
- ⬜ **DT4 — `local.properties.example`** não existe; documentar `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `WHATSAPP_SUPORTE` e as chaves de assinatura.

---

## 4. Resolvido

| Data | Item | Referência |
|---|---|---|
| 12/out/2026 | Edge Function órfã `webhook-abacatepay` removida de produção (continuava ativa e pública depois da migração) | `supabase functions delete` |
| 12/out/2026 | Verificado via CLI: 18/18 migrations locais aplicadas no remoto; 4 Edge Functions do Asaas `ACTIVE` (deploy de 02/ago) | — |
| 02/ago/2026 | CI do backend e deploy do Supabase movidos para o repo correto (`supabase-ci.yml`, `supabase-deploy.yml`); deploy validado em produção | jardulli-subscription#3, jardulli-calculator#4 |
| 02/ago/2026 | CI do app corrigido (verde pela 1ª vez): removido `org.gradle.java.home` local do `gradle.properties` | jardulli-calculator#3 |
| 02/ago/2026 | PDF não abria no Android 10+: gravação via `MediaStore.Downloads`; conflito de nomes no `file_paths.xml` | jardulli-calculator#3 |
| 02/ago/2026 | Confirmação antes de transferir o acesso a um novo aparelho | jardulli-calculator#3 |
| 28/jun/2026 | UI de cancelamento ("CANCELADA — Ativa até DD/MM") + `cancel_requested_at` no RPC | jardulli-calculator#3, jardulli-subscription#2 |
| 28/jun/2026 | Webhook Asaas não ativava assinatura: passa a casar por `checkoutSession` | jardulli-subscription#2 |
| 27/jun/2026 | `subscribe` sem `customerData` (Asaas exigia endereço); RPC não conta `pending` como ativo; login re-checado antes de assinar | jardulli-subscription#2, jardulli-calculator#2 |
| 20/jun/2026 | Migração AbacatePay → Asaas (schema, functions, webhook, 29 testes Deno) | jardulli-subscription#1 |
| — | **M2** (retry do webhook AbacatePay): obsoleto, o AbacatePay foi removido. O webhook Asaas responde 500 em erro de handler para o Asaas retentar | — |
| — | **M3** (rate-limit nas functions): `subscribe` e `cancel-subscription` usam `_shared/rate-limit.ts` (5 req / 60 s) | migration `20260426000200` |
| — | **DT3** (CI/CD): app e backend com pipeline de CI; backend com deploy automático | ver acima |
| 25/abr/2026 | C1–C3, H1–H5 (RLS do histórico, refresh de token, validação de CPF/CNPJ, testes de cálculo etc.) | revisão de abril |

Também entregues: `CASOS_DE_USO.md` (99 casos), `OPS_ASAAS_PRODUCAO.md`, `CLAUDE.md` reescrito.
