# Teste end-to-end do fluxo de assinatura

> Smoke test manual cobrindo cadastro → perfil → assinatura PIX → balanço → cancelamento.
> Execute após cada deploy em produção ou mudanças nas Edge Functions.

Estimativa: ~15 minutos.

---

## Pré-requisitos

- APK debug instalado em um dispositivo Android real (PIX precisa de app bancário). Build:
  ```powershell
  cd c:\repo\JardulliCalculadora
  .\gradlew.bat installDebug
  ```
- Email descartável para o cadastro (ex.: `+test@gmail.com`).
- App bancário com PIX habilitado e R$ 0,01 disponível (a AbacatePay em modo dev usa cobrança de centavos).
- CPF/CNPJ válido (use um gerador de teste — Receita rejeita aleatórios).

---

## 1. Cadastro (AuthActivity)

1. Abrir o app → **Entrar** → **Criar conta**.
2. Preencher email + senha (mín. 6 caracteres).
3. **Esperado:**
   - Toast “Conta criada” e navegação para `MainActivity`.
   - No Supabase Dashboard → **Authentication → Users** aparece o novo usuário.
   - Tabela `profiles` recebe linha automática (trigger `handle_new_user`).

---

## 2. Perfil (PerfilActivity)

1. Menu → **Perfil**.
2. Preencher: Nome completo, CPF (ou CNPJ), Telefone (`(19) 99942-9110`).
3. Salvar.
4. **Esperado:**
   - Toast “Perfil atualizado”.
   - No banco:
     ```sql
     SELECT full_name, cpf_cnpj, phone FROM profiles WHERE id = '<user_id>';
     ```
     deve retornar os valores normalizados (CPF só dígitos).
   - CPF inválido (ex.: `111.111.111-11`) deve ser rejeitado em UI sem chamar o backend.

---

## 3. Selecionar plano (PlanosActivity)

1. Menu → **Planos**.
2. **Esperado:** lista mostra 4 planos (Testdrive, Flexível, Profissional **R$ 300/ano**, Frotista **R$ 480/2 anos**).
3. Tocar em **Profissional**.
4. Confirmar diálogo.
5. **Esperado:**
   - App abre URL do checkout AbacatePay (`https://...abacatepay.com/...`).
   - No banco:
     ```sql
     SELECT id, status, plan_id, abacate_billing_id
     FROM subscriptions WHERE user_id = '<user_id>'
     ORDER BY created_at DESC LIMIT 1;
     ```
     status = `pending`, `abacate_billing_id` preenchido.

---

## 4. Pagamento PIX

1. No checkout AbacatePay, escolher **PIX**.
2. Escanear QR no app bancário e pagar (R$ 0,01 em modo dev).
3. Aguardar até 30s.
4. **Esperado:**
   - AbacatePay envia `billing.paid` para o webhook.
   - Logs:
     ```powershell
     supabase functions logs webhook-abacatepay --project-ref ljeroorpbqnlggiyyttb --tail
     ```
     mostram `Webhook received` + `Subscription activated/renewed`.
   - Banco:
     ```sql
     SELECT status, current_period_start, current_period_end FROM subscriptions WHERE id = '<sub_id>';
     SELECT * FROM payment_history WHERE subscription_id = '<sub_id>';
     SELECT processed, error_message FROM webhook_events ORDER BY created_at DESC LIMIT 1;
     ```
     - subscription `status = 'active'`, `current_period_end ≈ now() + 365 dias`
     - payment_history tem 1 linha
     - webhook_events `processed = true`, `error_message IS NULL`

---

## 5. Voltar ao app e validar plano

1. Voltar ao app → menu → **Planos**.
2. **Esperado:** plano Profissional marcado como **ATIVO** com botão vermelho **Cancelar Plano** visível.
3. Menu → **Perfil**: mostra plano atual = Profissional.

---

## 6. Realizar um balanço

1. `MainActivity` → fluxo: TratorOperação → Rodados → Lastro → Pesos → Relatório Final.
2. **Esperado:** abre `RelatorioFinalActivity` com cálculos.
3. Tocar em **Gerar PDF**.
4. **Esperado:**
   - PDF salvo em Downloads e abre via FileProvider.
   - Banco:
     ```sql
     SELECT id, modelo_trator, peso_total, created_at
     FROM historico_balancos WHERE user_id = '<user_id>'
     ORDER BY created_at DESC LIMIT 1;
     ```
     1 linha nova.
   - RPC server-side:
     ```sql
     SELECT count_balancos_mes_atual();
     ```
     retorna ≥ 1 (chamado como o usuário, via SQL Editor → Run as → user).

---

## 7. Histórico

1. Menu → **Histórico**.
2. **Esperado:** lista mostra o balanço recém criado (com data e modelo).

---

## 8. Cancelar assinatura

1. Menu → **Planos** → botão vermelho **Cancelar Plano**.
2. Confirmar diálogo.
3. **Esperado:**
   - Toast informando “Cancelado, acesso até dd/mm/aaaa”.
   - Banco:
     ```sql
     SELECT status, cancelled_at FROM subscriptions WHERE id = '<sub_id>';
     ```
     `status = 'cancelled'`, `cancelled_at` preenchido, `current_period_end` **inalterado** (acesso continua até o fim do período).

---

## 9. Rate-limit

1. Tocar **Cancelar Plano** repetidamente (>5x em 60s).
2. **Esperado:** ao 6º clique a Edge Function retorna `429` com `rate_limited`. App mostra mensagem de erro.

---

## 10. Cron / expiração (opcional, requer modo dev)

Se quiser testar a transição `active → past_due`:

```sql
-- Forçar vencimento em uma subscription de teste
UPDATE subscriptions
SET current_period_end = now() - interval '1 day'
WHERE id = '<sub_id>';

-- Disparar a função
```

```powershell
$key = 'sb_secret_XXXXXXXXXXXXXXXXXXXXXXXX'
curl.exe -X POST "https://ljeroorpbqnlggiyyttb.supabase.co/functions/v1/check-expired" `
  -H "Authorization: Bearer $key" -d '{}'
```

```sql
SELECT status FROM subscriptions WHERE id = '<sub_id>';
-- esperado: 'past_due'
```

---

## 11. Checklist final

- [ ] Cadastro cria usuário + profile
- [ ] Perfil rejeita CPF inválido (Mod 11)
- [ ] Planos lista 4 opções com preços corretos
- [ ] Subscribe abre checkout PIX e cria subscription `pending`
- [ ] Webhook `billing.paid` ativa subscription e registra payment
- [ ] Plano ativo aparece no app com botão Cancelar
- [ ] Balanço persiste em `historico_balancos`
- [ ] RPC `count_balancos_mes_atual()` reflete a contagem
- [ ] Histórico mostra o balanço
- [ ] Cancelamento marca `cancelled_at` mantendo `current_period_end`
- [ ] Rate-limit retorna 429 após 5 chamadas em 60s
- [ ] (Opcional) `check-expired` move `active` → `past_due` quando vencido

---

## Cleanup

```sql
-- Limpar dados de teste
DELETE FROM payment_history WHERE subscription_id IN (SELECT id FROM subscriptions WHERE user_id = '<user_id>');
DELETE FROM subscriptions WHERE user_id = '<user_id>';
DELETE FROM historico_balancos WHERE user_id = '<user_id>';
DELETE FROM rate_limits WHERE user_id = '<user_id>';
DELETE FROM profiles WHERE id = '<user_id>';
-- O usuário em auth.users só pode ser removido pelo dashboard ou via admin API
```
