# Próximos Passos — Finalizar os testes dos últimos ajustes (Asaas)

> Escopo: **fechar a validação** da migração AbacatePay → Asaas. O código das Fases 1–3
> já está implementado localmente (ver [`PLANO_MIGRACAO_ASAAS.md`](PLANO_MIGRACAO_ASAAS.md)).
> Este documento cobre **só o que falta testar** para considerar a migração concluída.
>
> Gerado em **18/jun/2026**.

---

## 0. Estado atual dos testes

| Camada | Onde | Hoje | Falta |
|---|---|---|---|
| Unit Android | `app/src/test/...` | ✅ ~22 testes (`CalculoUtilTest`, `DocumentValidatorTest`) verdes | Confirmar `gradlew test` verde após mudanças |
| Painel web (vitest) | `backend/apps/web/lib/*.test.ts` | ✅ 4 suites (`brazilian-documents`, `current-subscription`, `navigation`, `status`) | Rodar após rename `abacate_*`→`asaas_*` |
| Edge Functions (Deno) | `backend/supabase/functions/*` | ⚠️ `deno check`/`fmt`/`lint` na CI, **0 testes** | **Criar testes** p/ `subscribe`, `cancel-subscription`, `webhook-asaas` |
| E2E sandbox | manual | ⏸ Pendente (Fases 4–5) | Secrets + webhook no painel + smoke test cartão |
| E2E doc | [`OPS_E2E_TEST.md`](OPS_E2E_TEST.md) | ❌ Desatualizado (AbacatePay/PIX) | Reescrever p/ Asaas/cartão |

**Definição de pronto:** os 5 itens da coluna "Falta" fechados + CI verde (`android.yml` e `backend.yml`).

---

## 1. Camada local — rodar o que já existe (rápido, sem device)

Faça isto **primeiro**: garante que os renames da migração não quebraram nada compilável.

```powershell
# 1a. Unit tests Android (JVM, sem device)
cd c:\repo\JardulliCalculadora
.\gradlew.bat test
# relatório: app/build/reports/tests/testDebugUnitTest/index.html

# 1b. Lint Android
.\gradlew.bat lintDebug

# 1c. Painel web (vitest) — valida colunas renomeadas asaas_*
cd backend\apps\web
npm test
npm run lint

# 1d. Edge Functions — espelha a CI backend.yml
cd ..\..
deno fmt --check supabase/functions
deno lint supabase/functions
foreach ($fn in Get-ChildItem supabase/functions/*/index.ts) { deno check $fn.FullName }
```

- [ ] `gradlew test` verde
- [ ] `gradlew lintDebug` sem erros novos
- [ ] `npm test` (web) verde — confirma que `dashboard.ts`/`current-subscription` leem `asaas_*`
- [ ] `deno fmt/lint/check` verdes nas 4 functions

> Se algo falhar aqui, é regressão direta da migração — corrigir antes de seguir.

---

## 2. Testes Deno das Edge Functions (lacuna principal)

Hoje a CI **pula** `deno test` porque não existe nenhum (`backend.yml` linhas 53–59).
Esta é a maior lacuna de cobertura dos "últimos ajustes". Criar testes unitários
focados na **lógica pura** (sem rede), que é exatamente o que mudou na migração:

### 2.1 `_shared/asaas-webhook-validator.test.ts`
- token correto (`asaas-access-token` == `ASAAS_WEBHOOK_TOKEN`) → válido
- token errado / ausente → inválido
- comparação timing-safe não vaza por tamanho

### 2.2 `_shared/asaas-client.test.ts`
- monta header `access_token` + `User-Agent` obrigatório
- converte **reais** corretamente no item do checkout (`price_cents/100` → `30.00`)
- mapeia ciclo `ANNUALLY` → `YEARLY` (e demais)
- parsing de erro Asaas `{ errors: [{ code, description }] }`
- `cancelSubscription` idempotente em 404
- (usar `fetch` stub/mock — não bater na API real)

### 2.3 `webhook-asaas/index.test.ts` — roteamento evento → status (o coração da migração)

| Evento | status esperado |
|---|---|
| `PAYMENT_CONFIRMED` / `PAYMENT_RECEIVED` | `active` + grava `asaas_subscription_id` + `payment_history` |
| `PAYMENT_OVERDUE` | `past_due` |
| `PAYMENT_REFUNDED` / `PAYMENT_CHARGEBACK_REQUESTED` | `cancelled` |
| `SUBSCRIPTION_DELETED` / `SUBSCRIPTION_INACTIVATED` | `cancelled` respeitando `current_period_end` |
| evento duplicado (mesmo `evt_xxx`) | idempotente (não reprocessa) |
| token inválido | resposta de rejeição |
| campo desconhecido no payload | **não quebra** (tolerância a campos novos) |

> Extrair a função de roteamento (`evento → ação`) para um módulo puro testável, se
> ainda estiver inline no handler. Assim os testes não precisam de Supabase real.

Rodar:
```powershell
cd c:\repo\JardulliCalculadora\backend
deno test --allow-all supabase/functions
```

