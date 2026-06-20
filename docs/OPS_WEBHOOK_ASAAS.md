# Configurar gateway Asaas (secrets + webhook)

> Guia operacional das **Fases 4–5** do [`PLANO_MIGRACAO_ASAAS.md`](PLANO_MIGRACAO_ASAAS.md).
> Pré-requisito: código já mergeado e Edge Functions deployadas (`subscribe`, `cancel-subscription`, `webhook-asaas`, `check-expired`).

Projeto Supabase: `ljeroorpbqnlggiyyttb`

URL do webhook:

```
https://ljeroorpbqnlggiyyttb.supabase.co/functions/v1/webhook-asaas
```

---

## 1. Criar conta e obter a API Key

1. **Sandbox:** crie/acesse a conta em <https://sandbox.asaas.com>.
2. **Configurações → Integrações → API** → copie a API Key.
   - Sandbox começa com `$aact_hmlg_…`; produção com `$aact_prod_…`.
3. Base URL correspondente:
   - Sandbox: `https://api-sandbox.asaas.com/v3`
   - Produção: `https://api.asaas.com/v3`

---

## 2. Gerar o token do webhook

O Asaas envia, em todo POST, o header `asaas-access-token` com o valor que você definir.
**Não use a API key** — gere um token dedicado:

```powershell
# 32 bytes random base64
[Convert]::ToBase64String((1..32 | ForEach-Object {Get-Random -Maximum 256}))
```

---

## 3. Configurar secrets no Supabase

```powershell
cd c:\repo\JardulliCalculadora\backend
supabase secrets set ASAAS_BASE_URL='https://api-sandbox.asaas.com/v3' --project-ref ljeroorpbqnlggiyyttb
supabase secrets set ASAAS_API_KEY='$aact_hmlg_xxxxxxxx'              --project-ref ljeroorpbqnlggiyyttb
supabase secrets set ASAAS_WEBHOOK_TOKEN='<token-do-passo-2>'         --project-ref ljeroorpbqnlggiyyttb
# (APP_COMPLETION_URL / APP_RETURN_URL já devem existir)
```

Confirmar:

```powershell
supabase secrets list --project-ref ljeroorpbqnlggiyyttb
```

Deve listar `ASAAS_API_KEY`, `ASAAS_BASE_URL`, `ASAAS_WEBHOOK_TOKEN`, `APP_COMPLETION_URL`, `APP_RETURN_URL`.

> ⚠️ **`verify_jwt = false`:** o Asaas não envia JWT do Supabase. O `config.toml` já publica a função `webhook-asaas` com JWT desabilitado:
> ```toml
> [functions.webhook-asaas]
> verify_jwt = false
> ```
> Sem isso, o gateway das Edge Functions rejeita com `UNAUTHORIZED_NO_AUTH_HEADER` antes do código rodar.

---

## 4. Cadastrar o webhook no Asaas

Via painel (**Configurações → Integrações → Webhooks → Adicionar**) ou via API:

```powershell
curl.exe -sS -X POST "https://api-sandbox.asaas.com/v3/webhooks" `
  -H "Content-Type: application/json" `
  -H "User-Agent: JardulliCalculadora" `
  -H "access_token: $aact_hmlg_xxxxxxxx" `
  -d '{
    "name": "Jardulli - subscriptions",
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

- `authToken` deve ser **igual** ao `ASAAS_WEBHOOK_TOKEN`.
- Eventos mínimos: `PAYMENT_CONFIRMED` + `PAYMENT_RECEIVED` (ativação/renovação), `PAYMENT_OVERDUE` (atraso), `SUBSCRIPTION_DELETED` (cancelamento). Os demais são defesa em profundidade.

---

## 5. Como o validador funciona

A função `webhook-asaas` usa [`_shared/asaas-webhook-validator.ts`](../backend/supabase/functions/_shared/asaas-webhook-validator.ts):
compara, em tempo constante, o header `asaas-access-token` com `ASAAS_WEBHOOK_TOKEN`.
Se não bater → `401 Unauthorized` e nada é processado. (O Asaas não assina o corpo; não há HMAC.)

---

## 6. Testar com cURL

```powershell
$token = '<token-do-passo-2>'
$body = @{
  id          = "evt_test_123"
  event       = "PAYMENT_CONFIRMED"
  dateCreated = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
  payment     = @{
    object             = "payment"
    id                 = "pay_test_123"
    customer           = "cus_test_123"
    subscription       = "sub_test_123"
    value              = 30.0
    billingType        = "CREDIT_CARD"
    status             = "CONFIRMED"
    externalReference  = "<uuid-da-subscription-pending>"
    paymentDate        = (Get-Date -Format 'yyyy-MM-dd')
  }
} | ConvertTo-Json -Depth 6 -Compress

