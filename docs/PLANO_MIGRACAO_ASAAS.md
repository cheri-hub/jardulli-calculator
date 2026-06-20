# Plano de Migração: AbacatePay → Asaas (Assinaturas)

> **Objetivo:** Substituir o gateway **AbacatePay** por **Asaas**, focando em **assinaturas
> recorrentes**, mantendo o **backend o mais fino possível** e delegando ao Asaas o máximo
> de responsabilidade (captura/armazenamento de cartão, recorrência, cobrança, notificações).

> ### Decisões confirmadas (14/jun/2026)
> - **Métodos de pagamento:** apenas **CREDIT_CARD** (renovação automática total pelo Asaas).
> - **Sem assinantes legados:** ambiente novo → **troca limpa**, sem caminho de convivência
>   `legacy_v1`. Código AbacatePay é removido direto; colunas renomeadas.
>
> ### Progresso
> - ✅ **Fase 1** — camada compartilhada Asaas: `_shared/asaas-types.ts`, `_shared/asaas-client.ts`, `_shared/asaas-webhook-validator.ts`.
> - ✅ **Fase 2** — migration `20260614120000_migrate_gateway_to_asaas.sql` (rename `abacate_*`→`asaas_*`, drop `legacy_v1`/`abacate_product_id`, `billing_cycle`→`asaas_cycle` com enum Asaas, `ANNUALLY`→`YEARLY`, add `plans.image_base64`).
> - ✅ **Fase 3** — rewire das edge functions (`subscribe` usa `POST /v3/checkouts`; `cancel-subscription` usa `DELETE /v3/subscriptions/{id}`; novo `webhook-asaas`; `check-expired` mantido como rede de segurança). Removidos `abacatepay-client.ts`, `webhook-validator.ts`, `types.ts`, `webhook-abacatepay/`. `config.toml`: `[functions.webhook-asaas] verify_jwt=false`. `errors.ts` repontado para `asaas-types`.
>   - **Extra (fora do inventário original):** painel web `backend/apps/web` também ajustado (`dashboard.ts`, `navigation.ts`+test, `types.ts`) — consumia as colunas renomeadas e validava host `abacatepay.com` → `asaas.com`.
>   - `backend/.env.example` e comentário em `PlanosActivity` atualizados. Workflows não precisaram de mudança (deploy itera as pastas dinamicamente).
> - ⏭ **Pendente (operacional, requer você):** Fase 4 (secrets `ASAAS_*` no Supabase + cadastro do webhook no painel) e Fase 5 (smoke test sandbox). Falta também: definir `image_base64`/preços por plano e `deno test`/CI verde.

---

## 1. Resumo executivo

### Decisão-chave

Usar o **Checkout hospedado do Asaas** (`POST /v3/checkouts`) com:

```json
{
  "billingTypes": ["CREDIT_CARD"],
  "chargeTypes":  ["RECURRENT"],
  "subscription": { "cycle": "MONTHLY", "nextDueDate": "2026-07-15" },
  "items":        [{ "name": "...", "quantity": 1, "value": 30.00, "imageBase64": "..." }],
  "callback":     { "successUrl": "...", "cancelUrl": "...", "expiredUrl": "..." },
  "externalReference": "<id da nossa subscription>",
  "customerData": { "name": "...", "cpfCnpj": "...", "email": "...", "phone": "..." }
}
```

A resposta traz `link` (URL hospedada) e `id` (checkout id). É o **equivalente direto** do
`POST /v2/subscriptions/create` do AbacatePay, que hoje retorna `checkout_url`. O cliente entra
na página, cadastra o cartão **no Asaas** (nunca no nosso backend), e o Asaas passa a cobrar
automaticamente a cada ciclo.

**Consequência mais importante:** o **app Android não precisa de nenhuma alteração** se mantivermos
o contrato das Edge Functions (`subscribe` devolve `checkout_url`, `cancel-subscription` recebe
`subscription_id`, RPC `get_active_subscription` inalterada). Toda a migração é **backend-only**.

### Por que não usar `POST /v3/subscriptions` direto

`POST /v3/subscriptions` com `billingType: CREDIT_CARD` exige enviar **dados do cartão** (ou token)
pelo nosso backend → carrega ônus PCI e contradiz o objetivo de backend mínimo. O `POST /v3/checkouts`
resolve isso: o Asaas hospeda a captura do cartão e cria a assinatura por baixo dos panos.

---

