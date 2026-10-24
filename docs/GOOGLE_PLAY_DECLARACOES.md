# Google Play — riscos críticos e altos das três declarações

> **Data da análise:** 24/out/2026. Baseada no código do app (branch `main`) e em páginas oficiais da Google consultadas nesta data (links no fim). As regras da Google mudam; **confirmar no Play Console antes de enviar**. Este documento não é parecer jurídico.
>
> Escopo: os três itens da seção **Declarações** do Play Console (Políticas do programa para desenvolvedores · Assinatura de apps do Google Play · Leis de exportação dos EUA). Só riscos **críticos** e **altos**; o que for médio ou baixo não está aqui.

Legenda: 🔴 crítico (bloqueia a publicação ou pode derrubar o app/conta) · 🟠 alto (tem de ser tratado antes do envio) · ✅ verificado em fonte oficial · 🔍 a confirmar

## Resumo

| # | Declaração | Risco | Nível | Situação |
|---|---|---|---|---|
| C1 | Políticas do programa | Assinatura paga **fora do Google Play Billing** (checkout Asaas no navegador) | 🔴 | ✅ regra confirmada · decisão pendente |
| C2 | Políticas do programa | App com `targetSdk = 35`; novos apps precisam de **API 36** desde 31/ago/2026 | 🔴 | ✅ regra confirmada · correção pendente |
| A1 | Políticas do programa | Dados pessoais (CPF/CNPJ, telefone, aparelho) e coerência com *Segurança dos dados*, privacidade e exclusão de conta | 🟠 | em andamento (P5, P16, P17) |
| A2 | Políticas do programa | Nome **"John Deere"** usado no app e no site (marca de terceiro) | 🟠 | 🔍 |
| A3 | Políticas do programa | Tipo de conta de desenvolvedor: exigência de teste fechado (12 testadores, 14 dias) | 🟠 | ✅ regra · 🔍 qual é a sua conta |
| S1 | Assinatura de apps | Keystore de upload: perda, senha parcialmente exposta, backup | 🟠 | pendente |
| S2 | Assinatura de apps | Termos aceitos em nome da empresa; só AAB; nome do pacote definitivo | 🟠 | decisão do responsável |
| E1 | Leis de exportação dos EUA | Certificação legal sem classificação de criptografia feita | 🟠 | pendente (responsável legal) |
| E2 | Leis de exportação dos EUA | Mudança futura de criptografia invalida a declaração | 🟠 | processo |

**Não há risco crítico no item 2 nem no item 3.** Os dois críticos estão no item 1, e **C2 tem prazo**: ver abaixo.

---

## 1. Políticas do programa para desenvolvedores

Marcar essa caixa afirma que o app cumpre **todas** as políticas. Se uma regra for violada, o app pode ser recusado no envio ou removido depois, e a conta fica em risco.

### 🔴 C1 — Pagamento de assinatura fora do Google Play Billing

**O que o app faz hoje.** Em `PlanosActivity.java` (por volta da linha 229), o botão de assinar pede um link de pagamento ao servidor e o abre no navegador (`new Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl))`). O pagamento é feito no checkout hospedado do **Asaas**. O app não usa a biblioteca Google Play Billing (nenhuma dependência `billing` em `app/build.gradle.kts`). O plano pago libera funções do próprio app (histórico, PDF, balanços ilimitados).

**O que a política diz** ✅ (Política de Pagamentos, seção 2): apps que exigem ou aceitam pagamento por *itens no app, assinaturas, funcionalidade do app ou serviços em nuvem* devem usar o sistema de cobrança da Google Play. As exceções são: bens e serviços **físicos** (seção 3), cobrança alternativa dentro do app (seção 8) e links de pagamento fora do app (seção 9). As seções 8 e 9 só valem para quem **se inscreve no programa** e aceita os termos. O Brasil está entre os países em que a Google oferece alternativas de cobrança, mas a participação é uma escolha do desenvolvedor.

**Consequência.** Rejeição no envio ou remoção do app depois da publicação, e advertência na conta. A página da política não detalha as penalidades; ver a seção de *Enforcement* da Política do programa.

**Caminhos possíveis (decisão do responsável, não do código):**

| Opção | Resumo | Custo / risco |
|---|---|---|
| A. Google Play Billing | Vender as assinaturas pela Play. O servidor passa a validar compras da Play (nova Edge Function, novos estados de assinatura, reembolso e cancelamento pela Play). | Trabalho grande no app e no backend; a Google cobra a taxa de serviço dela. Cobre o app inteiro com a política. |
| B. Programa de cobrança alternativa / links externos (Brasil) | Inscrever-se no programa da Google para manter o Asaas. | Exige inscrição no Console, relatar cada transação externa à Google e aceitar a taxa do programa. 🔍 ler os termos atuais do programa antes de decidir. |
| C. App sem compra | O app apenas entra em contas já assinadas pelo site, sem botão de assinar, sem preço e sem link de pagamento dentro do app. | Reduz a exposição, mas qualquer texto que **leve o usuário a pagar fora** ainda conta como violação sem a inscrição da opção B. Perde conversão. |

