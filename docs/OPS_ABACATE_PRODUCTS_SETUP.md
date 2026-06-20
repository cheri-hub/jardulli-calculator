> ⛔ **OBSOLETO (jun/2026):** o gateway foi migrado para **Asaas**. O Asaas não usa "produtos"
> pré-cadastrados — o item vai inline no checkout. Não execute este runbook.
> Ver [`PLANO_MIGRACAO_ASAAS.md`](PLANO_MIGRACAO_ASAAS.md) e [`OPS_WEBHOOK_ASAAS.md`](OPS_WEBHOOK_ASAAS.md).
> _(Será removido na Fase 7, após validação em produção.)_

# OPS — Setup de Produtos AbacatePay v2

> **Pré-requisito** para o fluxo nativo de assinatura funcionar.
> Sem `plans.abacate_product_id` preenchido, a edge function `subscribe`
> retorna `422 plan_not_subscribable`.

## Migração v1 → v2

A partir das migrations `20260427000000` e `20260427000100`:

- Tabela `plans` ganhou `abacate_product_id` (text) e `billing_cycle`
  (`WEEKLY` | `MONTHLY` | `SEMIANNUALLY` | `ANNUALLY`).
- Plano **Frotista** virou anual (`interval_days=365`, `price_cents=24000`,
  `billing_cycle=ANNUALLY`).
- Tabela `subscriptions` ganhou `abacate_subscription_id` (subs_xxx),
  `cancel_requested_at`, `legacy_v1`. Linhas existentes ativas/pendentes
  foram marcadas `legacy_v1=true` e continuam no fluxo antigo até cancelar.

---

## Passo 1 — Criar produtos no painel AbacatePay

Acesse [app.abacatepay.com](https://app.abacatepay.com) → **Produtos**
→ **Novo produto** para cada plano pago:

| Plano       | Slug         | Frequência     | Preço      |
|-------------|--------------|----------------|------------|
| Flexível    | `flexivel`   | `MONTHLY`      | R$ 30,00   |
| Profissional| `profissional`| `SEMIANNUALLY`| R$ 150,00  |
| Frotista    | `frotista`   | `ANNUALLY`     | R$ 240,00  |

> **Atenção:** o produto deve ter `frequency` setado para que o checkout
> de assinatura funcione (sem `frequency`, o checkout cai em pagamento
> avulso). Métodos: marcar apenas **CARD** — PIX foi descontinuado para
> assinaturas v2.

Após criar, o painel mostra um identificador `prod_xxxxxxxx`. Anote os
três IDs.

## Passo 2 — Gravar IDs em migration

Crie `backend/supabase/migrations/<timestamp>_plans_abacate_product_ids.sql`:

```sql
update plans set abacate_product_id = 'prod_REAL_ID_FLEXIVEL'    where slug = 'flexivel';
update plans set abacate_product_id = 'prod_REAL_ID_PROFISSIONAL' where slug = 'profissional';
update plans set abacate_product_id = 'prod_REAL_ID_FROTISTA'     where slug = 'frotista';
```

Aplicar:

```powershell
cd backend
supabase db push --include-all --password '<DB_PASSWORD>'
```

## Passo 3 — Smoke test

```powershell
# 1. Login mobile / web → gera JWT
# 2. Chamar subscribe
$body = @{ plan_slug = 'flexivel' } | ConvertTo-Json
Invoke-RestMethod -Method Post `
  -Uri 'https://ljeroorpbqnlggiyyttb.supabase.co/functions/v1/subscribe' `
  -Headers @{ Authorization = "Bearer $JWT"; 'Content-Type'='application/json' } `
  -Body $body
# → { checkout_url: 'https://pay.abacatepay.com/...' }
```

Pagar com cartão de teste (`4000 0000 0000 0010`). Esperar webhook
`subscription.completed` chegar (logs em
`supabase functions logs webhook-abacatepay`).

Verificar:

```sql
select id, status, abacate_subscription_id, abacate_billing_id,
       current_period_end, legacy_v1
  from subscriptions where user_id = '<user>';
-- status='active', abacate_subscription_id='subs_xxx' preenchido
```

---

## Troubleshooting

| Sintoma | Causa | Solução |
|---------|-------|---------|
| `422 plan_not_subscribable` | Faltou `abacate_product_id` | Rodar Passo 2 |
| Checkout abre mas cobra valor único | Produto sem `frequency` no painel | Editar produto → setar frequência |
| Webhook chega mas subscription não atualiza | Match falhou — checar `idempotency_key` em `webhook_events.error_message` | Conferir se `data.checkout.externalId` bate com `subscriptions.id` |
| Cancel retorna 502 gateway_error | API AbacatePay devolveu erro ≠ 404 | Ver logs de `cancel-subscription`; pode ser subs_id inválido |

## Migração de assinaturas v1 ativas

Linhas com `legacy_v1=true` continuam funcionando no fluxo PIX antigo.
Quando o usuário cancelar (ou `current_period_end` passar), o cancel
local marca `cancelled` sem chamar a API v2. Para forçar todos a migrar
no próximo renew, basta deixar o tempo passar — não há renovação
automática no fluxo v1 (não havia subscription nativa). O usuário fará
um novo `subscribe` que já entra no fluxo v2.
