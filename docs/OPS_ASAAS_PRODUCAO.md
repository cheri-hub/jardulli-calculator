# Liberar Asaas em Produção (cutover sandbox → produção)

> Pré-requisito: todos os testes em **sandbox** já foram concluídos com sucesso
> (ver fluxo E2E em [`OPS_WEBHOOK_ASAAS.md`](OPS_WEBHOOK_ASAAS.md) §7 e o gate de
> testes em [`PROXIMOS_PASSOS_TESTES_ASAAS.md`](PROXIMOS_PASSOS_TESTES_ASAAS.md)).
> Este documento cobre **apenas** o cutover: trocar as credenciais/URLs de sandbox
> pelas de produção e validar com uma transação real. Para o setup conceitual
> (o que é cada secret, como o validador do webhook funciona, etc.) veja
> [`OPS_WEBHOOK_ASAAS.md`](OPS_WEBHOOK_ASAAS.md) e
> [`PLANO_MIGRACAO_ASAAS.md`](PLANO_MIGRACAO_ASAAS.md) (Fase 6).

Projeto Supabase: `ljeroorpbqnlggiyyttb`

URL do webhook (**a mesma** em sandbox e produção — é a nossa Edge Function):

```
https://ljeroorpbqnlggiyyttb.supabase.co/functions/v1/webhook-asaas
```

---

## 1. Obter as credenciais de produção

A conta de produção do Asaas é **separada** da conta sandbox — não é a mesma
credencial com outra URL.

1. Acesse **<https://app.asaas.com>** (não `sandbox.asaas.com`) com a conta real/comercial.
2. **Configurações → Integrações → API** → copie a API Key de produção.
   - Confirme o prefixo: produção começa com `$aact_prod_…` (sandbox era `$aact_hmlg_…`).
3. Anote a base URL de produção: `https://api.asaas.com/v3`.

---

## 2. Gerar um novo token de webhook (produção)

**Não reaproveite** o `ASAAS_WEBHOOK_TOKEN` usado em sandbox — gere um novo, exclusivo
para produção:

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object {Get-Random -Maximum 256}))
```

Guarde esse valor — ele será usado nos passos 3 e 5.

---

## 3. Atualizar os secrets no Supabase

```powershell
cd c:\repo\JardulliCalculadora\backend

supabase secrets set ASAAS_BASE_URL='https://api.asaas.com/v3'   --project-ref ljeroorpbqnlggiyyttb
supabase secrets set ASAAS_API_KEY='$aact_prod_xxxxxxxx'         --project-ref ljeroorpbqnlggiyyttb
supabase secrets set ASAAS_WEBHOOK_TOKEN='<token-do-passo-2>'    --project-ref ljeroorpbqnlggiyyttb
```

Confirmar:

```powershell
supabase secrets list --project-ref ljeroorpbqnlggiyyttb
```

> `APP_COMPLETION_URL` / `APP_RETURN_URL` não mudam — são independentes do gateway.

---

## 4. Garantir que as Edge Functions estão deployadas

Funções que dependem do Asaas: `subscribe`, `cancel-subscription`, `webhook-asaas`,
`check-expired`.

**Caminho normal (CI):** dar push em `main` alterando algo em `migrations/`,
`functions/` ou `config.toml` dispara `.github/workflows/backend-deploy.yml`, que
roda `supabase db push --include-all` e deploya as 4 funções — mas esse workflow é
protegido pelo ambiente GitHub **`production`**, que exige **aprovação manual** antes
de rodar. Aprove o run em Actions → o workflow pendente → *Review deployments*.

**Deploy manual (se preferir não esperar o CI):**

```powershell
supabase functions deploy subscribe             --project-ref ljeroorpbqnlggiyyttb
supabase functions deploy cancel-subscription    --project-ref ljeroorpbqnlggiyyttb
supabase functions deploy webhook-asaas          --project-ref ljeroorpbqnlggiyyttb
supabase functions deploy check-expired          --project-ref ljeroorpbqnlggiyyttb
```

> `config.toml` já publica `webhook-asaas` com `verify_jwt = false` — nada a mudar aqui;
> sem isso o Asaas seria rejeitado com `UNAUTHORIZED_NO_AUTH_HEADER` antes do nosso
> código rodar (o Asaas não manda JWT do Supabase, só o header `asaas-access-token`).

---

## 5. Cadastrar o webhook na conta de produção do Asaas

Repita o cadastro do webhook — **na conta de produção**, com a base URL e a API key de
produção, e o `authToken` = novo token do passo 2 (não o de sandbox):

```powershell
curl.exe -sS -X POST "https://api.asaas.com/v3/webhooks" `
  -H "Content-Type: application/json" `
  -H "User-Agent: JardulliCalculadora" `
  -H "access_token: $aact_prod_xxxxxxxx" `
  -d '{
    "name": "Jardulli - subscriptions (producao)",
    "url": "https://ljeroorpbqnlggiyyttb.supabase.co/functions/v1/webhook-asaas",
    "email": "admin@suaempresa.com",
    "enabled": true,
    "interrupted": false,
    "apiVersion": 3,
    "authToken": "<token-do-passo-2>",
    "events": [
      "PAYMENT_CONFIRMED",
      "PAYMENT_RECEIVED",
      "PAYMENT_OVERDUE",
      "PAYMENT_REFUNDED",
      "PAYMENT_CHARGEBACK_REQUESTED",
      "SUBSCRIPTION_DELETED",
      "SUBSCRIPTION_INACTIVATED"
    ]
  }'
