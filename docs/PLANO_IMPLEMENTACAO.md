# Plano de Implementação — JardulliCalculadora

## Resumo

Implementar 6 funcionalidades pendentes para alinhar o app Android com os 4 planos de assinatura: **Testdrive**, **Flexível**, **Profissional** e **Frotista**.

Funcionalidades: Patinagem/Avanço, Histórico de Frota, Integração Supabase (auth + assinatura via AbacatePay), Controle de acesso por feature, Limite de balanços e Suporte (FAQ/Email/WhatsApp).

---

## Contexto Atual

| Item | Detalhe |
|------|---------|
| **Plataforma** | Android — Java 11, minSdk 24, targetSdk 35 |
| **Build** | Gradle 8.9.0 (Kotlin DSL) |
| **Banco local** | SQLite via `DBHelper` (SQLiteOpenHelper) |
| **PDF** | iText 7.2.5 |
| **Fluxo** | MainActivity → TratorOperacao → Rodados → LastroLiquido → PesoFrontal → RelatorioFinal |
| **Séries ativas** | JD 8R, NH T7, NH T.8, Case Puma, JD 7J/M, JD 6J/M |
| **Estado atual** | Sem login, sem billing, sem histórico, sem controle de acesso |

## Backend de Assinatura (já implementado)

Projeto: `jardulli-subscription` — Supabase Cloud + AbacatePay

| Componente | Detalhe |
|------------|---------|
| **Auth** | Email/senha via Supabase Auth |
| **Banco** | PostgreSQL 17 com RLS — tabelas: `plans`, `profiles`, `subscriptions`, `payment_history`, `webhook_events` |
| **Edge Functions** | `POST /subscribe`, `POST /cancel-subscription`, `POST /webhooks/abacatepay`, `GET /check-expired` |
| **RPC** | `get_active_subscription()` — retorna plano ativo + features |
| **Pagamento** | AbacatePay (PIX + Cartão) — gera checkout URL |
| **Status machine** | pending → active → past_due → expired / cancelled |

---

## Planos de Assinatura

| Plano | Preço | Balanços/mês | Lastro | Patinagem | PDF | Histórico | Suporte |
|-------|-------|-------------|--------|-----------|-----|-----------|---------|
| **Testdrive** | R$0 | 1 | ✅ | ✅ | ❌ | ❌ | FAQ |
| **Flexível** | R$30/mês | Ilimitado | ✅ | ✅ | ✅ | ✅ | Email |
| **Profissional** | R$25/mês (12m) | Ilimitado | ✅ | ✅ | ✅ | ✅ | WhatsApp |
| **Frotista** | R$20/mês (24m) | Ilimitado | ✅ | ✅ | ✅ | ✅ | Prioritário |

---

## Fases de Implementação

### FASE 1 — Infraestrutura Base ✅ CONCLUÍDA

> Pré-requisito para todas as demais fases.

| Item | Descrição | Status |
|------|-----------|--------|
| 1.1 | `Plano.java` — enum com 4 planos (id, preço, limites, permissões) | ✅ |
| 1.2 | `SupabaseClient.java` — singleton OkHttp: auth, subscription, profiles, edge functions | ✅ |
| 1.3 | `PlanoManager.java` — cache SharedPreferences, contador mensal, sync Supabase, sessão auth | ✅ |
| 1.4 | `DBHelper.java` v2→v3 — tabela `historico_balancos` (16 colunas) + CRUD completo | ✅ |
| 1.5 | `build.gradle.kts` — OkHttp 4.12.0, `buildConfig=true`, Supabase URL/key via BuildConfig | ✅ |
| 1.6 | `AndroidManifest.xml` — permissão INTERNET + remoção de Activities inválidas | ✅ |
| 1.7 | `gradle.properties` — JDK 23 (fix incompatibilidade JDK 25 + AGP 8.9.0) | ✅ |

**Arquivos criados:** `Plano.java`, `SupabaseClient.java`, `PlanoManager.java`
**Arquivos modificados:** `DBHelper.java`, `build.gradle.kts`, `AndroidManifest.xml`, `gradle.properties`, `local.properties`

---

### FASE 2 — Redesign da MainActivity + Navegação

> Depende da Fase 1.

| Item | Descrição |
|------|-----------|
| 2.1 | Redesign `activity_main.xml` — substituir botão único por 5 cards de navegação |
| 2.2 | Cards: "Novo Balanço", "Histórico", "Patinagem e Avanço", "Meu Plano", "Suporte" |
| 2.3 | Badge do plano atual no header |
| 2.4 | `MainActivity.java` — PlanoManager init, gates de acesso, limite de balanços |

**Arquivos a modificar:** `activity_main.xml` (redesign), `MainActivity.java`

---

### FASE 3 — Features de Cálculo Pendentes

> Pode ser feita em paralelo com a Fase 2.

