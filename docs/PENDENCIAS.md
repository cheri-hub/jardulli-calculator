# Pendências — JardulliCalculadora

> Documento gerado em **25/abr/2026** após a rodada de correções CRITICAL + HIGH.
> Lista os itens MEDIUM/LOW remanescentes, débitos técnicos e melhorias futuras.

---

## 📋 Resumo

| Severidade | Aberto | Resolvido nesta rodada |
|---|---:|---:|
| 🔴 CRITICAL | 0 | 3 |
| 🟠 HIGH     | 0 | 5 |
| 🟡 MEDIUM   | 6 | — |
| 🟢 LOW      | 4 | — |
| ⚪ Funcionalidades pendentes | 1 | — |
| **Total aberto** | **11** | **8** |

---

## 🟡 MEDIUM — próxima sprint

### M1 — i18n: strings hardcoded em PT-BR
- **Arquivos:** ~todas as Activities, `RelatorioFinalActivity` (PDF), `SuporteActivity` (FAQ)
- **Estado:** `res/values/strings.xml` só contém `app_name`
- **Impacto:** impossível adicionar inglês/espanhol; manutenção de copy difícil
- **Esforço:** médio (mecânico mas extenso)
- **Ação:** mover toda string visível ao usuário para `strings.xml`; criar `values-en/strings.xml` quando houver tradução

### M2 — Webhook AbacatePay sem retry/timeout/idempotência defensiva
- **Arquivo:** `backend/supabase/functions/webhook-abacatepay/index.ts`
- **Hoje:** valida `idempotency_key` via UNIQUE constraint, mas sem retry exponencial nem timeout no fetch para o gateway
- **Impacto:** pagamentos podem ficar marcados como falhos por falha transitória
- **Ação:**
  - Wrap em `try/catch` com retry exponencial (3 tentativas, 1s/2s/4s)
  - Timeout explícito (30s) no fetch
  - Salvar `error_message` no `webhook_events` para reprocessamento manual
  - Worker periódico (cron) que processa `webhook_events WHERE NOT processed AND created_at < now() - interval '5 min'`

### M3 — Sem rate-limit nas Edge Functions
- **Arquivos:** `subscribe/index.ts`, `cancel-subscription/index.ts`
- **Impacto:** usuário pode disparar múltiplas chamadas em paralelo → race condition em `idx_unique_active_subscription`
- **Ação:**
  - Implementar rate-limit por `user_id` (ex.: 5 req/min) usando tabela `rate_limits` ou Redis
  - Ou usar advisory lock do Postgres: `SELECT pg_try_advisory_xact_lock(hashtext(user_id::text))`

### M4 — `DadosJardulli` Parcelable manual com 43+ campos
- **Arquivo:** `app/.../DadosJardulli.java`
- **Impacto:** propenso a bugs ao adicionar campo (esquecer `writeToParcel` ou ordem do `Creator`); difícil de manter
- **Opções:**
  - **Curta:** anotar com `@Parcelize` (requer plugin Kotlin) → não compatível com Java puro
  - **Média:** quebrar em sub-DTOs por etapa do wizard (`TratorConfig`, `RodadosConfig`, `LastrosConfig`, `PesosConfig`)
  - **Longa:** introduzir `ViewModel` + `SavedStateHandle`, eliminar Parcelable entre Activities (passar só ID e ler do SQLite)
- **Recomendação:** opção média na próxima refatoração, opção longa quando migrar para Single-Activity + Navigation Component

### M5 — `DBHelper` sem `onDowngrade()`
- **Arquivo:** `app/.../DBHelper.java`
- **Impacto:** instalar build com `DATABASE_VERSION` menor (ex.: rollback de release) crasha o app com `SQLiteException`
- **Ação:** sobrescrever `onDowngrade(SQLiteDatabase, int, int)` para fazer drop+recreate ou no-op com log de aviso

### M6 — Dedup do `HistoricoActivity.mesclar()` é frágil
- **Arquivo:** `app/.../HistoricoActivity.java` (método `chaveDedup`)
- **Hoje:** chave = `(data_min[16] + modelo + round(peso))` — se 2 balanços do mesmo modelo no mesmo minuto, descarta o segundo
- **Cenário real:** improvável, mas demos/treinamento podem gerar
- **Ação:** preferir `remoteId` quando disponível; usar `(data_segundos + modelo + peso_total + serie)` como fallback

---

## 🟢 LOW — nice-to-have

### L1 — Acessibilidade: `contentDescription` ausente
- **Padrão em:** `ImageButton` no `MainActivity`, ícones em `RelatorioFinalActivity`
- **Impacto:** TalkBack não anuncia botões → usuários cegos não conseguem usar
- **Ação:** adicionar `android:contentDescription` (ou `tools:ignore="ContentDescription"` se decorativo)