curl.exe -sS -X POST `
  "https://ljeroorpbqnlggiyyttb.supabase.co/functions/v1/webhook-asaas" `
  -H "asaas-access-token: $token" `
  -H "Content-Type: application/json" `
  -d $body
```

Esperado:
- `OK` (200) na primeira chamada;
- `OK` (200) em chamada repetida com o mesmo `evt_id` (idempotência);
- `401` se o token estiver errado;
- `500` se o handler quebrar (linha em `webhook_events.error_message`).

> Para o smoke test real, prefira pagar o `checkout_url` na página hospedada do Asaas com o cartão de teste `4111 1111 1111 1111` (CVV qualquer, validade futura) e deixar o Asaas disparar o webhook de verdade.

---

## 7. Fluxo E2E (sandbox)

1. Login no app → completar perfil (nome, CPF/CNPJ válido, telefone).
2. Assinar → `subscribe` devolve `checkout_url` → abrir no navegador.
3. Pagar com cartão de teste na página do Asaas.
4. Webhook `PAYMENT_CONFIRMED` chega → subscription `active`.
5. Validar no banco:
   ```sql
   select id, status, asaas_subscription_id, asaas_checkout_id,
          current_period_start, current_period_end
     from subscriptions where user_id = '<uuid>';
   ```
   Deve estar `status='active'`, `asaas_subscription_id='sub_xxx'`, período ≈ now()+interval do plano.
6. App (polling) reflete o plano via `get_active_subscription`.
7. Cancelar pelo app → `DELETE /v3/subscriptions/{id}` → `SUBSCRIPTION_DELETED` → acesso mantido até `current_period_end`.

---

## 8. Monitorar

- Logs em tempo real:
  ```powershell
  supabase functions logs webhook-asaas --project-ref ljeroorpbqnlggiyyttb --tail
  ```
- Eventos não processados:
  ```sql
  SELECT idempotency_key, event_type, error_message, created_at
  FROM webhook_events
  WHERE NOT processed
  ORDER BY created_at DESC
  LIMIT 50;
  ```

> ⚠️ Após **15 falhas consecutivas** o Asaas **pausa a fila** do webhook (reative no painel). Eventos ficam 14 dias para reprocessamento. Por isso a função sempre responde 200, exceto erro real de handler (500, para o Asaas retentar).

---

## 9. Produção

1. Trocar secrets para os valores de produção:
   ```powershell
   supabase secrets set ASAAS_BASE_URL='https://api.asaas.com/v3'    --project-ref ljeroorpbqnlggiyyttb
   supabase secrets set ASAAS_API_KEY='$aact_prod_xxxxxxxx'          --project-ref ljeroorpbqnlggiyyttb
   ```
2. Re-cadastrar o webhook na conta de produção (passo 4) com a URL de produção.
3. Smoke test com valor real baixo + cancelamento.

---

## 10. Checklist

- [ ] `ASAAS_API_KEY`, `ASAAS_BASE_URL`, `ASAAS_WEBHOOK_TOKEN` setados em `supabase secrets`
- [ ] Webhook cadastrado no Asaas com `authToken` == `ASAAS_WEBHOOK_TOKEN`
- [ ] Teste cURL retornou `200 OK` + idempotência OK + `401` com token errado
- [ ] Smoke test E2E com cartão de teste passou
- [ ] `webhook_events` registra o evento com `processed = true`
- [ ] `subscriptions` ativou com `asaas_subscription_id` populado