| Item | Descrição |
|------|-----------|
| 3.1 | `AvancoRatioActivity.java` + layout — cálculo de patinagem/avanço |
| 3.2 | Fórmula: `patinagem (%) = ((velTeórica - velReal) / velTeórica) × 100` |
| 3.3 | Tela standalone (fora do fluxo linear de balanço) |
| 3.4 | `RelatorioFinalActivity.java` — salvar balanço no DB + incrementar contador mensal |

**Arquivos a criar:** `AvancoRatioActivity.java`, `activity_avanco_ratio.xml`
**Arquivos a modificar:** `RelatorioFinalActivity.java`

---

### FASE 4 — Histórico + Suporte

> Depende da Fase 1.

| Item | Descrição |
|------|-----------|
| 4.1 | `HistoricoActivity.java` — RecyclerView de balanços salvos (data, série, modelo, peso total) |
| 4.2 | `HistoricoAdapter.java` — adapter para a lista |
| 4.3 | Clique abre detalhe; opção de deletar registros |
| 4.4 | Gate: `PlanoManager.podeVerHistorico()` antes de abrir |
| 4.5 | `SuporteActivity.java` — conteúdo dinâmico por plano |
| 4.6 | Testdrive=FAQ · Flexível=+Email · Profissional=+WhatsApp · Frotista=+Prioritário |

**Arquivos a criar:** `HistoricoActivity.java`, `HistoricoAdapter.java`, `activity_historico.xml`, `item_historico.xml`, `SuporteActivity.java`, `activity_suporte.xml`
**Arquivos a modificar:** `AndroidManifest.xml`

---

### FASE 5 — Integração Supabase (Auth + Assinatura)

> Depende da Fase 1.

| Item | Descrição |
|------|-----------|
| 5.1 | `AuthActivity.java` — login/cadastro (email+senha) + "Continuar sem conta" (Testdrive) |
| 5.2 | `PerfilActivity.java` — nome, CPF/CNPJ, telefone (obrigatório antes de assinar) |
| 5.3 | `PlanosActivity.java` — pricing page com 4 cards |
| 5.4 | Fluxo: clicar "Assinar" → POST /subscribe → checkout_url → navegador → AbacatePay |
| 5.5 | Polling `get_active_subscription()` a cada 3s após checkout |
| 5.6 | `PlanoManager.java` — integrar sync real com Supabase RPC |
| 5.7 | Gates em toda a app: PDF, Histórico, balanços, dialog upsell |
| 5.8 | Deep link intent-filter para completion URL |

**Arquivos a criar:** `AuthActivity.java`, `PerfilActivity.java`, `PlanosActivity.java`, `activity_auth.xml`, `activity_perfil.xml`, `activity_planos.xml`, `item_plano.xml`
**Arquivos a modificar:** `build.gradle.kts`, `PlanoManager.java`, `RelatorioFinalActivity.java`, `MainActivity.java`, `AndroidManifest.xml`

---

## Grafo de Dependências

```
FASE 1 (Infraestrutura) ✅
  ├── FASE 2 (MainActivity redesign)
  ├── FASE 3 (Patinagem + salvar balanço) ← paralelo com Fase 2
  ├── FASE 4 (Histórico + Suporte)
  └── FASE 5 (Auth + Assinatura + Gates)
```

---

## Decisões Técnicas

| Decisão | Justificativa |
|---------|---------------|
| SharedPreferences (não Room) | Alinhado com simplicidade atual do projeto |
| DBHelper (não Room) | Manter padrão existente |
| JSON manual (não Gson/Moshi) | Evita dependência extra |
| OkHttp (não Retrofit) | Client HTTP leve para chamadas Supabase |
| Checkout via `Intent.ACTION_VIEW` | Abre AbacatePay no navegador nativo (não WebView) |
| Testdrive sem auth | Usuário usa o app sem login com funcionalidades limitadas |
| Contador no RelatorioFinal | Incrementa ao CONCLUIR, não ao iniciar |
| Patinagem standalone | Fora do fluxo linear de balanço |

## Escopo Excluído

- Login social (Google, Facebook)
- Tela de onboarding/tutorial
- Notificações push
- Analytics/tracking
- Supabase Realtime (usa polling)

## Ações Pendentes no Backend

- Atualizar tabela `plans` no Supabase: substituir Pro/Premium pelos 4 planos (Testdrive, Flexível, Profissional, Frotista) com preços e features corretos

## Verificação

| Fase | Critério |
|------|----------|
| 1 | `./gradlew assembleDebug` compila ✅ |
| 2 | Navegação por todos os cards funciona; gates bloqueiam corretamente |
| 3 | Patinagem calcula valores corretos; balanço é salvo no banco |
| 4 | Balanços aparecem no Histórico; links de suporte funcionam |
| 5 | Signup/login OK; perfil obrigatório; checkout → plano ativo; gates atualizam; offline funciona |
