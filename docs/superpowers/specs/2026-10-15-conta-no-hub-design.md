# Área de assinatura no jardulli-digital-hub

> Decidido em 15/out/2026 com o usuário. Substitui o site Next.js da VPS
> (`backend/apps/web`) como site oficial de assinaturas.

## Objetivo

O site oficial é `https://jardullimaquinas.com.br`, que é o projeto
`rpmarciano/jardulli-digital-hub` (Vite + React + shadcn, estático, publicado
por FTP na KingHost). Tudo que foi feito no site Next.js passa a viver ali:
páginas legais exigidas pela Play Store, planos com preços corretos, login,
painel da assinatura (assinar, cancelar, histórico), perfil e retorno do
pagamento.

## Decisões

- **Uma única origem**: `https://jardullimaquinas.com.br`. O CORS das Edge
  Functions (`ALLOWED_ORIGINS`) aceita só ela; não há lista nem `www`.
- **Segundo cliente Supabase** (`src/integrations/jardulli/`): o cliente
  existente aponta para o projeto Lovable (`chat-ia`), não para o de
  assinaturas (`ljeroorpbqnlggiyyttb`). Só a publishable key vai no build.
- **Estilo do hub**: componentes shadcn já existentes e os tokens do
  `index.css`. Nada de CSS do site Next.js.
- **Lógica reaproveitada** em `src/lib/conta/` (preços, status, validação de
  CPF/CNPJ, mensagens), com Vitest e o teste `copy-guard` portados.
- **Sessão no navegador** (`localStorage`, chave própria) em vez de cookies de
  servidor; rotas `/conta/*` protegidas no cliente. A segurança continua nas
  RLS e nas Edge Functions, que validam o JWT.

## Rotas

| Rota | Conteúdo |
|---|---|
| `/privacidade`, `/termos`, `/excluir-conta` | Páginas legais (Play Store) |
| `/produtos/assinaturas` | Planos reais; botões levam a `/conta` |
| `/conta/entrar` | Entrar e criar conta (`?modo=criar`) |
| `/conta` | Painel: status, assinar, cancelar, pagamentos |
| `/conta/perfil` | Nome, CPF/CNPJ, telefone |
| `/produtos/cadastro-sucesso` | `APP_COMPLETION_URL`: confirma o pagamento |
| `/produtos/assinatura-retorno` | `APP_RETURN_URL`: voltou antes de pagar |
| `/produtos/cadastro` | Redireciona para `/conta/entrar?modo=criar` (o formulário antigo era só visual) |

## Fora do hub, depois do deploy

1. `ALLOWED_ORIGINS=https://jardullimaquinas.com.br`,
   `APP_RETURN_URL=.../produtos/assinatura-retorno` (Supabase secrets).
2. Supabase Auth: Site URL e Redirect URLs com o domínio do hub (painel).
3. VPS: `jardulli.agrosinergia.com.br` e `jd-sub.cherihub.cloud` passam a
   redirecionar para o hub.
4. URLs da Play Store: `/privacidade` e `/excluir-conta` no domínio do hub.

## Deploy

Continua manual: `npm run build` e envio da pasta `dist/` por FTP. O
`.htaccess` passa a vir de `public/` (vai para `dist/` no build) e reúne o
cache, o fallback da SPA, cabeçalhos de segurança e o redirecionamento para
HTTPS sem `www`.
