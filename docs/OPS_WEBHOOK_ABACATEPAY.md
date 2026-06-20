> ⛔ **OBSOLETO (jun/2026):** substituído por [`OPS_WEBHOOK_ASAAS.md`](OPS_WEBHOOK_ASAAS.md).
> O gateway agora é **Asaas** (função `webhook-asaas`). Não execute este runbook.
> _(Será removido na Fase 7, após validação em produção. O secret citado aqui é de gateway desativado.)_

# Configurar webhook AbacatePay

> A Edge Function `webhook-abacatepay` já está deployada. Falta apontar o gateway para a URL e configurar o secret compartilhado.

URL do webhook em produção:

```
https://ljeroorpbqnlggiyyttb.supabase.co/functions/v1/webhook-abacatepay
```

---

## 1. Gerar segredo do webhook

Já está em `backend/.secrets`:

```
ABACATEPAY_WEBHOOK_SECRET=o/jC4bTX9qnixa8UDTm2EP+n4MO22fKl6MG01/CjpuLlisG017DzpumyxS+y1fjB
```

Para rotacionar:

```powershell
# 32 bytes random base64
[Convert]::ToBase64String((1..32 | ForEach-Object {Get-Random -Maximum 256}))
```

---

## 2. Configurar no Supabase (runtime secret da Function)

```powershell
cd c:\repo\JardulliCalculadora\backend
supabase secrets set ABACATEPAY_WEBHOOK_SECRET='<valor>' --project-ref ljeroorpbqnlggiyyttb
supabase secrets set ABACATEPAY_API_KEY='abc_dev_F0DHHq3NjeceGZ2YhsFL1CA6' --project-ref ljeroorpbqnlggiyyttb
```

Confirmar:

```powershell
supabase secrets list --project-ref ljeroorpbqnlggiyyttb
```

Deve listar `ABACATEPAY_API_KEY`, `ABACATEPAY_WEBHOOK_SECRET` (e os secrets internos do Supabase).

---

## 3. Cadastrar a URL na AbacatePay

1. Acesse <https://app.abacatepay.com> → **Integrações → Webhooks**.
2. **Novo webhook**:
   - **URL**: `https://ljeroorpbqnlggiyyttb.supabase.co/functions/v1/webhook-abacatepay`
   - **Eventos**: marque pelo menos `billing.paid` e `billing.disputed` (lista completa: também `billing.failed`, `billing.cancelled` se quiser tratar no futuro).
   - **Segredo**: cole o mesmo valor de `ABACATEPAY_WEBHOOK_SECRET`.
3. Salvar.

---

## 4. Como o validador funciona

A função usa [`_shared/webhook-validator.ts`](../backend/supabase/functions/_shared/webhook-validator.ts) que compara o segredo enviado pela AbacatePay (header `Authorization: Bearer ...` **ou** query string `?webhookSecret=...`) com o valor da env var.

Se o segredo não bater → resposta `401 Unauthorized` e nada é processado.

> ⚠️ **Importante — `verify_jwt = false`:** o gateway das Edge Functions exige por padrão um JWT do Supabase no header `Authorization`. AbacatePay não envia esse JWT, então a função `webhook-abacatepay` precisa ser publicada com JWT desabilitado:
>
> - via `supabase/config.toml` (já configurado neste repositório):
>   ```toml
>   [functions.webhook-abacatepay]
>   verify_jwt = false
>   ```
> - ou via flag no deploy:
>   ```powershell
>   supabase functions deploy webhook-abacatepay --no-verify-jwt --project-ref ljeroorpbqnlggiyyttb
>   ```
>
> Sem isso, o gateway rejeita com `{"code":"UNAUTHORIZED_NO_AUTH_HEADER"}` antes mesmo do código da função rodar.

> ⚠️ **URL-encode obrigatório:** o secret atual contém `/`, `+` e `=`. Em query string, `+` é decodificado como espaço. Sempre URL-encode o valor antes de montá-lo na URL (no painel da AbacatePay isso é feito automaticamente).

---

## 5. Testar com cURL

```powershell
$raw = 'o/jC4bTX9qnixa8UDTm2EP+n4MO22fKl6MG01/CjpuLlisG017DzpumyxS+y1fjB'
Add-Type -AssemblyName System.Web
$secret = [System.Web.HttpUtility]::UrlEncode($raw)

$body = @{
  event = 'billing.paid'
  data  = @{
    billing = @{
      id        = 'bill_test_123'
      amount    = 30000
      status    = 'PAID'
      createdAt = (Get-Date -Format 'o')
      updatedAt = (Get-Date -Format 'o')
    }
    payment = @{ method = 'PIX'; amount = 30000 }
  }
} | ConvertTo-Json -Depth 6 -Compress

curl.exe -sS -X POST `
  "https://ljeroorpbqnlggiyyttb.supabase.co/functions/v1/webhook-abacatepay?webhookSecret=$secret" `
  -H "Content-Type: application/json" `
  -d $body
```

Alternativa equivalente usando o header `Authorization`:

```powershell
curl.exe -sS -X POST `
  "https://ljeroorpbqnlggiyyttb.supabase.co/functions/v1/webhook-abacatepay" `
  -H "Authorization: Bearer $raw" `
  -H "Content-Type: application/json" `
  -d $body
```

Esperado:

- `OK` (200) na primeira chamada;
- `OK` (200) imediato em chamadas repetidas (idempotency);
- `401` se mudar `webhookSecret`;
- `500` se o handler quebrar (e linha em `webhook_events.error_message` no banco).

---

## 6. Monitorar

- Logs em tempo real:
  ```powershell
  supabase functions logs webhook-abacatepay --project-ref ljeroorpbqnlggiyyttb --tail
  ```
- Eventos não processados:
  ```sql
  SELECT idempotency_key, event_type, error_message, created_at
  FROM webhook_events
  WHERE NOT processed
  ORDER BY created_at DESC
  LIMIT 50;
  ```

---

## 7. Checklist final

- [ ] `ABACATEPAY_WEBHOOK_SECRET` setado em `supabase secrets`
- [ ] `ABACATEPAY_API_KEY` setado em `supabase secrets`
- [ ] Webhook cadastrado no painel AbacatePay com a URL correta
- [ ] Teste manual via cURL retornou `200 OK`
- [ ] Teste de idempotency (chamada duplicada) retornou `200 OK`
- [ ] Tabela `webhook_events` registra o evento com `processed = true`
