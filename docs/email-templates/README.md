# Templates de e-mail do Supabase Auth

Cópia de referência dos e-mails enviados pelo Supabase Auth. **O template em uso mora no painel do Supabase**; estes arquivos são o texto versionado para colar lá.

Projeto: `Jardulli Subscriptions` (`ljeroorpbqnlggiyyttb`)
Onde colar: painel → **Authentication → Email Templates** → escolher o template → preencher **Subject** e **Body (HTML)** → *Save*. A aba **Preview** mostra o resultado.

O template é **um só por tipo** e vale para o app e para o site (os dois usam o mesmo Supabase Auth).

| Template no painel | Subject | Body | Quando é enviado |
|---|---|---|---|
| **Confirm signup** | `Confirme seu cadastro no Jardulli` | [`confirmar-cadastro.html`](confirmar-cadastro.html) | Cadastro novo (a opção *Confirm email* está **ligada** em produção) |
| **Reset Password** | `Seu código para redefinir a senha` | [`redefinir-senha.html`](redefinir-senha.html) | Só depois que a recuperação de senha existir no app (P10) |

## Variáveis usadas

- `{{ .ConfirmationURL }}` — link de confirmação (cadastro).
- `{{ .Token }}` — código de 6 dígitos (redefinição de senha, digitado no app).
- `{{ .Email }}` — e-mail da conta.

## Observações

- **Estilo:** tabela + CSS inline de propósito (é o que Gmail, Outlook e celular renderizam de forma consistente). Verde `#2E7D32` = `green_700` do app.
- **Remetente:** continua sendo o padrão do Supabase. Mudar nome/endereço de quem envia exige SMTP próprio (P11 no [`PENDENCIAS.md`](../PENDENCIAS.md)).
- **Validade do código** (reset): o texto diz "1 hora", que é o padrão do Supabase. Se o tempo for alterado no painel, ajustar a frase.
- **Logo:** não incluído. E-mail não carrega imagem do app; precisaria de uma imagem em endereço público (por exemplo, no site).
- **Testar depois de salvar:** fazer um cadastro real e abrir o e-mail no celular e no webmail.
- Mudou o texto no painel? Atualize o arquivo aqui também.