## 2. Como o AbacatePay está implantado hoje

> ⚠️ No código o gateway se chama **AbacatePay** (não "AbatePay"). Os arquivos usam o prefixo `abacate`.

### Fluxo atual (assinatura)

```
App (PlanosActivity)
  └─ POST /functions/v1/subscribe        {plan_id: "flexivel"}
       └─ Edge Function subscribe
            ├─ valida perfil (nome, CPF/CNPJ Mod11, telefone)
            ├─ garante customer AbacatePay (POST /v1/customer/create)  → profiles.abacate_customer_id
            ├─ insere subscriptions (status=pending)
            └─ POST /v2/subscriptions/create (items=[abacate_product_id]) → checkout_url (bill_xxx)
       ◀─ {checkout_url}
  └─ abre checkout_url no navegador  +  começa polling de get_active_subscription
  ...
AbacatePay → POST /functions/v1/webhook-abacatepay  (subscription.completed / renewed / cancelled)
       └─ atualiza subscriptions.status, current_period_*, abacate_subscription_id (subs_xxx)
       └─ insere payment_history
App (polling) detecta plano ativo via RPC get_active_subscription
```

Cancelamento: `cancel-subscription` → `POST /v2/subscriptions/cancel` (subs_xxx) → webhook finaliza.
`check-expired` (cron) faz a varredura de transições de status por data.

### Inventário de arquivos acoplados ao AbacatePay

| Arquivo | Papel | Ação na migração |
|---|---|---|
| `backend/supabase/functions/_shared/abacatepay-client.ts` | Cliente HTTP v1/v2 (createCustomer, createSubscriptionCheckout, cancelSubscription) | **Reescrever** → `asaas-client.ts` |
| `backend/supabase/functions/_shared/webhook-validator.ts` | Valida secret + HMAC AbacatePay | **Reescrever** (Asaas usa header `asaas-access-token`, sem HMAC) |
| `backend/supabase/functions/_shared/types.ts` | Tipos Abacate v1/v2 + DB | **Reescrever** seção do gateway; manter tipos de DB |
| `backend/supabase/functions/subscribe/index.ts` | Cria checkout | **Editar** (chamar `POST /v3/checkouts`) |
| `backend/supabase/functions/cancel-subscription/index.ts` | Cancela assinatura | **Editar** (`DELETE /v3/subscriptions/{id}`) |
| `backend/supabase/functions/webhook-abacatepay/index.ts` | Recebe eventos | **Substituir** → `webhook-asaas/index.ts` (eventos `PAYMENT_*`/`SUBSCRIPTION_*`) |
| `backend/supabase/functions/check-expired/index.ts` | Cron de expiração | **Simplificar** (Asaas dispara `PAYMENT_OVERDUE`; manter só rede de segurança) |
| `backend/supabase/migrations/*subscriptions_v2_columns*` etc. | Colunas `abacate_*` | **Nova migration** renomeando para `asaas_*` |
| `backend/supabase/migrations/*plans_abacate_product_id*` | `plans.abacate_product_id`, `billing_cycle` | **Nova migration**: dropar `abacate_product_id`, normalizar ciclo p/ enum Asaas |
| `backend/supabase/config.toml` | `[functions.webhook-abacatepay] verify_jwt=false` | **Editar** → `[functions.webhook-asaas]` |
| `docs/OPS_WEBHOOK_ABACATEPAY.md`, `OPS_ABACATE_PRODUCTS_SETUP.md` | Runbooks | **Substituir** por runbooks Asaas |
| `.github/workflows/backend*.yml` | Secrets de deploy | **Editar** secrets `ABACATEPAY_*` → `ASAAS_*` |

**App Android** (`PlanosActivity`, `SupabaseClient`, `PlanoManager`, `Plano.java`): **sem mudanças de código**.
Único cuidado: o comentário em `PlanosActivity.assinarPlano()` cita "AbacatePay" — atualizar para "Asaas" (cosmético).

---

## 3. Mapeamento de conceitos AbacatePay → Asaas

