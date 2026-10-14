# Próximos passos — rumo à produção

> Ordem de execução do que falta. O **detalhe e o histórico** de cada item estão em
> [`PENDENCIAS.md`](PENDENCIAS.md) (documento vivo, os IDs `P1…` vêm de lá);
> este arquivo é só a **sequência** e o "como fazer".
> **Última atualização:** 12/out/2026

Quem: **U** = usuário (painéis, chaves, aparelho) · **C** = Claude (código, docs, CLI) · **U+C** = em conjunto.

| Fase | Objetivo | Bloqueia o lançamento? |
|---|---|---|
| 0 | Fechar o que já foi feito (Asaas em produção) | sim |
| 1 | Recuperação de senha + e-mail transacional | sim |
| 2 | Teste em aparelho real | sim |
| 3 | Segurança | sim (P2) |
| 4 | Páginas web em produção | sim |
| 5 | Release assinado e Play Store | sim |
| 6 | Pós-lançamento | não |

---

## Fase 0 — Fechar o Asaas em produção (≈ 30 min) · U

1. **Painel de produção do Asaas → Webhooks → aba "Logs de Webhooks".** Procurar entregas depois de 02/ago:
   - `200` → token bate e o webhook funciona.
   - `401` → token do painel ≠ `ASAAS_WEBHOOK_TOKEN` do Supabase. Em **Editar → Gerar Token**, copiar e gravar logo em seguida:
     `supabase secrets set ASAAS_WEBHOOK_TOKEN='<token>' --project-ref ljeroorpbqnlggiyyttb`
   - Sem linhas → ainda não houve pagamento em produção; seguir para o passo 2. **(P9)**
2. **Smoke test real** ([`OPS_ASAAS_PRODUCAO.md`](OPS_ASAAS_PRODUCAO.md) §6): no app, assinar o plano mais barato com cartão real. O checkout deve abrir em `www.asaas.com` (não `sandbox.asaas.com`). Depois conferir no SQL Editor:
   ```sql
   select event_type, processed, error_message, created_at from webhook_events order by created_at desc limit 10;
   select status, count(*), max(created_at) from subscriptions group by status;
   ```
   Cancelar em seguida e estornar a cobrança de teste. **(P1)**
3. **Senha do banco (P7):** conferir se `SUPABASE_DB_PASSWORD` do environment `production` (GitHub) é a senha atual; se não for, atualizar.
4. **C:** marcar P1, P7 e P9 como resolvidos no `PENDENCIAS.md` com o resultado.

---

## Fase 1 — Recuperação de senha e e-mail (≈ 1 dia) · U+C

Hoje **não existe "esqueci minha senha"**: o `SupabaseClient` só chama `signup`, login, `logout` e `refresh`. Quem esquecer a senha perde o acesso à conta e ao plano pago. **(P10)**

**Fluxo por código de 6 dígitos (tudo dentro do app, sem deep link):**

1. `AuthActivity`: link **"Esqueci minha senha"** → pede o e-mail → `POST /auth/v1/recover`.
2. O Supabase envia um e-mail com o código. **U:** em *Authentication → Email Templates → Reset Password*, trocar o link por `{{ .Token }}` (template pronto em [`email-templates/redefinir-senha.html`](email-templates/redefinir-senha.html)).
3. App pede **código + nova senha** → `POST /auth/v1/verify` (`type: recovery`, e-mail + código) → recebe a sessão → `PUT /auth/v1/user` com a nova senha.
4. **Como foi implementado:** a sessão devolvida pelo `verify` fica só em memória (nunca persistida) e é descartada logo após trocar a senha; o app volta ao login com o e-mail preenchido e o usuário entra com a nova senha (assim a pergunta de transferência de aparelho segue o fluxo normal de login).