### L2 — Sem detecção de offline antes de chamadas Supabase
- **Hoje:** chamada de rede falha com timeout de 15s
- **Ação:** checar `ConnectivityManager.getActiveNetwork() == null` e mostrar toast antes de chamar; mostrar banner offline em telas que dependem de cloud (Histórico, Planos)

### L3 — Logs não estruturados
- **Hoje:** `Log.d(TAG, "msg " + var)` por toda parte
- **Ação:** adotar formato `Log.d(TAG, "{event=sync_plano, plano=" + plano + ", success=" + ok + "}")` para facilitar grep / Crashlytics

### L4 — Planos legados (`pro`, `premium`) ainda existem
- **Estado:** soft-deleted via `is_active = false` na migration `20260425000000_align_plans_with_app.sql`
- **Risco:** subscriptions antigas ainda apontam para esses `plan_id`
- **Ação (após verificar zero subs ativas):** `DELETE FROM plans WHERE slug IN ('pro', 'premium')` em migration futura

---

## ⚪ Funcionalidades pendentes (não-bugs)

### F1 — Tela de Patinagem / Avanço completa
- **Estado:** `AvancoRatioActivity` existe e calcula, mas falta validação de inputs (`velocidadeReal > 0`, `velocidadeTeorica > 0`) e integração com Histórico
- **Ação:** validar inputs; opcionalmente persistir leituras de patinagem como sub-tipo de balanço

---

## 🔧 Débitos técnicos transversais

### DT1 — `security-crypto:1.1.0-alpha06` em produção
- **Justificativa documentada em** `app/build.gradle.kts`
- **Risco residual:** API alpha pode mudar em release futura; sem GA disponível desde 2021
- **Mitigação:** monitorar [androidx.security release notes](https://developer.android.com/jetpack/androidx/releases/security); ter plano de migração se 1.1.0 GA sair

### DT2 — Cobertura de testes ainda baixa
- **Hoje:** 22 testes (apenas `CalculoUtil` + `DocumentValidator`)
- **Faltam:**
  - Testes unitários para `PlanoManager.podeFazerBalanco()` (limite mensal)
  - Testes para `HistoricoActivity.mesclar()` (dedup)
  - Testes instrumentados (Espresso) para fluxo Auth → Perfil → Balanço
  - Testes de integração das Edge Functions (Deno test runner)

### DT3 — Sem CI/CD pipeline
- **Ação:** GitHub Actions com:
  - `gradlew assembleDebug testDebugUnitTest lintDebug` em cada PR
  - `supabase db lint` + `deno test` para backend
  - Deploy automático de Edge Functions em merge para `main`

### DT4 — `local.properties` deve incluir `WHATSAPP_SUPORTE`
- Adicionar ao `.env.example` ou `local.properties.example` (criar se não existe):
  ```properties
  SUPABASE_URL=https://xxxxx.supabase.co
  SUPABASE_ANON_KEY=eyJ...
  WHATSAPP_SUPORTE=5511999999999
  ```

---

## ✅ Resolvido nesta rodada (referência)

| Issue | Solução |
|---|---|
| C1: RLS UPDATE em `historico_balancos` | `migrations/20260425000200_security_hardening.sql` |
| C2: `security-crypto` alpha | Documentado risco aceito (sem GA disponível) |
| C3: Índice webhook_events | `idx_webhook_events_pending` parcial |
| H1: WhatsApp placeholder | `BuildConfig.WHATSAPP_SUPORTE` via `local.properties` |
| H2: Refresh-token logic | `SupabaseClient.refreshAccessToken()` + `SessionListener` |
| H3: Cache TTL plano (1h) | Já estava em 1h — confirmado |
| H4: Validar CPF/CNPJ no app | `DocumentValidator` extraído + 13 testes |
| H5: Testes do cálculo | `CalculoUtil` + 9 testes; 2ª divisão por zero corrigida |

---

## 📌 Priorização sugerida (próxima sprint)

1. **M2** — retry/timeout no webhook AbacatePay (risco financeiro)
2. **M3** — rate-limit nas Edge Functions de subscription (race condition)
3. **DT4** — atualizar `local.properties.example` (DX)
4. **DT2 (parcial)** — testes para `PlanoManager` (lógica de gates)
5. **M5** — `onDowngrade` no `DBHelper` (rollback seguro de release)
6. **L1** — acessibilidade básica (compliance)
7. **M1** — i18n (apenas se houver demanda)
8. **M4** — refatorar `DadosJardulli` (manutenção de longo prazo)