| Conceito | AbacatePay (atual) | Asaas (alvo) |
|---|---|---|
| Base URL | `api.abacatepay.com/v1` e `/v2` | `api.asaas.com/v3` (prod) / `api-sandbox.asaas.com/v3` |
| Auth API | Header `Authorization: Bearer <key>` | Header `access_token: <key>` (+ `User-Agent` obrigatório) |
| Cliente | `POST /v1/customer/create` → `cust_xxx` | Criado **inline** no checkout (`customerData`) → `cus_xxx` (opcional pré-criar via `POST /v3/customers`) |
| Checkout de assinatura | `POST /v2/subscriptions/create` → `{id: bill_xxx, url}` | `POST /v3/checkouts` → `{id, link}` |
| ID da assinatura | `subs_xxx` (via webhook) | `sub_xxx` (via webhook `PAYMENT_*`/`SUBSCRIPTION_CREATED`) |
| Cancelar | `POST /v2/subscriptions/cancel {id: subs_xxx}` | `DELETE /v3/subscriptions/{sub_xxx}` |
| Ciclos | `WEEKLY/MONTHLY/SEMIANNUALLY/ANNUALLY` | `WEEKLY/BIWEEKLY/MONTHLY/BIMONTHLY/QUARTERLY/SEMIANNUALLY/YEARLY` (`ANNUALLY`→`YEARLY`) |
| Valor | centavos (int) | reais (number, ex. `30.00`) |
| Ativação | webhook `subscription.completed` | webhook `PAYMENT_CONFIRMED` / `PAYMENT_RECEIVED` |
| Renovação | webhook `subscription.renewed` | novo `PAYMENT_CONFIRMED`/`PAYMENT_RECEIVED` a cada ciclo |
| Cancelamento (evento) | `subscription.cancelled` | `SUBSCRIPTION_DELETED` / `SUBSCRIPTION_INACTIVATED` |
| Disputa/estorno | `billing.disputed` | `PAYMENT_REFUNDED` / `PAYMENT_CHARGEBACK_REQUESTED` |
| Auth do webhook | `?webhookSecret=` ou `Authorization: Bearer` + HMAC `X-Webhook-Signature` | Header `asaas-access-token` == token configurado (sem HMAC) |
| Idempotência | chave composta `event:id:updatedAt` | usar `event.id` (`evt_xxx`) |

---

## 4. Arquitetura alvo

```
App (PlanosActivity)  ── sem alteração ──
  └─ POST /functions/v1/subscribe        {plan_id: "flexivel"}
       └─ Edge Function subscribe (FINA)
            ├─ valida perfil (já existe)
            ├─ insere subscriptions (status=pending, externalReference=id)
            └─ POST /v3/checkouts (RECURRENT, CREDIT_CARD, cycle, value, customerData inline)
       ◀─ {checkout_url: link}
  └─ abre link  +  polling get_active_subscription   (sem alteração)
  ...
Asaas → POST /functions/v1/webhook-asaas
       ├─ PAYMENT_CONFIRMED/RECEIVED → status=active, grava asaas_subscription_id, current_period_end=dueDate próximo, payment_history
       ├─ PAYMENT_OVERDUE            → status=past_due
       ├─ SUBSCRIPTION_DELETED       → status=cancelled (respeitando período pago)
       └─ PAYMENT_REFUNDED/CHARGEBACK→ status=cancelled
App (polling) detecta plano via RPC get_active_subscription  (sem alteração)
```

**Divisão de responsabilidade (o que fica no Asaas vs no nosso backend):**

| Responsabilidade | Onde fica |
|---|---|
| Captura e armazenamento de cartão (PCI) | **Asaas** |
| Recorrência / geração de cobranças por ciclo | **Asaas** |
| Notificações de cobrança/vencimento ao cliente | **Asaas** |
| Retentativas de cobrança | **Asaas** |
| Mapear evento → `status` do plano (active/past_due/cancelled) | Backend (mínimo) |
| Espelhar `current_period_end` p/ janela de acesso do app | Backend (do payload do webhook) |
| Gate de feature por plano (PDF, histórico, limites) | Backend (RPC + `plan_features`, já existe) |

---

## 5. Mudanças detalhadas por camada

### 5.1 App Android — **nenhuma mudança funcional**

- `subscribe` continua retornando `{checkout_url}` → `PlanosActivity` abre no navegador. ✔
- `cancel-subscription` continua recebendo `{subscription_id}` (o **nosso** UUID, não o do gateway). ✔
- `get_active_subscription` (plan_slug/features/status/period) inalterada → `PlanoManager` ok. ✔
- (Cosmético) atualizar o comentário "AbacatePay" em `PlanosActivity.assinarPlano()`.

### 5.2 `_shared/asaas-client.ts` (novo, substitui `abacatepay-client.ts`)