**Status: concluído em 14/out** — código mergeado (repo#11), template colado no Supabase e fluxo testado com e-mail real.

**C — a implementar:** 3 métodos no `SupabaseClient` (`recoverPassword`, `verifyRecovery`, `updatePassword`), UI na `AuthActivity` (ou activity própria), validações (mínimo 6 caracteres, como no cadastro), testes das partes puras.
**Cuidados:** mensagem neutra no passo 1 (não revelar se o e-mail existe); tratar código expirado/inválido; limite de tentativas do Supabase.
*Opcional:* "Alterar senha" dentro do Perfil. **O site web também não tem recuperação de senha** (ver Fase 4).

**SMTP próprio (U) — P11:** o e-mail padrão do Supabase tem limite baixo de envios por hora e é só para teste. Ele também envia a **confirmação de cadastro** (o app já mostra "Verifique seu email"). Configurar em *Authentication → SMTP Settings* antes do lançamento e testar cadastro e recuperação com um e-mail real. **Concluído em 14/out:** Resend como SMTP do Supabase, DNS na KingHost, e-mails testados.

---

## Fase 2 — Teste em aparelho real (P3) · U

Instalar `app\build\outputs\apk\release\app-release.apk` (assinado) ou o `debug`. Não usar o `app-release-260711.apk`, que é antigo. Roteiro:

- [ ] **PDF** (plano Profissional/Frotista) no Android 10+: gera, salva em Downloads e abre.
- [ ] **Transferência de aparelho:** logar no aparelho A, depois no B com o mesmo e-mail. Testar **Transferir** (A cai no próximo sync) e **Cancelar** (B é deslogado, A mantém).
- [ ] **Cancelamento:** cancelar plano pago → badge "CANCELADA — Ativa até DD/MM" e acesso mantido.
- [ ] **Recuperação de senha** (depois da Fase 1), com e-mail real.
- [ ] Cadastro novo → confirmação por e-mail → login → completar perfil → assinar.
- [ ] Limite do plano TESTDRIVE e do Flexível.

Anotar o que falhar; **C** corrige e atualiza o `PENDENCIAS.md`.

---

## Fase 3 — Segurança · U

- **P2 — Rotacionar a service-role key** que vazou em docs antigos. Gerar a nova no painel do Supabase, atualizar onde é usada (`.secrets` local; as Edge Functions recebem a chave do próprio Supabase), conferir que `subscribe`/`webhook-asaas` continuam respondendo e só então revogar a antiga.
- **P8 — `asaas.txt`:** mover as chaves para um gerenciador de senhas e apagar o arquivo.
- **P6 — Aprovação no deploy:** em *Settings → Environments → production*, ligar **Required reviewers** no repo do backend.

---

## Fase 4 — Páginas web em produção (P12 e P13) · U+C

**Estado hoje (verificado em 12/out):** site Next.js 16 (`backend/apps/web`) no ar em `https://jd-sub.cherihub.cloud` (VPS, nginx + systemd; o `deploy.yml` publica a cada push em `apps/web` na `main`, **sem ambiente de homologação**). Rotas: `/`, `/auth`, `/app`, `/app/profile`, `/completion`, `/return`, `/api/health`. O painel `/app` permite **cancelar assinatura** (chama `cancel-subscription` do navegador). `/completion` e `/return` são os destinos de redirecionamento do checkout do Asaas (`APP_COMPLETION_URL` / `APP_RETURN_URL`). `/privacidade` e `/termos` dão **404**; não há recuperação de senha. `supabase/test-app/` são páginas HTML de teste de desenvolvimento.

**Ordem decidida:** primeiro o **conteúdo** (4A), no endereço atual; depois o **domínio final** (4B).

### 4A — Conteúdo (primeiro) · P12

1. **Definir o conteúdo** (U): textos da home, de `/auth`, do painel `/app`, de `/completion` e `/return`, dados de contato/suporte (alinhados ao `SuporteActivity` do app), e planos e preços **iguais aos do app e do banco**. Sem esse conteúdo definido, o resto não fecha.
2. **Páginas públicas `/privacidade` e `/termos`** (C + U): a Play Store exige uma URL de política de privacidade. O app coleta e-mail, CPF/CNPJ e telefone (LGPD: finalidade, retenção, como pedir exclusão). O texto jurídico é decisão sua.
3. **Recuperação de senha no site** (C): mesmo fluxo da Fase 1 (o site também não tem).
4. **Revisão e fluxo ponta a ponta** (U): textos de `not-found`/`error`, e o caminho assinar no app → checkout → `/completion` → app mostra o plano; voltar/expirar → `/return`; cancelar pelo painel web.
5. **CORS** (C): o secret `ALLOWED_ORIGINS` **não existe**, então as Edge Functions aceitam `*`. Definir o endereço atual do site agora e trocar na 4B. `_shared/cors.ts` só usa o **primeiro** item da lista; para mais de uma origem é preciso ajustar o código.
6. **Operação** (C): o deploy faz `rsync --delete` e não guarda a versão anterior; criar um jeito de voltar atrás e considerar aprovação no deploy da web, como no do Supabase.
7. **`supabase/test-app/`:** garantir que nunca seja publicado (o `env.js` local tem chaves).

### 4B — Domínio final (depois do conteúdo) · P13

Hoje é um subdomínio de `cherihub.cloud`. Ao trocar:

1. **DNS** apontando para a VPS, `server_name` e **certificado TLS** no nginx (e renovação automática).
2. **`/etc/jardulli/jardulli-web.env` na VPS** (U): `NEXT_PUBLIC_SITE_URL` com o domínio novo, `NEXT_PUBLIC_SUPABASE_URL` do projeto de produção e a **anon key** (nunca a service-role). As `NEXT_PUBLIC_*` entram no build, então precisa de **novo build**.
3. **Secrets do Supabase:** atualizar `APP_COMPLETION_URL` e `APP_RETURN_URL` para `<domínio>/completion` e `/return` (os valores atuais não aparecem no CLI; conferir) e verificar se o Asaas de produção aceita o domínio nos callbacks do checkout.
4. **`ALLOWED_ORIGINS`** com o domínio novo; atualizar também o link da política de privacidade na Play Store.
5. Manter o endereço antigo redirecionando por um tempo (`301`), para não quebrar links já enviados.

---

## Fase 5 — Release e Play Store · U+C

1. **Keystore (P4):** guardar uma **cópia fora do computador** (keystore + senhas). Perder o keystore impede atualizar o app. Considerar ativar o **Play App Signing** na Play Console.
2. **Versão:** definir `versionCode`/`versionName` (hoje 1 / "1.0"); incrementar a cada envio.
3. **Minify:** decidir `isMinifyEnabled`. Se ligar, **C** valida o build release (iText e OkHttp costumam pedir regras no `proguard-rules.pro`) e **U** testa o APK de ponta a ponta.
4. **Gerar o AAB** (a Play Store exige AAB em app novo): `./gradlew bundleRelease`.
5. **Ficha da loja (P5):** política de privacidade (a URL pública vem da Fase 4A; o app coleta **e-mail, CPF/CNPJ e telefone** — descrever o uso e a exclusão de dados), seção *Segurança dos dados* da Play Console, screenshots, classificação etária.
6. Publicar primeiro numa **trilha de teste interno**, instalar pela loja e repetir o smoke test da Fase 2 antes de ir para produção.

---

## Fase 6 — Pós-lançamento

- Acompanhar nos primeiros dias: `supabase functions logs webhook-asaas --project-ref ljeroorpbqnlggiyyttb --tail`, `webhook_events` com `processed = false`, aba de logs do Asaas.
- Rollback e contingência: [`OPS_ASAAS_PRODUCAO.md`](OPS_ASAAS_PRODUCAO.md) §8.
- Melhorias já mapeadas (ver `PENDENCIAS.md` §2 e §3): avisar no aparelho antigo quando perder o acesso por transferência, i18n, acessibilidade, `onDowngrade` do `DBHelper`, mais testes.
- **Higiene dos docs:** reescrever `OPS_E2E_TEST.md` para Asaas, apagar `OPS_*ABACATE*`, `PROXIMOS_PASSOS_TESTES_ASAAS.md` e atualizar o `backend/README.md`.

---

## Regra

Ao concluir um passo: marcar aqui (`[x]` ou remover) **e** atualizar o `PENDENCIAS.md` (mover para "Resolvido" com data e PR), conforme o `CLAUDE.md`.
