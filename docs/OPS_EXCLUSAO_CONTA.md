# Exclusão de conta — como atender

Há dois caminhos. Os dois estão descritos para o usuário em `https://jd-sub.cherihub.cloud/excluir-conta` (essa é a URL que a Play Console pede).

## 1. Pelo app (automático)

Perfil → **Excluir conta** → digitar `EXCLUIR`. O app chama a Edge Function `delete-account` (repo `jardulli-subscription`), que:

1. cancela no Asaas toda assinatura do usuário (e o checkout, se nunca foi pago);
2. se o Asaas falhar, **devolve erro e não exclui nada** (o usuário vê "Nada foi excluído" e pode tentar de novo);
3. apaga o usuário em `auth.users`, o que remove em cascata `profiles`, `subscriptions`, `payment_history` e `historico_balancos`;
4. limpa as linhas dele em `rate_limits` (sem vínculo por chave estrangeira).

Nada a fazer manualmente. Se uma exclusão falhar de forma repetida, ver os logs da função `delete-account` no painel do Supabase (campos `userId` e `error`).

## 2. Pedido por e-mail (manual, prazo de 15 dias)

Chega em `appjardulli@agrosinergia.com.br` com o assunto "Excluir conta".

1. **Conferir a identidade:** o pedido tem que vir do e-mail cadastrado na conta. Se vier de outro e-mail, pedir confirmação.
2. **Reembolso de 7 dias:** se o cliente pediu reembolso por arrependimento, estornar a cobrança no Asaas **antes** de excluir.
3. **Achar o usuário e as assinaturas** (Supabase → SQL Editor):

   ```sql
   select u.id, u.email, s.id as sub_id, s.status,
          s.asaas_subscription_id, s.asaas_checkout_id
   from auth.users u
   left join subscriptions s on s.user_id = u.id
   where u.email = '<email do pedido>';
   ```

4. **Cancelar no Asaas** (painel do Asaas):
   - com `asaas_subscription_id` (`sub_...`): Assinaturas → cancelar;
   - sem `sub_...` mas com `asaas_checkout_id`: cancelar o checkout (ou só confirmar que está expirado).
5. **Excluir o usuário:** Supabase → Authentication → Users → Delete user. A cascata apaga o resto.
6. **Limpar o rate limit:**

   ```sql
   delete from rate_limits where user_id = '<id do usuário>';
   ```

7. **Responder ao titular** confirmando a exclusão, dentro de 15 dias, e guardar o e-mail do pedido e da resposta.

## O que a exclusão NÃO apaga

- Balanços salvos no aparelho do usuário (ficam só com ele).
- Registros de cobrança do próprio Asaas (retidos por ele para obrigações fiscais).
- Payloads brutos de webhook em `webhook_events` (sem vínculo com a conta; ver P19 em `PENDENCIAS.md`).
- O cadastro de cliente (`cus_...`) no Asaas: a decisão de removê-lo depende da revisão jurídica (P17).
