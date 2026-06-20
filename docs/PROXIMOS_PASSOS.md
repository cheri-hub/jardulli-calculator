> ⛔ **OBSOLETO (jun/2026):** este roteiro era do deploy do **AbacatePay v2**, gateway já substituído
> por **Asaas**. Use [`PLANO_MIGRACAO_ASAAS.md`](PLANO_MIGRACAO_ASAAS.md) (status/escopo) e
> [`OPS_WEBHOOK_ASAAS.md`](OPS_WEBHOOK_ASAAS.md) (Fases 4–5). _(Removível na Fase 7.)_

# Próximos Passos — Migração AbacatePay v2

> Estado atual: código v2 implementado e build verde.
> Faltam apenas ações operacionais (painel + deploy) e testes ponta a ponta.

## Contexto

| Item | Status |
|------|--------|
| Migrations criadas (plans + subscriptions) | ✅ Local |
| Edge functions refatoradas (subscribe / cancel / webhook) | ✅ Local |
| HMAC validator | ✅ Local |
| Documentação de ops | ✅ [`OPS_ABACATE_PRODUCTS_SETUP.md`](OPS_ABACATE_PRODUCTS_SETUP.md) |
| Produtos criados no painel AbacatePay | ⏸ Pendente |
| `abacate_product_id` populado em `plans` | ⏸ Pendente |
| Migrations aplicadas no remoto | ⏸ Pendente |
| Functions deployadas | ⏸ Pendente |
| Smoke test E2E (cartão de teste) | ⏸ Pendente |
| App Android consumindo novo fluxo | ⏸ Validar |

---

## Passo 1 — Criar produtos no painel AbacatePay

Acesse [app.abacatepay.com](https://app.abacatepay.com) → **Produtos** → **Novo produto**:

| Plano | Slug | `frequency` | Preço |
|-------|------|-------------|-------|
| Flexível | `flexivel` | `MONTHLY` | R$ 30,00 |
| Profissional | `profissional` | `SEMIANNUALLY` | R$ 150,00 |
| Frotista | `frotista` | `ANNUALLY` | R$ 240,00 |

Métodos: marcar **somente CARD**. Anote os três `prod_xxxxxxxx` retornados.

## Passo 2 — Migration com IDs reais

Criar `backend/supabase/migrations/20260427000200_plans_abacate_product_ids.sql`:

```sql
update plans set abacate_product_id = 'prod_PkBgEtF14ncF3FwB4mJECxxF'    where slug = 'flexivel';
update plans set abacate_product_id = 'prod_hhj1qZcTuRekqnmYp5BK1wDM' where slug = 'profissional';
update plans set abacate_product_id = 'prod_GUEf0WJhAqdHt3NrtpAws4Bg'    where slug = 'frotista';
```

## Passo 3 — Aplicar no remoto

```powershell
cd c:\repo\JardulliCalculadora\backend
supabase db push --include-all --password '<DB_PASSWORD>'
supabase functions deploy subscribe cancel-subscription webhook-abacatepay `
  --project-ref ljeroorpbqnlggiyyttb --no-verify-jwt
```

## Passo 4 — Smoke test E2E

1. Login no app Android (ou via curl no Auth) → obter JWT.
2. `POST /functions/v1/subscribe` com `{ "plan_slug": "flexivel" }`.
3. Abrir `checkout_url` no navegador → pagar com cartão de teste `4000 0000 0000 0010`.
4. Conferir webhook em `supabase functions logs webhook-abacatepay --project-ref ljeroorpbqnlggiyyttb`.
5. Validar no banco:
   ```sql
   select id, status, abacate_subscription_id, abacate_billing_id,
          current_period_start, current_period_end, legacy_v1
     from subscriptions where user_id = '<uuid>';
   ```
   Deve estar `status='active'`, `abacate_subscription_id='subs_xxx'`, `current_period_end ≈ now()+30 dias`.
6. `POST /functions/v1/cancel-subscription` com `{ "subscription_id": "<uuid>" }`.
7. Aguardar webhook `subscription.cancelled` → `status='cancelled'`, `cancelled_at` preenchido.

## Passo 5 — Validações no app Android

- `PlanosActivity` precisa exibir Frotista como **anual R$240** (era bianual).
- Fluxo de assinatura (`subscribe`) precisa abrir `checkout_url` em browser/WebView e tratar return.
- Cancelamento via `PerfilActivity`/`PlanosActivity` precisa chamar `cancel-subscription`.
- `PlanoManager.syncComSupabase()` deve atualizar plano após webhook completar.

> **Atenção:** o app ainda envia preferência PIX/CARD em algum ponto? Verificar
> e remover — método é fixo CARD no v2. Se houver tela de seleção, esconder
> ou marcar PIX como indisponível.

## Passo 6 — Migração de assinaturas v1 ativas (`legacy_v1=true`)

- Não há renovação automática no v1; a linha continua ativa até `current_period_end`.
- Usuário cancela → cancelamento local imediato (não chama API v2).
- Próximo `subscribe` cai no fluxo v2 automaticamente.
- Comunicar via email aos usuários `legacy_v1=true` com `current_period_end` próximo.

```sql
-- Lista de usuários a comunicar
select p.email, s.current_period_end, pl.slug
  from subscriptions s
  join profiles p on p.id = s.user_id
  join plans pl on pl.id = s.plan_id
 where s.legacy_v1 = true and s.status = 'active'
 order by s.current_period_end;
```

## Passo 7 — Monitoramento pós-deploy (primeiras 48h)

- `select count(*), event_type, processed from webhook_events group by 2,3;` — taxa de erro deve ser ~0.
- `select * from webhook_events where error_message is not null order by created_at desc limit 50;`
- Logs HMAC: `supabase functions logs webhook-abacatepay --project-ref ljeroorpbqnlggiyyttb | Select-String "HMAC"`.
- Verificar se `Webhook secret validation failed` permanece zero (config gateway-side).

## Passo 8 — Cleanup futuro (após 30 dias com 0 legacy_v1 ativos)

- Remover handlers `billing.paid` e `billing.disputed` de [webhook-abacatepay/index.ts](../backend/supabase/functions/webhook-abacatepay/index.ts).
- Remover `createBilling` de [_shared/abacatepay-client.ts](../backend/supabase/functions/_shared/abacatepay-client.ts).
- Remover `BASE_URL_V1`.
- Remover coluna `legacy_v1` (migration drop).
- Remover tipos v1 de [_shared/types.ts](../backend/supabase/functions/_shared/types.ts).

---

## Checklist rápido para deploy

- [ ] 3 produtos criados no painel com `frequency` correto
- [ ] Migration `20260427000200` com IDs reais commitada
- [ ] `supabase db push` aplicado
- [ ] Functions deployadas (`subscribe`, `cancel-subscription`, `webhook-abacatepay`)
- [ ] HMAC público key conferida (env var não é necessária — está no código)
- [ ] Smoke test passou em sandbox
- [ ] App Android testado contra produção
- [ ] Monitoramento ativo nas primeiras 48h