**Antes de publicar:** escolher A, B ou C. Hoje o app **não está conforme** com a política.

### 🔴 C2 — `targetSdk` precisa ser 36

**Evidência.** `app/build.gradle.kts`: `compileSdk = 35`, `targetSdk = 35`; AGP 8.9.3.

**Regra** ✅ (developer.android.com, *Target API level requirements*): a partir de **31/ago/2026**, apps novos e atualizações precisam ter como alvo o **Android 16 (API 36)** ou superior para serem enviados. Apps que não cumprem **não podem ser enviados**. A Google permite pedir **extensão até 1º/nov/2026** (formulário no Play Console).

**Impacto.** Hoje é 24/out/2026: o primeiro AAB com `targetSdk = 35` será recusado no envio. A extensão, se existir no Console, vence em poucos dias.

**O que fazer:** subir `compileSdk` e `targetSdk` para 36, verificar se o AGP 8.9.3 aceita compilar com 36 (senão atualizar), e testar em aparelho e emulador Android 16. 🔍 Pontos a conferir no teste: no Android 16, apps com alvo 36 podem ter o travamento de orientação ignorado em telas grandes (tablets, dobráveis), e as telas do app estão fixas em retrato (`android:screenOrientation="portrait"` no manifesto).

### 🟠 A1 — Dados pessoais e coerência das declarações

**O que o app coleta e envia** (código e docs): e-mail e senha (Supabase Auth); nome, CPF/CNPJ e telefone (`PerfilActivity`); identificador do aparelho para o limite de um aparelho por conta (`current_device_id`, `PlanoManager.getDeviceId`); balanços calculados e histórico; dados da assinatura e pagamentos (processados pelo Asaas). Não há SDKs de anúncios, analytics ou crash reporting no `build.gradle.kts` nem no `libs.versions.toml`.

**Riscos.**
- O formulário **Segurança dos dados** do Console, a **Política de Privacidade** e o comportamento real do app precisam dizer a mesma coisa. Divergência é motivo comum de rejeição e remoção.
- **Exclusão de conta** é obrigatória: tem de existir dentro do app **e** uma URL pública. Já feito (P16: botão no Perfil, `ExclusaoContaTest`, `https://jardullimaquinas.com.br/excluir-conta`); falta confirmar em aparelho.
- A política de privacidade foi redigida sem revisão jurídica (P17) e a exclusão não cobre `webhook_events.payload` (P19).

**Antes de publicar:** preencher *Segurança dos dados* a partir da lista acima (incluindo CPF/CNPJ e o identificador do aparelho), usar `https://jardullimaquinas.com.br/privacidade` como URL de privacidade e conferir que a exclusão funciona de ponta a ponta.

### 🟠 A2 — Marca "John Deere"

**Evidência.** O código usa "John Deere 8R" como nome de série (`LastroLiquidoActivity.java` linha 230 e comentários em `DadosJardulli.java`) e o site cita a John Deere.

**Risco.** As políticas de Propriedade Intelectual e Falsa Identidade da Play proíbem sugerir vínculo com uma marca sem autorização. Usar o nome apenas para **identificar o modelo do trator** costuma ser aceitável, mas depende da forma como aparece na ficha da loja.

**Mitigação (🔍 validar com o responsável legal):** não usar logotipo, cores nem a marca no nome do app, no ícone ou nas capturas de tela em destaque; incluir, na descrição, aviso do tipo "Marcas de terceiros citadas apenas para identificar modelos de máquinas; sem vínculo com os fabricantes"; guardar autorização por escrito se a Jardulli tiver relação comercial com a fabricante.

### 🟠 A3 — Tipo de conta de desenvolvedor

**Regra** ✅ (Ajuda do Play Console): contas **pessoais** criadas depois de 13/nov/2023 precisam rodar um **teste fechado com pelo menos 12 testadores por 14 dias seguidos** antes de pedir acesso à produção. Guias de terceiros dizem que contas de **organização** estão dispensadas (🔍 confirmar na página oficial).

**Risco.** Se a conta for pessoal, a publicação atrasa pelo menos 14 dias, e guias de terceiros dizem que cair abaixo de 12 testadores no meio pode reiniciar a contagem (🔍 confirmar).

**Antes de publicar:** conferir o tipo de conta. Se for pessoal e a Agrosinergia quiser a conta de organização, isso exige CNPJ e verificação (leva tempo); decidir já.

---

## 2. Assinatura de apps do Google Play

Aceitar os Termos de Serviço permite publicar **Android App Bundles** e usar o *Play App Signing*: a Google guarda a chave que assina o app para os usuários, e você assina o envio com uma **chave de upload**.

