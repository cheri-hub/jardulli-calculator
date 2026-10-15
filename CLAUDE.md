# CLAUDE.md

Calculadora Android de distribuição de peso/lastro de tratores (Jardulli), com login, planos pagos e relatório em PDF. O backend de assinaturas (Supabase + Asaas) vive na pasta `backend/`.

## Dois repositórios — atenção

- **Raiz** = app Android → `cheri-hub/jardulli-calculator`
- **`backend/`** = repositório **separado** → `cheri-hub/jardulli-subscription` (está em `/backend/` no `.gitignore` do app)
- Commit, push e PR de cada lado são feitos de dentro da respectiva pasta. Nunca commitar `backend/` pelo repo do app.
- Fluxo usado: branch → PR → merge commit (sem squash). Commits em inglês, conventional commits (`feat`, `fix`, `docs`, `ci`, `chore`).

## Comandos

```bash
# App (raiz)
./gradlew assembleDebug          # APK debug
./gradlew installDebug           # instala no device
./gradlew test                   # testes unitários (JVM)
./gradlew test --tests "com.example.jardullicalculadora.CalculoUtilTest"
./gradlew lintDebug

# Backend (cd backend) — precisa de Deno e Supabase CLI
deno fmt --check supabase/functions
deno lint supabase/functions
deno test --allow-all supabase/functions
supabase db push --linked
supabase functions deploy <nome> --project-ref ljeroorpbqnlggiyyttb
```

## Stack

- **App:** Java 11, Android SDK 35 (min 24), AGP 8.9.3 / Gradle 8.11.1, iText 7.2.5 (PDF), OkHttp 4.12, EncryptedSharedPreferences, SQLite.
- **Backend:** Supabase (Auth, Postgres + RLS, Edge Functions em Deno) + gateway **Asaas v3** (substituiu o AbacatePay).

## Arquitetura do app

Cadeia de cálculo — um único `DadosJardulli` (Parcelable, extra `"DADOS_JARDULLI"`) é acumulado e repassado:

```
MainActivity → TratorOperacaoActivity → RodadosActivity → LastroLiquidoActivity
             → PesoFrontalActivity → RelatorioFinalActivity (cálculo final + PDF)
```

Conta e extras: `AuthActivity`, `PerfilActivity` (CPF/CNPJ Mod 11), `PlanosActivity`, `HistoricoActivity`, `SuporteActivity`, `AvancoRatioActivity` (patinagem).

- Sem DI. `SupabaseClient` usa callbacks (não LiveData/coroutines) → todo update de UI em `runOnUiThread()`.
- Offline-first: balanço salvo no SQLite (`DBHelper`) e depois enviado ao Supabase.
- `PlanoManager`: sessão + cache do plano (TTL 1h, cai para TESTDRIVE se expirar sem sync). `getBalancosMesAtual()` usa `Math.max(nuvem, local)` para impedir burla do limite.
- `Plano`: TESTDRIVE, FLEXIVEL, PROFISSIONAL, FROTISTA. `CalculoUtil` = funções puras testáveis.
- Lastro líquido tem 2 fórmulas (`/75*2` e `/40*2`), escolhidas por série e eixo em `usarCalculo1()`.
- Dados de séries: só a JD 8R está no SQLite; as outras ficam em arrays paralelos em `TratorOperacaoActivity`.

## Regras de negócio que já deram bug (não regredir)

- `get_active_subscription()` só retorna `active`/`past_due`. `pending` **não** dá acesso.
- `webhook-asaas` casa a assinatura por `asaas_subscription_id` → `asaas_checkout_id` (`payment.checkoutSession`) → `externalReference`. O Asaas não propaga `externalReference` do checkout para o pagamento.
- `subscribe` **não** envia `customerData` (Asaas exige endereço completo; a página hospedada coleta).
- Cancelar mantém acesso até `current_period_end` (`cancel_requested_at`; UI "CANCELADA — Ativa até DD/MM"). Só `pending` é cancelada na hora.
- 1 aparelho por conta (`profiles.current_device_id`): `AuthActivity` pergunta antes de transferir o acesso. Enforcement é client-side.
- PDF: `MediaStore.Downloads` no Android 10+; `File` + FileProvider só abaixo disso. Nomes em `res/xml/file_paths.xml` precisam ser **únicos** (nome repetido sobrescreve o root).

## Ambiente e segredos

