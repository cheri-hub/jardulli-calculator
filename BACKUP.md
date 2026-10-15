# Backup — o que guardar fora do computador

Estes arquivos e dados **não estão no Git** e só existem nesta máquina. Guarde-os em um
**gerenciador de senhas** ou em uma pasta criptografada, em pelo menos dois lugares.
Este arquivo lista só *nomes e caminhos*, nunca valores: não cole senhas ou chaves aqui.

## Indispensável

| Item | Onde está | Por quê |
|---|---|---|
| Keystore de assinatura | `app/jardulli.jks` | Perder impede atualizar o app na Play Store, para sempre. |
| `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` | `local.properties` | Sem eles o keystore não serve. Guarde separados do `.jks`. |

## Importante (dá para refazer, mas dá trabalho)

| Item | Onde está | Conteúdo |
|---|---|---|
| `local.properties` | raiz | URL e anon key do Supabase, WhatsApp de suporte, dados de assinatura. Modelo: `local.properties.example`. |
| `asaas.txt` | raiz | Chave de API do Asaas (produção) e token do webhook. A chave só aparece uma vez no painel. |
| `backend/.secrets` | `backend/` | Login do Supabase, senha do banco, chaves e connection string. |
| `backend/.env` | `backend/` | Chaves do Supabase, incluindo a **service-role**. |
| Senhas de contas | fora de arquivo | Supabase, Asaas, Resend, KingHost, GitHub, Google Play Console, acesso SSH à VPS (`76.13.68.64`). |

## Não precisa de backup

- Código, docs e workflows: estão no GitHub (`cheri-hub/jardulli-calculator` e `cheri-hub/jardulli-subscription`).
- `~/.gradle/gradle.properties`: só o pin do JDK desta máquina.
- `backend/supabase/test-app/env.js`: arquivo de teste local.

## Cuidados

- `backend/.env` e `backend/.secrets` ainda têm chaves do **AbacatePay** (não usado mais). Ao
  rotacionar a service-role key (P2 em `docs/PENDENCIAS.md`), limpe esses arquivos e mova tudo
  para o gerenciador de senhas; isso também fecha o P8 (`asaas.txt`).
- Mantenha duas cópias do `.jks` em lugares diferentes (por exemplo, gerenciador de senhas e
  um pendrive guardado fora do escritório).
- O **Play App Signing** (Play Console) guarda a chave final de distribuição e reduz o risco,
  mas não substitui o backup do `.jks`.
- Nada disto pode ser commitado: `app/*.jks`, `local.properties`, `asaas.txt`, `.env` e
  `.secrets` já estão no `.gitignore`.