Funções:
- `createCheckout(payload)` → `POST /v3/checkouts` → `{ id, link }`.
- `cancelSubscription(asaasSubscriptionId)` → `DELETE /v3/subscriptions/{id}` (idempotente em 404).
- (opcional) `createCustomer(payload)` → `POST /v3/customers` se quisermos pré-criar; **não é necessário** com `customerData` inline.

Detalhes:
- Header `access_token: <ASAAS_API_KEY>` + `User-Agent: JardulliCalculadora` + `Content-Type: application/json`.
- Base URL de env `ASAAS_BASE_URL` (sandbox/prod). Reaproveitar `http-retry.ts`.
- Erros do Asaas vêm em `{ errors: [{ code, description }] }` → adaptar parsing.

### 5.3 `subscribe/index.ts` (editar)

Mudanças mínimas sobre o que já existe:
1. Remover validação `abacate_product_id` (não há "produto" no Asaas; o checkout recebe item inline).
2. Remover o passo "ensure customer" (passar `customerData` inline). *Opcional* manter pré-criação se preferir 1 `cus_xxx` por usuário.
3. Manter: auth, rate-limit, validação de perfil/CPF-CNPJ, insert `subscriptions(pending)`.
4. Trocar a chamada do gateway por `createCheckout`:
   - `items[0]`: `{ name: plan.name, quantity: 1, value: plan.price_cents/100, imageBase64: <logo> }`
   - `subscription`: `{ cycle: cicloAsaas(plan), nextDueDate: hoje }`
   - `chargeTypes: ["RECURRENT"]`, `billingTypes: ["CREDIT_CARD"]`
   - `externalReference: subscription.id`
   - `callback`: `{ successUrl: APP_COMPLETION_URL, cancelUrl: APP_RETURN_URL, expiredUrl: APP_RETURN_URL }`
5. Gravar `asaas_checkout_id = res.id` na subscription; retornar `{ checkout_url: res.link }`.

> ⚠️ `items[].imageBase64` é **obrigatório** no checkout. Definir uma logo pequena em base64
> (constante no backend ou coluna em `plans`). É a principal "pegadinha" do payload.

### 5.4 `cancel-subscription/index.ts` (editar)

- Onde hoje chama `cancelSubscription(abacate_subscription_id)` v2, passar a chamar `DELETE /v3/subscriptions/{asaas_subscription_id}`.
- Manter a lógica de "pending/legacy cancela local; ativo mantém até `current_period_end` e espera o webhook".
- `SUBSCRIPTION_DELETED` finaliza o status.

### 5.5 `webhook-asaas/index.ts` (novo, substitui `webhook-abacatepay`)

- **Auth:** comparar header `asaas-access-token` com env `ASAAS_WEBHOOK_TOKEN` (timing-safe). Sem HMAC.
- **Idempotência:** `idempotency_key = event.id` (`evt_xxx`) na tabela `webhook_events`.
- **Roteamento de eventos → status** (ver seção 6).
- Para `PAYMENT_*`: localizar a subscription por `payment.subscription` (sub_xxx) **ou** por `payment.externalReference` / `checkout externalReference` (= nosso `subscriptions.id`). Gravar `asaas_subscription_id` na primeira confirmação.
- `current_period_end`: usar o `nextDueDate` da assinatura (presente no payload de assinatura) ou somar `interval_days` como fallback.
- Sempre responder **200** (senão o Asaas pausa a fila após 15 falhas).

### 5.6 `check-expired/index.ts` (simplificar)

- Com o Asaas disparando `PAYMENT_OVERDUE`/`PAYMENT_RECEIVED`, a maior parte das transições vem por webhook.
- Manter como **rede de segurança**: varrer `active`/`past_due` com `current_period_end` vencido (caso um webhook seja perdido) e finalizar canceladas após o período pago.

### 5.7 Migrations

**Migration A — renomear colunas do gateway:**
```sql
ALTER TABLE profiles      RENAME COLUMN abacate_customer_id     TO asaas_customer_id;
ALTER TABLE subscriptions RENAME COLUMN abacate_billing_id      TO asaas_checkout_id;
ALTER TABLE subscriptions RENAME COLUMN abacate_subscription_id TO asaas_subscription_id;
ALTER TABLE payment_history RENAME COLUMN abacate_billing_id    TO asaas_payment_id;
-- payment_method enum: Asaas usa CREDIT_CARD/PIX/BOLETO. Avaliar adicionar 'boleto' se habilitar.
```

