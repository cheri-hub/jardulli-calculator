# Pendências — JardulliCalculadora

> **Documento vivo.** Deve ser atualizado a cada task concluída (regra no `CLAUDE.md`):
> mover o item para "Resolvido" com data e PR, e registrar o que for descoberto no caminho.
> **Última atualização:** 15/out/2026 · ordem de execução em [`PROXIMOS_PASSOS.md`](PROXIMOS_PASSOS.md)

Legenda: ⬜ aberto · 🔍 a confirmar · ✅ resolvido

---

## 1. Para ir a produção

| # | Item | Status | Quem |
|---|---|---|---|
| P1 | **Cutover do Asaas sandbox → produção** (chave `$aact_prod_`, `ASAAS_BASE_URL`, novo `ASAAS_WEBHOOK_TOKEN`, webhook na conta de produção, smoke test com cartão real). Runbook: [`OPS_ASAAS_PRODUCAO.md`](OPS_ASAAS_PRODUCAO.md) | 🔍 em 12/out o usuário testou a assinatura ponta a ponta e o plano ativou. Usuário confirmou (12/out) que o domínio do checkout é o de produção. **Falta:** **cancelar/estornar** a assinatura de teste (é recorrente: cobra de novo no próximo ciclo) | Usuário |
| P3 | **Testar em aparelho real** o que foi entregue sem teste manual: PDF no Android 10+ (gera e abre), transferência de dispositivo (Transferir / Cancelar), badge "CANCELADA — Ativa até DD/MM" após cancelar plano pago, rodapé "Powered by Agrosinergia" (P14) | ⬜ | Usuário |
| P4 | **Release assinado.** **Feito (15/out):** `applicationId` trocado de `com.example.jardullicalculadora` (a Play Store recusa `com.example`) para **`br.com.agrosinergia.jardulli`**, definitivo depois de publicado; o `namespace` Java segue `com.example.jardullicalculadora` (não afeta a loja); authority do FileProvider no manifesto agora é `${applicationId}.fileprovider`; `./gradlew bundleRelease` gera o AAB assinado com o `app/jardulli.jks` (fora do git); criado `local.properties.example`. **Falta (usuário):** (a) **copiar `app/jardulli.jks` e as senhas para fora do computador** (perder o keystore impede atualizar o app); (b) instalar o app com o pacote novo (é outro app para o Android: desinstale o antigo) e repetir o roteiro do P3; (c) decidir `versionName` (hoje "1.0") e `isMinifyEnabled` (hoje `false`; se ligar, o iText provavelmente precisa de regras no `proguard-rules.pro`); (d) considerar o Play App Signing na Play Console | 🔍 | Usuário + Claude |
| P5 | **Ficha da Play Store**: política de privacidade (URL: `https://jardullimaquinas.com.br/privacidade`, ver P21), screenshots, classificação etária, formulário de segurança dos dados; exige também a exclusão de conta (P16) | ⬜ | Usuário |
| P6 | Environment `production` do repo do backend **sem *required reviewers***: todo push em `supabase/` na `main` faz deploy direto em produção | ⬜ opcional | Usuário |
| P7 | Secret `SUPABASE_DB_PASSWORD` do environment `production` é de 26/abr, mas o `.secrets` foi alterado em 02/ago. Se a senha do banco realmente mudou, atualizar o secret (senão o próximo deploy falha em `supabase link`) | 🔍 | Usuário |
| P8 | `asaas.txt` na raiz do app guarda chaves reais em texto puro (gitignored). Após o cutover, mover para um gerenciador de senhas e apagar o arquivo | ⬜ | Usuário |
| P9 | `ASAAS_WEBHOOK_TOKEN` no Supabase não é alterado desde **18/jun** (era o de sandbox); o runbook pede um token novo para produção. Se o `authToken` do webhook de produção no Asaas for diferente do secret, todo webhook volta `401` e assinaturas pagas não ativam. Garantir que os dois são iguais (ou gerar um novo e atualizar os dois) | 🔍 se a assinatura de 12/out foi em produção, o webhook funcionou e o token está correto; confirmar junto com o P1 | Usuário |
| P12 | **(substituído pelo P21) Páginas web — conteúdo (primeiro).** O site (`backend/apps/web`) está no ar em `jardulli.agrosinergia.com.br` (antes `jd-sub.cherihub.cloud`, que redireciona). **Feito:** (13/out, jardulli-subscription#4 e #5) `/privacidade`, `/termos` e `/excluir-conta`, com rodapé de razão social, CNPJ e endereço; (14/out, jardulli-subscription#6) **textos de todas as telas** reescritos para o cliente (nova home com planos e preços, entrar, painel, perfil, retorno do pagamento, erro e 404), sem jargão técnico e com acentos, protegidos por teste (`copy-guard`). (14/out, jardulli-subscription#7 e #8) **(C) CORS** restrito: secret `ALLOWED_ORIGINS=https://jardulli.agrosinergia.com.br` definido no Supabase (preflight verificado: devolve só essa origem); **(D) rollback do deploy**: o `setup.sh` guarda o build anterior (`.next.prev`) e volta sozinho se o health-check falhar, e o workflow "Deploy to VPS" aceita o campo `ref` (SHA/tag) para republicar uma versão antiga; **(E)** CI da web (`web-ci.yml`) e guarda `repo-guards` que barra `env.js` com chaves e `supabase/test-app/` versionado. O 1º deploy com o `setup.sh` novo passou e `/`, `/auth` e `/api/health` respondem 200; o rollback automático em si ainda não foi exercitado numa falha real. **Decidido:** (B) recuperação de senha **só no app**, não no site. **Falta:** opcional exigir aprovação no deploy (Settings → Environments → `vps-production` → Required reviewers); link da Google Play na home (hoje "em breve", desativado); revisar visualmente as telas no site publicado. **Atenção:** os preços existem em três lugares que precisam bater: `Plano.java` (app), tabela `plans` (banco) e `lib/plans-catalog.ts` (home). Detalhes em [`PROXIMOS_PASSOS.md`](PROXIMOS_PASSOS.md) (Fase 4A) | 🔍 | Usuário + Claude |
| P13 | **(substituído pelo P21) Páginas web — domínio final (depois do conteúdo).** **Feito em 14/out (jardulli-subscription#9):** site em `https://jardulli.agrosinergia.com.br` (DNS A → VPS, certificado Let's Encrypt, nginx), `NEXT_PUBLIC_SITE_URL` novo + build, endereço antigo `jd-sub.cherihub.cloud` redirecionando (301), secrets do Supabase `ALLOWED_ORIGINS`, `APP_RETURN_URL` e `ALLOWED_ORIGINS` trocados; **`APP_COMPLETION_URL` = `https://jardullimaquinas.com.br/produtos/cadastro-sucesso`** (página que a Jardulli já tem; a `/completion` do nosso site deixou de ser o destino do pagamento) (preflight verificado). **A confirmar:** (a) o Asaas aceita os domínios dos callbacks do checkout (assinar no app e ver se volta para `cadastro-sucesso`; se falhar, cadastrar o site em Asaas → Configurações → Informações); (b) Supabase → Authentication → URL Configuration: **Site URL** e redirect URLs com o domínio novo (o link do e-mail de confirmação de cadastro usa o Site URL); (c) atualizar o link da política de privacidade na Play Store (P5); (d) renovação automática do certificado novo (`certbot renew --dry-run`). Detalhes na Fase 4B | 🔍 | Usuário + Claude |
| P14 | **Rodapé "Powered by Agrosinergia" no app.** Definido em 13/out: só na `MainActivity` e na `RelatorioFinalActivity`, e também no PDF (pé de cada página, posição absoluta, sem criar página extra; coberto por teste com PDF real). Componente `RodapeAgrosinergia` soma a altura da barra de navegação (Android 15+). **Falta:** ver em aparelho real (Android 15 com navegação por 3 botões e por gestos; PDF gerado) | 🔍 | Usuário |
| P16 | **Exclusão de conta** (exigência da Play Store). Implementado em 13/out: botão "Excluir conta" no Perfil (digitar `EXCLUIR`), Edge Function `delete-account` (cancela no Asaas antes e não exclui nada se o gateway falhar; depois apaga o usuário e a cascata cuida do resto), página pública `/excluir-conta` e [`OPS_EXCLUSAO_CONTA.md`](OPS_EXCLUSAO_CONTA.md) para pedidos por e-mail. Revisada por security-reviewer (sem crítico; o achado alto — assinatura/checkout vivo no Asaas depois da exclusão — foi corrigido). **Falta:** testar em aparelho com uma conta de teste (com e sem assinatura ativa) e informar a URL `/excluir-conta` na Play Console | 🔍 | Usuário |
| P17 | **Revisão jurídica** da Política de Privacidade e dos Termos (minuta escrita pelo Claude com base nos dados tratados; inclui arrependimento de 7 dias, foro de Piracicaba/SP, bases legais e retenção), antes de informar a URL à Play Store | ⬜ | Usuário |
| P18 | **Exclusão de conta sem reautenticação:** qualquer token de acesso válido (vida de 1 h) basta para apagar a conta; só há a confirmação digitada no app. Risco aceito por ora (token roubado exige acesso ao aparelho). Se quiser endurecer: pedir a senha de novo no app ou exigir login recente na função | ⬜ opcional | Claude |
| P19 | **Dados que a exclusão não cobre:** (a) `webhook_events.payload` guarda os payloads brutos do Asaas sem vínculo com a conta (ids `cus_`/`pay_`, valores): definir uma limpeza periódica; (b) o cadastro de cliente (`cus_...`) no Asaas, com nome, CPF/CNPJ, e-mail e endereço, não é removido: decidir com a revisão jurídica (P17); (c) a cascata apaga o `payment_history` local, o Asaas é quem mantém o registro fiscal | ⬜ | Usuário + Claude |
| P20 | **Monitoramento do site (VPS):** em 13/out o VPS (`76.13.68.64`) ficou fora do ar (portas 22, 80 e 443 sem resposta) e o deploy do site falhou até o usuário religar a máquina; não se sabe desde quando estava fora. Falta um monitor simples de `https://jd-sub.cherihub.cloud/api/health` (ex.: UptimeRobot) com alerta por e-mail, e confirmar no provedor se o VPS reinicia sozinho após uma queda | ⬜ | Usuário |
| P21 | **Site oficial no `jardulli-digital-hub`** (decidido em 15/out; substitui o site Next.js da VPS, ver [`docs/superpowers/specs/2026-10-15-conta-no-hub-design.md`](superpowers/specs/2026-10-15-conta-no-hub-design.md)). **Feito (15/out):** PR [rpmarciano/jardulli-digital-hub#2](https://github.com/rpmarciano/jardulli-digital-hub/pull/2) (`/conta`, `/privacidade`, `/termos`, `/excluir-conta`, retorno do pagamento, preços do catálogo, `.htaccess` no `dist/`, testes); secrets do Supabase `ALLOWED_ORIGINS=https://jardullimaquinas.com.br`, `APP_COMPLETION_URL=.../produtos/cadastro-sucesso` e `APP_RETURN_URL=.../produtos/assinatura-retorno`. **Falta:** (a) usuário: **deploy por FTP** (`dist/` com o `.htaccess`, ver topo do `DEPLOY-FTP.md` do hub) e testar login, assinar, cancelar, perfil e o retorno do pagamento no endereço oficial; (b) mergear o PR do hub; (c) Supabase → Authentication → URL Configuration: **Site URL** `https://jardullimaquinas.com.br`; (d) VPS: `jardulli.agrosinergia.com.br` e `jd-sub.cherihub.cloud` passam a redirecionar para o hub (Claude, depois do teste); (e) Play Store: `https://jardullimaquinas.com.br/privacidade` e `/excluir-conta`; (f) revisar o texto de `/privacidade` (menciona o que o site guarda no navegador) junto da revisão jurídica (P17) | 🔍 | Usuário + Claude |

---

## 2. Melhorias e inconsistências conhecidas

- ⬜ **Aparelho desconectado por transferência cai silenciosamente** para TESTDRIVE no próximo sync; falta avisar o usuário no aparelho antigo.
- ⬜ **Single-device é só client-side**: o JWT do aparelho antigo continua válido no servidor até expirar.
- ⬜ **Docs obsoletos** (ainda descrevem o AbacatePay): `OPS_ABACATE_PRODUCTS_SETUP.md`, `OPS_WEBHOOK_ABACATEPAY.md`, `OPS_E2E_TEST.md` (precisa de reescrita para Asaas/cartão) e `backend/README.md`. `PROXIMOS_PASSOS.md` foi reescrito em 12/out (plano real); `PROXIMOS_PASSOS_TESTES_ASAAS.md` segue obsoleto (diz que há 0 testes Deno e sandbox pendente).
- ⬜ **Casos de uso**: `CASOS_DE_USO.md` (UC-6.2) diz que o PDF usa MediaStore — passou a ser verdade só após o fix de 02/ago; reconferir o texto.
- ✅ Duas cópias do backend: o clone antigo `C:epojardulli-subscription` foi apagado em 15/out; trabalhar só pelo `backend/`.
- ⬜ Supabase CLI local está na 2.51.0 (atual: 2.120.0). O login é global: verificar com `supabase projects list` que `Jardulli Subscriptions` aparece antes de usar o CLI (ver `CLAUDE.md`).

---

## 3. Débitos técnicos (herdados da revisão de 25/abr/2026, ainda abertos)

**Médios**
- ⬜ **M1 — i18n:** `strings.xml` tem só `app_name`; todo texto está hardcoded em PT-BR.
- ⬜ **M4 — `DadosJardulli`:** Parcelable manual com 43+ campos; propenso a erro ao adicionar campo. Sugestão: quebrar em sub-DTOs por etapa do wizard.
- ⬜ **M5 — `DBHelper` sem `onDowngrade()`:** instalar build com `DATABASE_VERSION` menor crasha o app.
- ⬜ **M6 — `HistoricoActivity.mesclar()`:** dedup por `(minuto + modelo + peso)` descarta 2 balanços iguais no mesmo minuto; preferir `remoteId`.

**Baixos**
- ⬜ **L1 — Acessibilidade:** quase nenhum `contentDescription` nos layouts.
- ⬜ **L2 — Offline:** checar `ConnectivityManager` antes de chamar o Supabase (hoje falha por timeout de 15s).
- ⬜ **L3 — Logs** não estruturados.
- ⬜ **L4 — Planos legados `pro`/`premium`** continuam como `is_active = false`; remover em migration futura, depois de confirmar zero assinaturas apontando para eles.

**Funcionalidade e infra**
- ⬜ **F1 — Patinagem/Avanço:** `AvancoRatioActivity` valida `velocidadeTeorica`; conferir `velocidadeReal` e integração com o Histórico.
- ⬜ **DT1 — `security-crypto:1.1.0-alpha06`** em produção (risco aceito; sem GA).
- ⬜ **DT2 — Cobertura de testes:** app com 27 testes (`CalculoUtil` 15 + `DocumentValidator` 12); backend com 29 testes Deno (webhook, client, validador). Faltam: `PlanoManager.podeFazerBalanco()`, `mesclar()`, Espresso (Auth → Perfil → Balanço) e testes das functions `subscribe`/`cancel-subscription`.
- ✅ **DT4 — `local.properties.example`** criado em 15/out (Supabase, WhatsApp e chaves de assinatura).

---

## 4. Resolvido

| Data | Item | Referência |
|---|---|---|
| 15/out/2026 | **P2 — Chaves do Supabase:** o usuário desabilitou as *legacy API keys* (a service-role vazada deixa de valer); o app e o site passaram a usar a *publishable key* (`sb_publishable_`), verificado: Auth e REST respondem, `profiles` sem login volta `[]` (RLS) e o build do app embute a publishable. Chaves antigas em `backend/.env` e `.secrets` já estavam revogadas; clone antigo do backend apagado | — |
| 14/out/2026 | **P11 — SMTP próprio:** Resend configurado como SMTP do Supabase (DNS do domínio `agrosinergia.com.br` na KingHost); e-mails de cadastro e de recuperação chegando, testado pelo usuário | painéis Resend/Supabase/KingHost |
| 14/out/2026 | **P10 — Recuperação de senha por código:** template Reset Password no Supabase e fluxo testado com e-mail real pelo usuário | jardulli-calculator#11 |
| 13/out/2026 | Páginas públicas `/privacidade`, `/termos` e `/excluir-conta` no ar, com rodapé (razão social, CNPJ, endereço) | jardulli-subscription#4 e #5 |
| 12/out/2026 | E-mail de confirmação de cadastro personalizado (template colado no painel) e cadastro novo testado ponta a ponta | `email-templates/`, jardulli-calculator#9 |
| 12/out/2026 | Edge Function órfã `webhook-abacatepay` removida de produção (continuava ativa e pública depois da migração) | `supabase functions delete` |
| 12/out/2026 | Verificado via CLI: 18/18 migrations locais aplicadas no remoto; 4 Edge Functions do Asaas `ACTIVE` (deploy de 02/ago) | — |
| 02/ago/2026 | CI do backend e deploy do Supabase movidos para o repo correto (`supabase-ci.yml`, `supabase-deploy.yml`); deploy validado em produção | jardulli-subscription#3, jardulli-calculator#4 |
| 02/ago/2026 | CI do app corrigido (verde pela 1ª vez): removido `org.gradle.java.home` local do `gradle.properties` | jardulli-calculator#3 |
| 02/ago/2026 | PDF não abria no Android 10+: gravação via `MediaStore.Downloads`; conflito de nomes no `file_paths.xml` | jardulli-calculator#3 |
| 02/ago/2026 | Confirmação antes de transferir o acesso a um novo aparelho | jardulli-calculator#3 |
| 28/jun/2026 | UI de cancelamento ("CANCELADA — Ativa até DD/MM") + `cancel_requested_at` no RPC | jardulli-calculator#3, jardulli-subscription#2 |
| 28/jun/2026 | Webhook Asaas não ativava assinatura: passa a casar por `checkoutSession` | jardulli-subscription#2 |
| 27/jun/2026 | `subscribe` sem `customerData` (Asaas exigia endereço); RPC não conta `pending` como ativo; login re-checado antes de assinar | jardulli-subscription#2, jardulli-calculator#2 |
| 20/jun/2026 | Migração AbacatePay → Asaas (schema, functions, webhook, 29 testes Deno) | jardulli-subscription#1 |
| — | **M2** (retry do webhook AbacatePay): obsoleto, o AbacatePay foi removido. O webhook Asaas responde 500 em erro de handler para o Asaas retentar | — |
| — | **M3** (rate-limit nas functions): `subscribe` e `cancel-subscription` usam `_shared/rate-limit.ts` (5 req / 60 s) | migration `20260426000200` |
| — | **DT3** (CI/CD): app e backend com pipeline de CI; backend com deploy automático | ver acima |
| 25/abr/2026 | C1–C3, H1–H5 (RLS do histórico, refresh de token, validação de CPF/CNPJ, testes de cálculo etc.) | revisão de abril |

Também entregues: `CASOS_DE_USO.md` (99 casos), `OPS_ASAAS_PRODUCAO.md`, `CLAUDE.md` reescrito.
