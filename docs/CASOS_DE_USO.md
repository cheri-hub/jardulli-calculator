# Casos de Uso — Jardulli Calculadora

> Catálogo abrangente dos casos de uso das features do app Android.
> Gerado em **28/jun/2026**. Baseado no código em `app/src/main/java/...` e no
> backend Supabase (`backend/`).
>
> **Atores:**
> - **Visitante** — usuário não logado.
> - **Usuário** — logado (qualquer plano).
> - **Assinante** — logado com plano pago ativo (Flexível, Profissional, Frotista).
> - **Sistema** — app + backend (Edge Functions, webhook Asaas, cron).
>
> **Planos:** `TESTDRIVE` (grátis, 1 balanço/mês), `FLEXIVEL` (R$30/mês),
> `PROFISSIONAL` (R$150/sem), `FROTISTA` (R$240/ano). Veja a matriz de
> permissões em [§9](#9-gating-por-plano-matriz).

---

## Índice

1. [Navegação / Hub (MainActivity)](#1-navegação--hub)
2. [Conta e Autenticação](#2-conta-e-autenticação)
3. [Perfil](#3-perfil)
4. [Planos e Assinatura](#4-planos-e-assinatura)
5. [Cálculo de Balanço (distribuição de peso)](#5-cálculo-de-balanço)
6. [Relatório e PDF](#6-relatório-e-pdf)
7. [Patinagem / Avanço](#7-patinagem--avanço)
8. [Histórico de Balanços](#8-histórico-de-balanços)
9. [Suporte](#9-suporte)
10. [Gating por Plano (matriz)](#10-gating-por-plano-matriz)
11. [Sincronização, Offline e Segurança](#11-sincronização-offline-e-segurança)

---

## 1. Navegação / Hub

Tela inicial com 6 cards e um badge de plano/saldo.

| UC | Caso de uso | Resultado |
|----|-------------|-----------|
| UC-1.1 | Abrir o app | Hub carrega; badge mostra plano atual e, no Testdrive, "Restam N balanço(s) este mês"; nos pagos, "Calcular distribuição de peso". |
| UC-1.2 | Hub re-sincroniza ao voltar (`onResume`) | Dispara `syncComSupabase`; badge é atualizado após o sync. |
| UC-1.3 | Tocar "Novo Balanço" com saldo disponível | Abre a cadeia de cálculo (`TratorOperacaoActivity`). |
| UC-1.4 | Tocar "Novo Balanço" com limite atingido (Testdrive, 1/mês usado) | Dialog "Limite Atingido" → botão "Ver Planos" (leva a Planos se logado, senão a Login). |
| UC-1.5 | Tocar "Patinagem" | Abre `AvancoRatioActivity` (sem gating — disponível em todos os planos). |
| UC-1.6 | Tocar "Histórico" com plano que permite (Flexível+) | Abre `HistoricoActivity`. |
| UC-1.7 | Tocar "Histórico" no Testdrive | Dialog "Histórico Bloqueado" com upsell → "Ver Planos". |
| UC-1.8 | Tocar "Meu Plano" logado | Abre `PlanosActivity`. |
| UC-1.9 | Tocar "Meu Plano" deslogado | Abre `AuthActivity` (login/cadastro). |
| UC-1.10 | Tocar "Minha Conta" logado | Abre `PerfilActivity`. Subtítulo do card mostra "Editar perfil ou sair". |
| UC-1.11 | Tocar "Minha Conta" deslogado | Abre `AuthActivity`. Subtítulo mostra "Entrar ou cadastrar". |
| UC-1.12 | Tocar "Suporte" | Abre `SuporteActivity` (FAQ sempre; canais conforme plano). |

---

## 2. Conta e Autenticação

`AuthActivity` + `SupabaseClient` (Supabase Auth). Sessão em EncryptedSharedPreferences.

### Fluxos principais
| UC | Caso de uso | Fluxo / Resultado |
|----|-------------|-------------------|
| UC-2.1 | **Cadastro** novo | Modo "Cadastrar" → email + senha → `signUp`. Se Supabase retornar sessão: salva sessão, registra dispositivo, sincroniza plano, vai para Planos. Se exigir confirmação de email: Toast "Verifique seu email para confirmar o cadastro". |
| UC-2.2 | **Login** | Modo "Entrar" → `signIn` → salva sessão (access+refresh+userId) → registra dispositivo → sync → redireciona a Planos. |
| UC-2.3 | Alternar Login ↔ Cadastro | Link "Não tem conta? Cadastre-se" / "Já tem conta? Entre" troca o modo e os textos. |
| UC-2.4 | "Continuar sem conta" | Fecha a tela e volta ao hub como Visitante. |
| UC-2.5 | **Logout** (via Perfil) | Dialog de confirmação → `forcarLogout` → limpa sessão, volta a TESTDRIVE local. |
| UC-2.6 | Sessão persistente | Ao reabrir o app, `restaurarSessao` recarrega tokens do storage; usuário segue logado sem novo login. |
| UC-2.7 | **Refresh automático** de token | Em 401 com refresh token válido, `executeAsyncInternal` renova o access token e refaz a request uma vez; tokens novos são persistidos. |

### Exceções / validações
| UC | Caso | Resultado |
|----|------|-----------|
| UC-2.8 | Email ou senha vazios | Toast "Preencha email e senha". |
| UC-2.9 | Senha < 6 caracteres | Toast "A senha deve ter pelo menos 6 caracteres". |
| UC-2.10 | Credenciais inválidas / email já cadastrado | Toast com a mensagem de erro do backend. |
| UC-2.11 | Sem internet | Toast "Falha na conexão: …". |
| UC-2.12 | **Sessão expirada** (refresh falha) | `onSessionExpired` limpa a sessão; próxima ação pede login novamente ("Sessão expirada. Faça login novamente."). |
| UC-2.13 | **Enforcement 1-dispositivo**: outro aparelho loga na mesma conta | No próximo `syncComSupabase` deste aparelho, `current_device_id` não bate → `forcarLogout` automático local. |

---

## 3. Perfil

`PerfilActivity`. Nome, CPF/CNPJ (Mod 11), telefone. Necessário para assinar.

| UC | Caso de uso | Resultado |
|----|-------------|-----------|
| UC-3.1 | Abrir perfil logado | Carrega `full_name`, `cpf_cnpj`, `phone` do servidor nos campos. |
| UC-3.2 | Salvar perfil válido | `updateProfile` → Toast "Perfil salvo!" → fecha. |
| UC-3.3 | Abrir perfil vindo do fluxo de assinatura | Banner/Toast: "Complete seus dados antes de assinar um plano (exigência do pagamento)." |
| UC-3.4 | Nome vazio | Toast "Preencha o nome". |
| UC-3.5 | CPF/CNPJ vazio | Toast "Preencha o CPF ou CNPJ". |
| UC-3.6 | CPF/CNPJ inválido (falha Mod 11) | Toast "CPF ou CNPJ inválido". |
| UC-3.7 | Telefone vazio | Toast "Preencha o telefone". |
| UC-3.8 | CPF válido (11 díg.) ou CNPJ válido (14 díg.) | `DocumentValidator.isValid` aceita ambos; normaliza dígitos. |
| UC-3.9 | Erro de rede ao salvar | Toast "Erro: …"; permanece na tela. |
| UC-3.10 | Logout pela tela de perfil | Vide UC-2.5. |

---

## 4. Planos e Assinatura

`PlanosActivity` → Edge Function `subscribe` → checkout hospedado Asaas → webhook `webhook-asaas`. Cancelamento via `cancel-subscription`.

### Visualização e assinatura
| UC | Caso de uso | Fluxo / Resultado |
|----|-------------|-------------------|
| UC-4.1 | Ver planos | Lista os 4 planos com preço e features; marca o atual com badge. |
| UC-4.2 | **Assinar** plano pago (logado, perfil completo) | Pré-checa login → valida perfil → `subscribe` cria assinatura `pending` + checkout Asaas → abre `checkout_url` no navegador; inicia polling. |
| UC-4.3 | Pagar no checkout Asaas (cartão) | Asaas captura o cartão; emite `PAYMENT_CONFIRMED`/`PAYMENT_RECEIVED`. |
| UC-4.4 | **Webhook ativa a assinatura** | `webhook-asaas` casa por `checkoutSession`→`asaas_checkout_id` (1º pagamento) ou por `sub_xxx`/`externalReference`; grava `asaas_subscription_id`, define período, insere `payment_history`, status → `active`. |
| UC-4.5 | App reflete plano ativo | Polling (3s) chama `get_active_subscription`; quando ≠ TESTDRIVE, atualiza UI e para o polling com Toast "Plano atualizado: …". |
| UC-4.6 | **Renovação** (cobrança recorrente) | Novo `PAYMENT_*` com `sub_xxx` já conhecido → estende `current_period_end`; idempotente para entrega dupla (CONFIRMED+RECEIVED). |

### Cancelamento
| UC | Caso de uso | Resultado |
|----|-------------|-----------|
| UC-4.7 | **Cancelar** assinatura ativa | Dialog de confirmação → `cancel-subscription` → `DELETE` no Asaas → marca `cancel_requested_at`; **mantém acesso até `current_period_end`** (status segue `active`). Toast "Acesso até DD/MM". |
| UC-4.8 | UI pós-cancelamento | Card mostra badge **"CANCELADA"** e botão desabilitado **"Ativa até DD/MM"** (não oferece cancelar de novo). |
| UC-4.9 | Webhook `SUBSCRIPTION_DELETED`/`INACTIVATED` | Carimba `cancelled_at`; só expira imediatamente se já pendente ou período vencido — senão honra o período pago. |
| UC-4.10 | **Downgrade automático** ao fim do período | Cron `check-expired` vira `active`→`expired` após `current_period_end`; app volta a Testdrive no próximo sync. |
| UC-4.11 | Cancelar assinatura `pending` (nunca paga) | Cancelamento imediato (status → `cancelled`), sem esperar período. |

### Exceções
| UC | Caso | Resultado |
|----|------|-----------|
| UC-4.12 | Assinar **sem login** | Toast "Faça login para assinar" → abre Login. (Guard em `assinarPlano` e re-check em `enviarSubscribe`.) |
| UC-4.13 | Assinar com **perfil incompleto** | Toast "Complete seu perfil…" → abre Perfil com banner. (Pré-check no app + 422 `profile_incomplete` no backend.) |
| UC-4.14 | Já tem assinatura ativa/pending | Backend retorna `already_subscribed` (409). |
| UC-4.15 | **Erro do gateway** (Asaas) | `subscribe` retorna `gateway_error` (502); Toast "Erro: Payment gateway error"; nenhuma página abre. |
| UC-4.16 | Plano sem ciclo configurado | `plan_not_subscribable` (422). |
| UC-4.17 | Rate limit (5 req/60s) | `rate_limited` (429). |
| UC-4.18 | Webhook com token inválido | `webhook-asaas` responde 401; nada é processado. |
| UC-4.19 | Webhook duplicado (mesmo `evt_id`) | Idempotente: responde 200 sem reprocessar. |

---

## 5. Cálculo de Balanço

Cadeia de Activities passando `DadosJardulli` (Parcelable) via Intent:
`TratorOperacao → Rodados → LastroLiquido → PesoFrontal → RelatorioFinal`.

| UC | Etapa | Entradas / Comportamento |
|----|-------|--------------------------|
| UC-5.1 | **Trator e Operação** | Série, modelo, operação, engate e peso do operador. Avança acumulando em `DadosJardulli`. |
| UC-5.2 | **Rodados** | Rodado dianteiro/traseiro, eixos, grupos. |
| UC-5.3 | **Lastro Líquido** | Valor com multiplicador %. Fórmula 1 `(v×m)/75×2` ou Fórmula 2 `(v×m)/40×2` conforme série **e** direção (dianteiro/traseiro) — `usarCalculo1()`. |
| UC-5.4 | **Pesos** | Pesos frontais, traseiros e cabine. |
| UC-5.5 | **Relatório final** | `CalculoUtil` computa peso total, dianteiro, traseiro, divisão % di/tra, peso/CV, peso/kW e consumo de diesel. |
| UC-5.6 | Concluir balanço | Salva no SQLite local e incrementa o contador mensal (`registrarBalanco`). |
| UC-5.7 | **Limite mensal (Testdrive)** | 2ª tentativa no mês é barrada já no hub (UC-1.4). Planos pagos: ilimitado. |
| UC-5.8 | Receber tela sem dados (`DADOS_JARDULLI` nulo) | Activity valida e faz `finish()`; no relatório, Toast "Erro: Dados não recebidos!". |
| UC-5.9 | Voltar no meio da cadeia | Botão "Voltar" encerra a activity atual, preservando o acúmulo anterior. |

---

## 6. Relatório e PDF

`RelatorioFinalActivity` (iText 7).

| UC | Caso de uso | Resultado |
|----|-------------|-----------|
| UC-6.1 | Ver resultados | Mostra todos os cálculos finais na tela. |
| UC-6.2 | **Gerar PDF** (Flexível/Profissional/Frotista) | Cria o PDF (Downloads via MediaStore) e abre/compartilha; Toast "PDF gerado com sucesso!". |
| UC-6.3 | "Gerar PDF" no Testdrive | Botão a 50% de opacidade → dialog "PDF Bloqueado" (upsell). |
| UC-6.4 | Abrir PDF sem leitor instalado | Salva em Downloads e oferece chooser/compartilhar; Toast informativo se nenhum app compatível. |
| UC-6.5 | **Salvar balanço** automaticamente | Grava no SQLite local sempre; espelha na nuvem se logado **e** plano permite histórico (Flexível+). |
| UC-6.6 | Espelhamento na nuvem falha | Não interrompe o fluxo — registro local é a fonte de verdade local. |
| UC-6.7 | Erro ao gerar PDF | Toast "Erro ao gerar PDF: …". |

---

## 7. Patinagem / Avanço

`AvancoRatioActivity`. Calculadora pura, offline, sem gating.

| UC | Caso de uso | Resultado |
|----|-------------|-----------|
| UC-7.1 | Calcular com vel. teórica > real | `Patinagem = (teórica−real)/teórica×100`. Classificação: ✅ 0–8%, ⚠️ 8–15%, ❌ >15%. |
| UC-7.2 | Calcular com vel. real > teórica | Resultado vira **Avanço**: ✅ 0–5%, ⚠️ >5%. |
| UC-7.3 | Campo vazio | Toast "Preencha todos os campos". |
| UC-7.4 | Velocidade teórica ≤ 0 | Toast "Velocidade teórica deve ser maior que zero". |
| UC-7.5 | Valor não numérico | Toast "Valores inválidos". Aceita vírgula decimal (converte `,`→`.`). |

---

## 8. Histórico de Balanços

`HistoricoActivity` (RecyclerView). Local (SQLite) + nuvem (Supabase).

| UC | Caso de uso | Resultado |
|----|-------------|-----------|
| UC-8.1 | Abrir histórico (Flexível+) | Mostra locais imediatamente; se logado, busca nuvem e mescla deduplicando por `data+modelo+peso`. |
| UC-8.2 | Histórico no Testdrive | Bloqueado no hub (UC-1.7). |
| UC-8.3 | Offline / nuvem indisponível | Mostra apenas os locais; falha de nuvem é silenciosa (log). |
| UC-8.4 | Lista vazia | Exibe estado vazio (`txtVazio`). |
| UC-8.5 | **Deletar** balanço | Dialog → remove do SQLite local e, se logado e com `remoteId`, deleta na nuvem (RLS owner-only). |
| UC-8.6 | Deletar: local ok, nuvem falha | Toast "Removido localmente; falha na nuvem". |
| UC-8.7 | Ordenação | Por data desc (ISO-8601 lexicográfico). |

---

## 9. Suporte

`SuporteActivity`. FAQ para todos; canais conforme `tipoSuporte` do plano.

| UC | Caso de uso | Resultado |
|----|-------------|-----------|
| UC-9.1 | Ver FAQ | 6 perguntas expansíveis (toque alterna resposta) — disponível em todos os planos. |
| UC-9.2 | Email (Flexível, Profissional, Frotista) | Botão Email abre `mailto:suporte@jardulli.com.br`. |
| UC-9.3 | WhatsApp (Profissional, Frotista) | Botão WhatsApp abre `wa.me/<número>` — só se `WHATSAPP_SUPORTE` configurado. |
| UC-9.4 | Selo Prioritário (Frotista) | Mostra `txtPrioritario` + canais email e WhatsApp. |
| UC-9.5 | Testdrive | Apenas FAQ (sem email/WhatsApp). |
| UC-9.6 | Sem cliente de email instalado | Toast "Nenhum cliente de email instalado neste dispositivo". |
| UC-9.7 | Sem WhatsApp instalado | Toast "WhatsApp não está instalado neste dispositivo". |

---

## 10. Gating por Plano (matriz)

| Feature | Testdrive | Flexível | Profissional | Frotista |
|---|:--:|:--:|:--:|:--:|
| Balanços/mês | **1** | ∞ | ∞ | ∞ |
| Patinagem | ✅ | ✅ | ✅ | ✅ |
| Lastro líquido | ✅ | ✅ | ✅ | ✅ |
| Histórico (nuvem) | ❌ | ✅ | ✅ | ✅ |
| Gerar PDF | ❌ | ❌ | ✅ | ✅ |
| Suporte | FAQ | + Email | + WhatsApp | + Prioritário |
| Preço | Grátis | R$30/mês | R$150/sem | R$240/ano |

> As permissões vêm do `plan_features` (JSONB) do backend quando disponível,
> com fallback para o enum `Plano`. Isso permite ajustar limites sem rebuild.

---

## 11. Sincronização, Offline e Segurança

| UC | Caso de uso | Comportamento |
|----|-------------|---------------|
| UC-11.1 | **Offline-first** | Cálculos e histórico local funcionam sem internet; só login, sync de plano e pagamento exigem rede. |
| UC-11.2 | Sync no `onResume` do hub | Verifica dispositivo + plano; atualiza badge. |
| UC-11.3 | **Cache de plano (TTL 1h)** | Se passar 1h sem sync bem-sucedido, força Testdrive (reduz janela de uso premium pós-cancelamento). |
| UC-11.4 | Contagem de balanços anti-bypass | `getBalancosMesAtual` usa `max(cloud, local)` para resistir a clear-data / troca de aparelho. |
| UC-11.5 | Features críticas (PDF/Histórico) | Devem chamar `syncComSupabase` antes do uso para refletir cancelamentos. |
| UC-11.6 | **Storage seguro** | Tokens e plano em EncryptedSharedPreferences (AES256-GCM); excluídos de backup/transfer. Fallback para SharedPreferences se o KeyStore corromper. |
| UC-11.7 | KeyStore corrompido | App não quebra: cai no fallback de prefs em texto. |
| UC-11.8 | Enforcement 1-dispositivo | `current_device_id` no perfil; mismatch → logout automático (UC-2.13). |
| UC-11.9 | Webhook sem JWT | Função `webhook-asaas` publicada com `verify_jwt=false`; validação por `asaas-access-token` (timing-safe). |
| UC-11.10 | RLS (Row Level Security) | Toda leitura/escrita de assinatura, perfil e histórico é owner-only no Supabase. |

---

## Resumo de cobertura

| Módulo | Casos de uso |
|---|---|
| Hub / Navegação | 12 |
| Conta e Autenticação | 13 |
| Perfil | 10 |
| Planos e Assinatura | 19 |
| Cálculo de Balanço | 9 |
| Relatório e PDF | 7 |
| Patinagem / Avanço | 5 |
| Histórico | 7 |
| Suporte | 7 |
| Sincronização / Segurança | 10 |
| **Total** | **99** |