**Migration B — planos:**
```sql
ALTER TABLE plans DROP COLUMN IF EXISTS abacate_product_id;
-- billing_cycle: normalizar valores para o enum Asaas (ANNUALLY → YEARLY, etc.)
-- (opcional) adicionar plans.image_base64 para o item do checkout.
```

- RPC `get_active_subscription`: **não muda** (não retorna colunas do gateway).
- Reavaliar o índice único parcial `idx_unique_active_subscription` (segue válido).

### 5.8 Config & Secrets

`config.toml`:
```toml
[functions.webhook-asaas]
verify_jwt = false      # Asaas não envia JWT do Supabase
```
(remover o bloco `[functions.webhook-abacatepay]`)

Secrets (`supabase secrets set ...` e GitHub Actions):
| Antigo | Novo |
|---|---|
| `ABACATEPAY_API_KEY` | `ASAAS_API_KEY` |
| `ABACATEPAY_WEBHOOK_SECRET` | `ASAAS_WEBHOOK_TOKEN` |
| — | `ASAAS_BASE_URL` (`https://api-sandbox.asaas.com/v3` → prod depois) |
| `APP_COMPLETION_URL` / `APP_RETURN_URL` | mantidos (viram `successUrl`/`cancelUrl`) |

---

## 6. Mapeamento de eventos de webhook → status

| Evento Asaas | Ação no nosso backend |
|---|---|
| `PAYMENT_CONFIRMED` (cartão autorizado) | `status=active`; grava `asaas_subscription_id`; `current_period_end = nextDueDate`; insere `payment_history` |
| `PAYMENT_RECEIVED` (liquidado) | idem (idempotente); confirma recebimento |
| `PAYMENT_OVERDUE` | `status=past_due` |
| `PAYMENT_REFUNDED` | `status=cancelled` |
| `PAYMENT_CHARGEBACK_REQUESTED` / `_DISPUTE` | `status=cancelled` (revisar manualmente) |
| `SUBSCRIPTION_CREATED` | (opcional) registrar `asaas_subscription_id` cedo |
| `SUBSCRIPTION_DELETED` / `SUBSCRIPTION_INACTIVATED` | `status=cancelled` respeitando `current_period_end` |

> **Importante:** a **ativação** vem dos eventos `PAYMENT_*`, não dos `SUBSCRIPTION_*`
> (estes não carregam confirmação de pagamento). O objeto `payment` traz `subscription` (sub_xxx)
> e `externalReference` para casar com a nossa subscription.

---

## 7. Pontos de atenção (gotchas)

1. **`imageBase64` obrigatório** em `items[]` do checkout. Defina uma logo pequena.
2. **Valor em reais** (`30.00`), não centavos — converter `price_cents/100`.
3. **Ciclo**: mapear enum AbacatePay → Asaas (`ANNUALLY` → `YEARLY`).
4. **Header `User-Agent` obrigatório** nas chamadas à API Asaas (senão pode ser rejeitado).
5. **Webhook deve responder 200** sempre; após **15 falhas** consecutivas o Asaas **pausa a fila**. Eventos ficam 14 dias para reprocessar.
6. **Tolerância a campos novos**: o Asaas adverte que pode incluir novos atributos no payload; não quebrar em campos desconhecidos.
7. **`access_token` no webhook**: configure um token dedicado (não a API key de produção) no painel/`POST /v3/webhook`, e valide no handler.
8. **PIX recorrente não é automático** (sem Pix Automático). Para auto-renovação, manter **CREDIT_CARD** (espelha o `methods:["CARD"]` atual). PIX pode ser oferecido como pagamento avulso no futuro.
9. **`externalReference`**: sempre enviar o nosso `subscriptions.id` para reconciliação no webhook.
10. **Sufixo da chave** indica ambiente (`$aact_hmlg_` sandbox / `$aact_prod_` produção) — bom para um guard no startup.

---

## 8. Plano de execução faseado

**Fase 0 — Preparação**
- [ ] Criar conta sandbox Asaas; obter `ASAAS_API_KEY` (`$aact_hmlg_`).
- [ ] Definir `ASAAS_WEBHOOK_TOKEN`; logo base64 do item de checkout.

**Fase 1 — Backend (shared)** ✅
- [x] `asaas-client.ts` (createCheckout, cancelSubscription).
- [x] `asaas-webhook-validator.ts` → validação por `asaas-access-token`.
- [x] `asaas-types.ts` → tipos Asaas (Checkout, Subscription, Payment, Webhook) + rows `asaas_*`.