- `local.properties` (não versionado; modelo em `local.properties.example`): `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `WHATSAPP_SUPORTE`; para release assinado, `STORE_FILE`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- **JDK:** CI usa 17. Localmente o JDK 25 (padrão aqui) é incompatível com o AGP; o pin do JDK 23 fica em `~/.gradle/gradle.properties` (`org.gradle.java.home`). **Nunca** pôr `org.gradle.java.home` no `gradle.properties` do projeto — quebra o CI.
- **Supabase CLI:** o login é global e pode estar em outra conta (já aconteceu: dava 403 neste projeto). Antes de `db push`, `secrets` ou `functions`, rodar `supabase projects list` e conferir que `Jardulli Subscriptions` (`ljeroorpbqnlggiyyttb`) aparece. Para não mexer no login dos outros projetos, usar `$env:SUPABASE_ACCESS_TOKEN` só naquela janela do terminal. Sempre passar `--project-ref ljeroorpbqnlggiyyttb` explícito.
- `asaas.txt` na raiz tem chaves reais (gitignored). Não commitar, não copiar para docs. Nunca colar service-role key em arquivo versionado.
- Tokens em EncryptedSharedPreferences (fallback para plain se o KeyStore corromper); `jardulli_plano_prefs` fora do backup. A anon key é pública — a segurança depende das RLS.

## CI/CD

- **App:** `.github/workflows/android.yml` → lint, testes, `assembleDebug`.
- **Backend:** `backend/.github/workflows/` → `supabase-ci.yml` (deno fmt/lint/check/test), `supabase-deploy.yml` (`db push` + deploy das functions no push em `main`; environment `production`, secrets `SUPABASE_ACCESS_TOKEN`/`PROJECT_ID`/`DB_PASSWORD` configurados nele), `deploy.yml` (web Next.js → VPS).
- Edge Functions: `subscribe`, `cancel-subscription`, `webhook-asaas` (`verify_jwt = false`, auth por header `asaas-access-token`), `check-expired`, `delete-account` (exclusão de conta; cancela no Asaas antes de apagar, ver `docs/OPS_EXCLUSAO_CONTA.md`). Projeto Supabase: `ljeroorpbqnlggiyyttb`.
- **Site oficial:** `https://jardullimaquinas.com.br` = repositório `rpmarciano/jardulli-digital-hub` (Vite + React estático, deploy manual por FTP; clone em `C:epojardulli-digital-hub`). Tem `/conta`, páginas legais e retorno do pagamento. O site Next.js em `backend/apps/web` (VPS) está **legado**: só redireciona. O CORS do Supabase aceita só a origem do hub (`ALLOWED_ORIGINS`); ao mudar de domínio, trocar também `APP_COMPLETION_URL`/`APP_RETURN_URL` e o Site URL do Supabase Auth. Detalhes em `docs/PENDENCIAS.md` (P21).

## Docs (`docs/`)

Úteis: `OPS_ASAAS_PRODUCAO.md` (cutover sandbox→prod), `OPS_WEBHOOK_ASAAS.md`, `OPS_EXCLUSAO_CONTA.md`, `PLANO_MIGRACAO_ASAAS.md`, `ASAAS_API.md`, `CASOS_DE_USO.md`, `SETUP_CI_CD.md`, **`PENDENCIAS.md`** (documento vivo, ver acima), **`PROXIMOS_PASSOS.md`** (ordem de execução rumo à produção; marcar o passo concluído junto com o `PENDENCIAS.md`).
**Obsoletos** (ainda falam de AbacatePay): `*ABACATE*`, `OPS_E2E_TEST.md`, `backend/README.md`.

## Pendências — documento vivo (obrigatório)

A lista de pendências (produção, melhorias, débitos técnicos e o histórico do que foi resolvido) fica em **`docs/PENDENCIAS.md`**. É a única fonte; não duplicar a lista aqui.

**Ao finalizar qualquer task**, antes de dar por concluída:
1. Atualizar `docs/PENDENCIAS.md`: mover o item resolvido para "Resolvido" com data e PR (`repo#N`), ou ajustar o status (⬜ aberto · 🔍 a confirmar · ✅ resolvido).
2. Registrar qualquer pendência nova descoberta no caminho, na seção certa.
3. Atualizar a linha "Última atualização" no topo do arquivo.
4. Se a task mexeu nos dois repos, atualizar o doc uma única vez, no repo do app (`docs/` fica aqui), e commitar junto do PR da task.

Não marcar como resolvido o que não foi verificado: se depende de teste em aparelho ou de ação do usuário (painéis, chaves), deixar 🔍 ou ⬜ e dizer isso na resposta.

## Convenções

PT-BR em UI, variáveis, comentários e logs. Layouts `activity_*.xml`; IDs em camelCase com prefixo (`btn`, `txt`, `edit`, `spinner`). Activities travadas em portrait.