- [ ] Testes 2.1, 2.2, 2.3 criados e verdes localmente
- [ ] `backend.yml` passa a executar `deno test` (já detecta `*.test.ts` automaticamente)

---

## 3. Preparar o ambiente sandbox (Fase 4 — requer você)

Sem isto o E2E não roda. Ver [`OPS_WEBHOOK_ASAAS.md`](OPS_WEBHOOK_ASAAS.md) para o detalhe.

- [ ] Conta sandbox Asaas criada; `ASAAS_API_KEY` com prefixo `$aact_hmlg_`
- [ ] Definir `ASAAS_WEBHOOK_TOKEN` (token dedicado, **não** a API key)
- [ ] Definir `image_base64` (logo pequena) e preços/ciclo por plano em `plans`
- [ ] Secrets no Supabase:
  ```powershell
  cd c:\repo\JardulliCalculadora\backend
  supabase secrets set ASAAS_API_KEY='$aact_hmlg_...' `
    ASAAS_WEBHOOK_TOKEN='...' `
    ASAAS_BASE_URL='https://api-sandbox.asaas.com/v3'
  ```
- [ ] Migrations aplicadas no sandbox: `supabase db push`
- [ ] Functions deployadas: `supabase functions deploy subscribe cancel-subscription webhook-asaas check-expired`
- [ ] Webhook cadastrado no painel Asaas apontando p/ `/functions/v1/webhook-asaas`, eventos `PAYMENT_*` + `SUBSCRIPTION_*`
- [ ] Guard de ambiente conferido (chave `hmlg` ≠ `prod`)

---

## 4. Smoke test E2E em sandbox (Fase 5)

Roteiro resumido (detalhe em `PLANO_MIGRACAO_ASAAS.md` §9). Cartão de teste Asaas: `4111 1111 1111 1111`, CVV qualquer, validade futura.

1. [ ] Login no app → completar perfil (nome, CPF/CNPJ válido Mod 11, telefone)
2. [ ] Assinar plano → `subscribe` retorna `checkout_url` (link sandbox) → abre no navegador
3. [ ] Pagar com cartão de teste → webhook `PAYMENT_CONFIRMED` chega
4. [ ] `subscriptions.status = active`, `asaas_subscription_id` (sub_xxx) gravado, `current_period_end = nextDueDate`
5. [ ] `payment_history` recebe 1 linha
6. [ ] App (polling `get_active_subscription`) reflete o plano ativo
7. [ ] Cancelar pelo app → `DELETE /v3/subscriptions/{id}` → `SUBSCRIPTION_DELETED` → acesso até `current_period_end`
8. [ ] **Bordas:** webhook duplicado (idempotência), token errado (rejeição), simular `PAYMENT_OVERDUE` → `past_due`

> Simular liquidação no sandbox: `POST /v3/sandbox/payment/{id}/confirm` / `/receive` / `/overdue` / `/refund` (ver `docs/ASAAS_API.md`).

Validações SQL:
```sql
select id, status, asaas_subscription_id, asaas_checkout_id,
       current_period_start, current_period_end
  from subscriptions where user_id = '<uuid>' order by created_at desc limit 1;
select * from payment_history where subscription_id = '<sub_id>';
select processed, error_message from webhook_events order by created_at desc limit 5;
```

---

## 5. Atualizar o runbook de E2E manual

`OPS_E2E_TEST.md` ainda descreve **AbacatePay + PIX** (`abacate_billing_id`, `billing.paid`,
`webhook-abacatepay`, pagamento PIX por centavos). Precisa virar **Asaas + cartão**:

- [ ] Trocar passos 3–5 (checkout/pagamento) para o fluxo de cartão Asaas
- [ ] Trocar nomes de coluna nas queries (`abacate_*` → `asaas_*`)
- [ ] Trocar `webhook-abacatepay` → `webhook-asaas` nos comandos de log
- [ ] Trocar evento `billing.paid` → `PAYMENT_CONFIRMED`/`PAYMENT_RECEIVED`
- [ ] Ajustar checklist final (seção 11)

---

## 6. Portão final (Definition of Done)

- [ ] **§1** todos os testes locais verdes (Android + web + deno check/lint/fmt)
- [ ] **§2** testes Deno das functions criados e a CI `backend.yml` rodando `deno test` verde
- [ ] **§3** sandbox provisionado (secrets, migrations, functions, webhook)
- [ ] **§4** smoke test E2E sandbox passou ponta a ponta (incluindo bordas)
- [ ] **§5** `OPS_E2E_TEST.md` reescrito para Asaas
- [ ] CI verde nos dois workflows (`android.yml`, `backend.yml`)
- [ ] Só então liberar a **Fase 6** (produção) do plano de migração

---

## Ordem sugerida de execução

```
§1 (local, minutos)  →  §2 (escrever testes Deno)  →  §3 (provisionar sandbox)
   →  §4 (smoke E2E)  →  §5 (atualizar runbook)  →  §6 (portão) →  Fase 6 produção
```

§1 e §2 não dependem de você (rodam local/CI). §3 e §4 exigem acesso ao painel
Asaas e a um device Android. §5 pode ser feito em paralelo a partir de §4.