**Fase 2 — Migrations**
- [ ] Migration A (renomear colunas `abacate_*` → `asaas_*`).
- [ ] Migration B (planos: dropar product_id, normalizar ciclo, image_base64).
- [ ] `supabase db push` no sandbox.

**Fase 3 — Edge Functions**
- [ ] Editar `subscribe` (checkout RECURRENT).
- [ ] Editar `cancel-subscription` (DELETE subscription).
- [ ] Criar `webhook-asaas`; remover `webhook-abacatepay`.
- [ ] Simplificar `check-expired`.
- [ ] `config.toml`: `verify_jwt=false` no `webhook-asaas`.

**Fase 4 — Config/CI**
- [ ] `supabase secrets set ASAAS_*` no projeto sandbox.
- [ ] Atualizar workflows GitHub Actions.
- [ ] Cadastrar webhook no painel Asaas (eventos `PAYMENT_*` + `SUBSCRIPTION_*`).

**Fase 5 — Testes (sandbox)** — ver seção 9.

**Fase 6 — Produção**
- [ ] Trocar `ASAAS_BASE_URL` + `ASAAS_API_KEY` (`$aact_prod_`).
- [ ] Re-cadastrar webhook em produção.
- [ ] Smoke test com valor baixo + cancelamento.

**Fase 7 — Limpeza**
- [ ] Remover `abacatepay-client.ts`, `webhook-abacatepay/`, docs/secrets antigos.
- [ ] Atualizar `CLAUDE.md` (substituir AbacatePay por Asaas na descrição da arquitetura).

---

## 9. Plano de testes (sandbox)

1. Criar plano em `plans` com `billing_cycle=MONTHLY` e preço.
2. Login no app → completar perfil (nome, CPF/CNPJ válido, telefone).
3. Assinar → backend retorna `checkout_url` (link sandbox) → abrir.
4. Pagar com cartão de teste `4111 1111 1111 1111` (CVV qualquer, validade futura).
5. Confirmar pagamento (hosted) → webhook `PAYMENT_CONFIRMED` chega → subscription `active`.
6. App (polling) reflete o plano via `get_active_subscription`.
7. Validar `payment_history` e `asaas_subscription_id` gravados.
8. Cancelar pelo app → `DELETE /v3/subscriptions/{id}` → `SUBSCRIPTION_DELETED` → acesso até `current_period_end`.
9. Testes de borda: webhook duplicado (idempotência), token de webhook errado (401), `PAYMENT_OVERDUE` → `past_due`.

> Endpoints sandbox para simular liquidação: `POST /v3/sandbox/payment/{id}/confirm` / `/receive` / `/overdue` / `/refund` (ver `docs/ASAAS_API.md`).

---

## 10. Resumo de arquivos

**Criar:**
- `backend/supabase/functions/_shared/asaas-client.ts`
- `backend/supabase/functions/webhook-asaas/index.ts`
- `backend/supabase/migrations/<ts>_rename_gateway_columns_to_asaas.sql`
- `backend/supabase/migrations/<ts>_plans_asaas_adjustments.sql`
- `docs/OPS_WEBHOOK_ASAAS.md` (runbook)

**Editar:**
- `subscribe/index.ts`, `cancel-subscription/index.ts`, `check-expired/index.ts`
- `_shared/webhook-validator.ts`, `_shared/types.ts`
- `config.toml`, `.github/workflows/backend*.yml`, `CLAUDE.md`
- `PlanosActivity.java` (apenas comentário)

**Remover (Fase 7):**
- `_shared/abacatepay-client.ts`, `webhook-abacatepay/`
- `docs/OPS_WEBHOOK_ABACATEPAY.md`, `docs/OPS_ABACATE_PRODUCTS_SETUP.md`
- Secrets `ABACATEPAY_*`

---

## 11. Referências

- Guia da API: `docs/ASAAS_API.md`
- Checkout com assinatura recorrente: <https://docs.asaas.com/docs/checkout-com-assinatura-recorrente>
- `POST /v3/checkouts`: cria checkout hospedado (resposta `{id, link}`)
- Eventos de assinatura: <https://docs.asaas.com/docs/eventos-para-assinaturas>
- Eventos de cobrança (ativação): `PAYMENT_CONFIRMED`, `PAYMENT_RECEIVED`, `PAYMENT_OVERDUE`, `PAYMENT_REFUNDED`