```

Confirme no painel (**Configurações → Integrações → Webhooks**) que o webhook aparece
como ativo e com a URL correta.

---

## 6. Smoke test com dinheiro real

Faça **uma** assinatura de verdade, com o plano mais barato disponível e um cartão real:

1. Login no app (conta de teste sua, não de um cliente) → completar perfil.
2. Assinar → abrir o `checkout_url` retornado → pagar com cartão real.
3. Confirmar que o webhook `PAYMENT_CONFIRMED` chegou e a assinatura ativou:
   ```sql
   select id, status, asaas_subscription_id, asaas_checkout_id,
          current_period_start, current_period_end
     from subscriptions where user_id = '<uuid-da-conta-de-teste>';
   ```
   Esperado: `status='active'`, `asaas_subscription_id='sub_xxx'` preenchido, período
   coerente com o ciclo do plano.
4. Confirmar que `payment_history` registrou o pagamento.
5. No app, confirmar que o plano aparece como ativo (via `get_active_subscription`).
6. **Cancelar** pelo app → confirmar `DELETE /v3/subscriptions/{id}` no Asaas → evento
   `SUBSCRIPTION_DELETED` chega → acesso permanece até `current_period_end` (ver UI
   "CANCELADA — Ativa até DD/MM").
7. Estornar/cancelar a cobrança real feita no passo 2, se aplicável (evitar cobrar de
   verdade só pelo teste).

---

## 7. Monitorar as primeiras transações reais

Nas primeiras horas/dias após liberar, acompanhe de perto:

```powershell
supabase functions logs webhook-asaas --project-ref ljeroorpbqnlggiyyttb --tail
```

```sql
SELECT idempotency_key, event_type, error_message, created_at
FROM webhook_events
WHERE NOT processed
ORDER BY created_at DESC
LIMIT 50;
```

> ⚠️ Após **15 falhas consecutivas** o Asaas **pausa a fila** do webhook (reative no
> painel de produção se isso acontecer). Eventos ficam retidos 14 dias para reprocessar.

---

## 8. Rollback / contingência

Se o smoke test falhar ou um problema aparecer logo após liberar:

1. **Reverter os secrets para os valores de sandbox** imediatamente, para não continuar
   processando (ou tentando processar) cobranças reais com um backend com bug:
   ```powershell
   supabase secrets set ASAAS_BASE_URL='https://api-sandbox.asaas.com/v3' --project-ref ljeroorpbqnlggiyyttb
   supabase secrets set ASAAS_API_KEY='$aact_hmlg_xxxxxxxx'               --project-ref ljeroorpbqnlggiyyttb
   supabase secrets set ASAAS_WEBHOOK_TOKEN='<token-de-sandbox>'          --project-ref ljeroorpbqnlggiyyttb
   ```
   Isso volta o backend a aceitar apenas o fluxo de teste, sem tocar em cobranças reais
   já feitas (que continuam existindo na conta de produção do Asaas e no nosso banco).
2. **Pausar novas assinaturas no app**, se o problema for grave o suficiente para não
   arriscar mais cobranças reais quebradas — não há feature flag pronta para isso hoje;
   a forma mais rápida seria comentar/desabilitar o botão de assinar em `PlanosActivity`
   e publicar um hotfix, ou usar `check-expired`/RPC para invalidar assinaturas
   problemáticas manualmente via SQL. Trate como último recurso.
3. Investigue com os logs do passo 7 antes de tentar liberar de novo.

---

## 9. Checklist final

- [ ] Login feito em `app.asaas.com` (produção) e `ASAAS_API_KEY` de produção copiada (`$aact_prod_…`)
- [ ] Novo `ASAAS_WEBHOOK_TOKEN` de produção gerado (diferente do de sandbox)
- [ ] Secrets atualizados no Supabase: `ASAAS_BASE_URL`, `ASAAS_API_KEY`, `ASAAS_WEBHOOK_TOKEN`
- [ ] `supabase secrets list` confirma os 3 valores
- [ ] Edge Functions (`subscribe`, `cancel-subscription`, `webhook-asaas`, `check-expired`) deployadas e atualizadas em produção
- [ ] Webhook cadastrado na conta de produção do Asaas, com `authToken` == novo `ASAAS_WEBHOOK_TOKEN`
- [ ] Smoke test com dinheiro real: assinatura ativou (`status='active'`, `asaas_subscription_id` populado, `payment_history` gravado)
- [ ] Smoke test: cancelamento funcionou (acesso mantido até `current_period_end`)
- [ ] Cobrança real de teste estornada/cancelada
- [ ] Logs (`webhook-asaas --tail`) e `webhook_events` monitorados sem erros
- [ ] Plano de rollback revisado e entendido pela equipe antes de anunciar a liberação
