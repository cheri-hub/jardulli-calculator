# Agendar `check-expired` (cron)

> A Edge Function [`check-expired`](../backend/supabase/functions/check-expired/index.ts) faz a transição:
>
> - `active` → `past_due` quando `current_period_end < now()`
> - `past_due` → `expired` após 7 dias atrasados
> - `pending` → `expired` após 24h sem pagamento
>
> Sem cron, assinaturas vencidas continuam contando como ativas indefinidamente.
> Recomendamos rodar a cada **6 horas**.

Existem 2 opções (use **uma**, não as duas).

---

## Opção A — pg_cron + pg_net dentro do Supabase (recomendado)

Mantém tudo dentro do Supabase, sem dependência externa.

### A.1 Habilitar extensões

Dashboard → **Database → Extensions** → habilite:

- `pg_cron`
- `pg_net`

Ou via SQL (já como migration):

```sql
CREATE EXTENSION IF NOT EXISTS pg_cron;
CREATE EXTENSION IF NOT EXISTS pg_net;
```

### A.2 Criar a chamada agendada

Substitua `<ANON_OR_SERVICE_KEY>` pelo `SUPABASE_SECRET_KEY` (service_role) — necessário para invocar Edge Function sem autenticação de usuário.

```sql
-- Roda a cada 6 horas
SELECT cron.schedule(
  'check-expired-every-6h',
  '0 */6 * * *',
  $$
  SELECT net.http_post(
    url     := 'https://ljeroorpbqnlggiyyttb.supabase.co/functions/v1/check-expired',
    headers := jsonb_build_object(
      'Content-Type',  'application/json',
      'Authorization', 'Bearer <SUPABASE_SECRET_KEY>'
    ),
    body    := '{}'::jsonb,
    timeout_milliseconds := 30000
  );
  $$
);
```

### A.3 Verificar agendamento

```sql
SELECT jobid, schedule, jobname, active FROM cron.job;
SELECT * FROM cron.job_run_details ORDER BY start_time DESC LIMIT 10;
```

### A.4 Pausar / remover

```sql
SELECT cron.unschedule('check-expired-every-6h');
```

> ⚠️ **Não comitar a service-role key em SQL versionado.** Crie uma migration que insere via `current_setting('app.settings.service_key')` ou execute o `cron.schedule` manualmente no SQL Editor do dashboard.

---

## 3. Validar manualmente

```powershell
$key = 'sb_secret_XXXXXXXXXXXXXXXXXXXXXXXX'
curl.exe -X POST `
  "https://ljeroorpbqnlggiyyttb.supabase.co/functions/v1/check-expired" `
  -H "Authorization: Bearer $key" `
  -H "Content-Type: application/json" `
  -d '{}'
```

Esperado: `{"marked_past_due":N,"marked_expired":M}` em até 1s.

---

## 4. Logs e troubleshooting

```powershell
supabase functions logs check-expired --project-ref ljeroorpbqnlggiyyttb --tail
```

Verificar transições no banco:

```sql
SELECT status, COUNT(*) FROM subscriptions GROUP BY status;

SELECT id, status, current_period_end, cancelled_at
FROM subscriptions
WHERE status IN ('past_due', 'expired')
ORDER BY current_period_end DESC
LIMIT 20;
```

---

## 5. Checklist final

- [ ] Opção escolhida: **A (pg_cron)** ou **B (GitHub Actions)**
- [ ] Schedule criado e visível em `cron.job` ou no GitHub Actions
- [ ] Invocação manual retornou `{ marked_past_due, marked_expired }`
- [ ] Após o vencimento de uma subscription de teste, ela passou para `past_due` na próxima execução