### 🟠 S1 — Keystore de upload (perda, senha, backup)

- A chave de upload é o `STORE_FILE` do `local.properties`. Se for perdida, ✅ é possível pedir **reset da chave de upload** no Console, o que atrasa a próxima atualização; sem Play App Signing seria irrecuperável.
- Parte da senha do keystore foi exibida por engano numa sessão de trabalho (não foi gravada em arquivo versionado). **Trocar a senha** com `keytool -storepasswd` e `-keypasswd` antes do primeiro AAB de upload, e atualizar o `local.properties`.
- Fazer **backup** do keystore e das senhas em local separado do repositório (ver `BACKUP.md`, que lista só nomes e caminhos).
- No Console, deixar a **Google gerar** a chave de assinatura do app (padrão) e usar o seu keystore só como chave de upload.

### 🟠 S2 — Termos aceitos em nome da empresa; AAB; pacote definitivo

- Aceitar os termos é um ato **contratual**: deve ser feito por quem representa a empresa titular da conta.
- Só se publica **AAB** (`./gradlew bundleRelease`), não APK. O `applicationId` do AAB precisa ser igual ao da ficha do app: `br.com.agrosinergia.jardulli`.
- Esse nome de pacote é **definitivo** depois de publicado.
- 🔍 Não encontrei afirmação oficial clara se optar pelo Play App Signing pode ser desfeito depois. Tratar como **irreversível** e conferir a ajuda do Console.

---

## 3. Leis de exportação dos EUA

Marcar a caixa é uma **certificação legal**: afirma que o app pode ser exportado dos EUA e que as leis (incluindo regras de software com criptografia) foram cumpridas, **independentemente de onde a empresa está**.

### 🟠 E1 — Certificação sem classificação feita

**Inventário de criptografia no app (do código):**
- Comunicação **HTTPS/TLS** com Supabase e Asaas pela biblioteca OkHttp 4.12 (criptografia padrão do sistema).
- **EncryptedSharedPreferences** (AES-256-GCM, chave no Android Keystore), usada para guardar sessão e plano (`PlanoManager.java`).
- Nenhum algoritmo próprio e nenhuma criptografia de conteúdo de terceiros.

**Risco.** Ninguém no projeto classificou formalmente o app (categoria de criptografia, possível classificação 5D992 de mercado de massa, necessidade de relatório anual de autoclassificação ao BIS). Certificar sem isso expõe a empresa a declaração incorreta. O uso apenas de HTTPS e de armazenamento local cifrado costuma estar nas categorias mais simples, mas 🔍 **isso é uma conclusão a ser confirmada por quem responde legalmente pela Agrosinergia**, com base nas páginas do BIS.

**Ação.** O responsável legal confirma a classificação e registra a decisão (data, quem, base) num arquivo do repositório. Usar o inventário acima como insumo.

### 🟠 E2 — Mudanças futuras

A declaração vale para o app como um todo. Se uma versão futura adicionar criptografia própria (cifrar arquivos, biblioteca de cripto nova, protocolo diferente de HTTPS), **reavaliar** antes de publicar. Incluir "mudou algo de criptografia?" no checklist de release.

---

## Ordem sugerida

1. **C2** (prazo curto): subir `targetSdk` e `compileSdk` para 36 e testar; pedir extensão no Console se ainda estiver disponível e o prazo apertar.
2. **C1**: decidir A, B ou C. Sem isso o app não deve ser enviado para produção.
3. **S1**: trocar a senha do keystore e fazer o backup.
4. **A3**: conferir o tipo de conta e iniciar o teste fechado, se for o caso.
5. **A1**: preencher *Segurança dos dados* e validar exclusão de conta no aparelho.
6. **A2, E1, S2**: validação do responsável legal (marca, exportação, termos).

## O que foi verificado

- ✅ [Política de Pagamentos do Google Play](https://support.google.com/googleplay/android-developer/answer/9858738) (seções 2, 3, 8 e 9, lidas em 24/out/2026).
- ✅ [Alternativas de cobrança no Google Play](https://support.google.com/googleplay/answer/11174377): o Brasil está entre os países elegíveis; a participação é opcional.
- ✅ [Requisitos de API alvo](https://developer.android.com/google/play/requirements/target-sdk): API 36 desde 31/ago/2026, extensão até 1º/nov/2026.
- ✅ [Requisitos de teste para contas pessoais novas](https://support.google.com/googleplay/android-developer/answer/14151465): 12 testadores, 14 dias.
- 🔍 Criptografia e exportação: [BIS, produtos de mercado de massa](https://www.bis.gov/learn-support/encryption-controls/mass-market) e a [ajuda da Google sobre leis de exportação](https://support.google.com/googleplay/android-developer/answer/113770). Não encontrei o texto exato do questionário atual do Console.
- 🔍 Pontos marcados com lupa acima dependem de confirmação no Console ou do responsável legal.
