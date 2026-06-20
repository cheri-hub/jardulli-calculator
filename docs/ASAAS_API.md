# Guia da API Asaas

Documentação completa da API de pagamentos Asaas e guia de implementação em aplicações.

---

## Sumário

1. [Ambientes e URLs](#ambientes-e-urls)
2. [Autenticação](#autenticação)
3. [Clientes](#clientes)
4. [Cobranças (Payments)](#cobranças-payments)
   - [Boleto Bancário](#boleto-bancário)
   - [PIX](#pix)
   - [Cartão de Crédito](#cartão-de-crédito)
5. [Parcelamento e Carnê](#parcelamento-e-carnê)
6. [Assinaturas (Subscriptions)](#assinaturas-subscriptions)
7. [Links de Pagamento](#links-de-pagamento)
8. [Transferências](#transferências)
9. [Webhooks](#webhooks)
10. [Sandbox e Testes](#sandbox-e-testes)
11. [Principais Endpoints](#principais-endpoints)
12. [Implementação em Android (Java)](#implementação-em-android-java)

---

## Ambientes e URLs

| Ambiente   | Base URL                              | Prefixo da API Key     |
|------------|---------------------------------------|------------------------|
| Produção   | `https://api.asaas.com/v3`            | `$aact_prod_`          |
| Sandbox    | `https://api-sandbox.asaas.com/v3`    | `$aact_hmlg_`          |

O sandbox é um ambiente de testes completo. Crie uma conta sandbox em [sandbox.asaas.com](https://sandbox.asaas.com).

---

## Autenticação

Todas as requisições devem incluir os seguintes headers:

```
Content-Type: application/json
User-Agent: nome_da_sua_aplicacao
access_token: sua_api_key_aqui
```

A API Key é obtida no painel Asaas em **Configurações > Integrações > API**.

### Exemplo de requisição (cURL)

```bash
curl -X GET "https://api.asaas.com/v3/customers" \
  -H "Content-Type: application/json" \
  -H "User-Agent: MeuApp" \
  -H "access_token: $aact_prod_XXXXXXXX"
```

---

## Clientes

Antes de criar cobranças, é necessário cadastrar o cliente (pagador).

### Criar cliente

```
POST /v3/customers
```

**Campos obrigatórios:**

| Campo     | Tipo   | Descrição           |
|-----------|--------|---------------------|
| `name`    | string | Nome completo       |
| `cpfCnpj` | string | CPF ou CNPJ         |

**Campos opcionais:**

| Campo                    | Tipo    | Descrição                        |
|--------------------------|---------|----------------------------------|
| `email`                  | string  | Email do cliente                 |
| `phone`                  | string  | Telefone fixo                    |
| `mobilePhone`            | string  | Celular                          |
| `address`                | string  | Logradouro                       |
| `addressNumber`          | string  | Número                           |
| `complement`             | string  | Complemento                      |
| `province`               | string  | Bairro                           |
| `postalCode`             | string  | CEP                              |
| `externalReference`      | string  | ID no seu sistema                |
| `notificationDisabled`   | boolean | Desativar notificações do Asaas  |
| `additionalEmails`       | string  | Emails adicionais (vírgula)      |
| `observations`           | string  | Observações internas             |
| `groupName`              | string  | Grupo do cliente                 |
| `company`                | string  | Empresa                          |
| `foreignCustomer`        | boolean | Cliente estrangeiro              |

### Exemplo

```json
POST /v3/customers
{
  "name": "João da Silva",
  "cpfCnpj": "12345678901",
  "email": "joao@email.com",
  "mobilePhone": "11999999999",
  "postalCode": "01001000"
}
```

**Resposta:**
```json
{
  "id": "cus_XXXXXXXX",
  "name": "João da Silva",
  "cpfCnpj": "12345678901",
  "email": "joao@email.com",
  ...
}
```

### Outros endpoints de clientes

| Método | Endpoint               | Descrição              |
|--------|------------------------|------------------------|
| GET    | `/v3/customers`        | Listar clientes        |
| GET    | `/v3/customers/{id}`   | Buscar por ID          |
| PUT    | `/v3/customers/{id}`   | Atualizar cliente      |
| DELETE | `/v3/customers/{id}`   | Remover cliente        |

---

## Cobranças (Payments)

### Criar cobrança

```
POST /v3/payments
```

**Campos obrigatórios:**

| Campo         | Tipo   | Descrição                                          |
|---------------|--------|----------------------------------------------------|
| `customer`    | string | ID do cliente (`cus_XXXXXXXX`)                     |
| `billingType` | string | Tipo: `BOLETO`, `CREDIT_CARD`, `PIX`, `UNDEFINED`  |
| `value`       | number | Valor da cobrança                                  |
| `dueDate`     | string | Data de vencimento (`YYYY-MM-DD`)                  |

**Campos opcionais comuns:**

| Campo               | Tipo   | Descrição                                |
|---------------------|--------|------------------------------------------|
| `description`       | string | Descrição visível ao cliente             |
| `externalReference`  | string | ID no seu sistema                        |
| `installmentCount`   | number | Número de parcelas                       |
| `installmentValue`   | number | Valor de cada parcela                    |
| `totalValue`         | number | Valor total (alternativa a value)        |
| `postalService`      | boolean | Enviar boleto via correios              |
| `split`             | array  | Divisão entre contas                     |
| `callback`          | object | URLs de sucesso/cancelamento             |

**Status possíveis da cobrança:**

| Status               | Descrição                              |
|----------------------|----------------------------------------|
| `PENDING`            | Aguardando pagamento                   |
| `RECEIVED`           | Recebida (confirmação manual)          |
| `CONFIRMED`          | Pagamento confirmado                   |
| `OVERDUE`            | Vencida                                |
| `REFUNDED`           | Estornada                              |
| `RECEIVED_IN_CASH`   | Recebida em dinheiro                   |
| `REFUND_REQUESTED`   | Estorno solicitado                     |
| `REFUND_IN_PROGRESS` | Estorno em processamento               |
| `CHARGEBACK_REQUESTED` | Chargeback solicitado               |
| `CHARGEBACK_DISPUTE` | Disputa de chargeback                  |
| `AWAITING_CHARGEBACK_REVERSAL` | Aguardando reversão       |
| `DUNNING_REQUESTED`  | Recuperação solicitada                 |
| `DUNNING_RECEIVED`   | Recuperação recebida                   |
| `AWAITING_RISK_ANALYSIS` | Aguardando análise de risco        |

### Resposta da cobrança

```json
{
  "id": "pay_XXXXXXXX",
  "status": "PENDING",
  "customer": "cus_XXXXXXXX",
  "billingType": "PIX",
  "value": 100.00,
  "netValue": 99.01,
  "dueDate": "2026-07-01",
  "invoiceUrl": "https://www.asaas.com/i/XXXXXXXX",
  "bankSlipUrl": "https://www.asaas.com/b/pdf/XXXXXXXX",
  "creditDate": "2026-07-02",
  "nossoNumero": "123456",
  "discount": { ... },
  "fine": { ... },
  "interest": { ... },
  "split": [ ... ],
  "chargeback": null,
  "refunds": []
}
```

---

### Boleto Bancário

```json
POST /v3/payments
{
  "customer": "cus_XXXXXXXX",
  "billingType": "BOLETO",
  "value": 150.00,
  "dueDate": "2026-07-15",
  "description": "Mensalidade Julho"
}
```

**Obter linha digitável:**

```
GET /v3/payments/{id}/identificationField
```

Resposta:
```json
{
  "identificationField": "23793.38128 60000.000003 00000.000400 1 88780000015000",
  "nossoNumero": "1234567",
  "barCode": "23791887800000150003381286000000000000000040"
}
```

**Descontos, juros e multa:**

```json
{
  "customer": "cus_XXXXXXXX",
  "billingType": "BOLETO",
  "value": 200.00,
  "dueDate": "2026-07-15",
  "discount": {
    "value": 10,
    "dueDateLimitDays": 5,
    "type": "PERCENTAGE"
  },
  "interest": {
    "value": 2
  },
  "fine": {
    "value": 5,
    "type": "PERCENTAGE"
  }
}
```

---

### PIX

```json
POST /v3/payments
{
  "customer": "cus_XXXXXXXX",
  "billingType": "PIX",
  "value": 75.50,
  "dueDate": "2026-07-10"
}
```

**Obter QR Code PIX:**

```
GET /v3/payments/{id}/pixQrCode
```

Resposta:
```json
{
  "encodedImage": "iVBORw0KGgoAAAANSUhEUg...",
  "payload": "00020126580014br.gov.bcb.pix...",
  "expirationDate": "2026-07-10 23:59:59"
}
```

| Campo           | Descrição                                           |
|-----------------|-----------------------------------------------------|
| `encodedImage`  | Imagem do QR Code em Base64 (PNG)                   |
| `payload`       | Código PIX Copia e Cola                             |
| `expirationDate`| Data/hora de expiração                              |

Para exibir no Android, decodifique o `encodedImage` de Base64 para `Bitmap`:

```java
byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
Bitmap qrBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
imageView.setImageBitmap(qrBitmap);
```

---

### Cartão de Crédito

**Opção 1: Dados do cartão inline**

```json
POST /v3/payments
{
  "customer": "cus_XXXXXXXX",
  "billingType": "CREDIT_CARD",
  "value": 199.90,
  "dueDate": "2026-07-01",
  "creditCard": {
    "holderName": "João da Silva",
    "number": "4111111111111111",
    "expiryMonth": "12",
    "expiryYear": "2028",
    "ccv": "123"
  },
  "creditCardHolderInfo": {
    "name": "João da Silva",
    "email": "joao@email.com",
    "cpfCnpj": "12345678901",
    "postalCode": "01001000",
    "addressNumber": "123",
    "phone": "11999999999"
  }
}
```

**Opção 2: Token do cartão (recomendado para compras recorrentes)**

Primeiro, tokenize o cartão:

```json
POST /v3/creditCard/tokenize
{
  "customer": "cus_XXXXXXXX",
  "creditCard": {
    "holderName": "João da Silva",
    "number": "4111111111111111",
    "expiryMonth": "12",
    "expiryYear": "2028",
    "ccv": "123"
  },
  "creditCardHolderInfo": {
    "name": "João da Silva",
    "email": "joao@email.com",
    "cpfCnpj": "12345678901",
    "postalCode": "01001000",
    "addressNumber": "123",
    "phone": "11999999999"
  }
}
```

Depois use o token:

```json
POST /v3/payments
{
  "customer": "cus_XXXXXXXX",
  "billingType": "CREDIT_CARD",
  "value": 199.90,
  "dueDate": "2026-07-01",
  "creditCardToken": "token_retornado"
}
```

**Parcelamento no cartão:** até 21x (Visa/Master). Use `installmentCount` e `installmentValue` ou `totalValue`.

---

## Parcelamento e Carnê

```json
POST /v3/payments
{
  "customer": "cus_XXXXXXXX",
  "billingType": "BOLETO",
  "value": 100.00,
  "dueDate": "2026-07-01",
  "installmentCount": 6,
  "installmentValue": 100.00
}
```

Isso cria 6 cobranças de R$ 100,00 cada (carnê). A primeira vence em `dueDate`, as demais nos meses seguintes.

Para cartão de crédito, o parcelamento funciona da mesma forma usando `installmentCount` e `totalValue`.

---

## Assinaturas (Subscriptions)

Cobranças recorrentes automáticas.

### Criar assinatura

```
POST /v3/subscriptions
```

```json
{
  "customer": "cus_XXXXXXXX",
  "billingType": "BOLETO",
  "value": 49.90,
  "nextDueDate": "2026-08-01",
  "cycle": "MONTHLY",
  "description": "Plano Mensal"
}
```

**Ciclos disponíveis:**

| Ciclo          | Descrição      |
|----------------|----------------|
| `WEEKLY`       | Semanal        |
| `BIWEEKLY`     | Quinzenal      |
| `MONTHLY`      | Mensal         |
| `QUARTERLY`    | Trimestral     |
| `SEMIANNUALLY` | Semestral      |
| `YEARLY`       | Anual          |

**Diferença de assinatura vs parcelamento:**
- **Assinatura:** cobra automaticamente por tempo indeterminado até cancelamento
- **Parcelamento:** número fixo de cobranças (carnê)

As cobranças da assinatura são criadas automaticamente com até 40 dias de antecedência.

### Endpoints de assinaturas

| Método | Endpoint                        | Descrição                    |
|--------|---------------------------------|------------------------------|
| GET    | `/v3/subscriptions`             | Listar assinaturas           |
| GET    | `/v3/subscriptions/{id}`        | Buscar por ID                |
| PUT    | `/v3/subscriptions/{id}`        | Atualizar assinatura         |
| DELETE | `/v3/subscriptions/{id}`        | Cancelar assinatura          |
| GET    | `/v3/subscriptions/{id}/payments` | Cobranças da assinatura    |

---

## Links de Pagamento

Links para pagamento sem precisar cadastrar cliente previamente.

```json
POST /v3/paymentLinks
{
  "name": "Produto X",
  "billingType": "UNDEFINED",
  "chargeType": "DETACHED",
  "value": 99.90,
  "description": "Descrição do produto"
}
```

| Campo        | Descrição                                                |
|--------------|----------------------------------------------------------|
| `billingType`| `UNDEFINED` permite que o cliente escolha a forma de pagamento |
| `chargeType` | `DETACHED` (avulso), `RECURRENT` (recorrente), `INSTALLMENT` (parcelado) |

A resposta inclui a `url` do link de pagamento para compartilhar com o cliente.

---

## Transferências

### Transferência via PIX

```json
POST /v3/transfers
{
  "value": 500.00,
  "pixAddressKey": "chave_pix_destino",
  "pixAddressKeyType": "CPF",
  "description": "Pagamento fornecedor"
}
```

### Transferência via conta bancária

```json
POST /v3/transfers
{
  "value": 500.00,
  "bankAccount": {
    "bank": {
      "code": "341"
    },
    "accountName": "Empresa XYZ",
    "ownerName": "Empresa XYZ LTDA",
    "cpfCnpj": "12345678000199",
    "agency": "1234",
    "account": "56789",
    "accountDigit": "0",
    "bankAccountType": "CONTA_CORRENTE"
  }
}
```

**Tipos de chave PIX:** `CPF`, `CNPJ`, `EMAIL`, `PHONE`, `EVP` (chave aleatória).

---

## Webhooks

Webhooks permitem receber notificações automáticas sobre eventos nas cobranças.

### Configuração

Configure a URL do webhook no painel Asaas ou via API:

```json
POST /v3/webhook
{
  "url": "https://seu-servidor.com/webhook/asaas",
  "email": "admin@suaempresa.com",
  "enabled": true,
  "interrupted": false,
  "apiVersion": 3
}
```

### Estrutura do evento

```json
{
  "id": "evt_XXXXXXXX",
  "event": "PAYMENT_RECEIVED",
  "dateCreated": "2026-07-01 10:30:00",
  "payment": {
    "id": "pay_XXXXXXXX",
    "customer": "cus_XXXXXXXX",
    "value": 100.00,
    "netValue": 99.01,
    "billingType": "PIX",
    "status": "RECEIVED",
    "confirmedDate": "2026-07-01",
    "paymentDate": "2026-07-01",
    ...
  }
}
```

### Principais eventos

| Evento                       | Descrição                           |
|------------------------------|-------------------------------------|
| `PAYMENT_CREATED`            | Cobrança criada                     |
| `PAYMENT_AWAITING_RISK_ANALYSIS` | Aguardando análise de risco    |
| `PAYMENT_APPROVED_BY_RISK_ANALYSIS` | Aprovada pela análise       |
| `PAYMENT_REPROVED_BY_RISK_ANALYSIS` | Reprovada pela análise      |
| `PAYMENT_UPDATED`            | Cobrança atualizada                 |
| `PAYMENT_CONFIRMED`          | Pagamento confirmado                |
| `PAYMENT_RECEIVED`           | Pagamento recebido                  |
| `PAYMENT_OVERDUE`            | Cobrança vencida                    |
| `PAYMENT_DELETED`            | Cobrança removida                   |
| `PAYMENT_RESTORED`           | Cobrança restaurada                 |
| `PAYMENT_REFUNDED`           | Cobrança estornada                  |
| `PAYMENT_RECEIVED_IN_CASH_UNDONE` | Recebimento em dinheiro desfeito |
| `PAYMENT_CHARGEBACK_REQUESTED` | Chargeback solicitado            |
| `PAYMENT_CHARGEBACK_DISPUTE` | Disputa de chargeback               |
| `PAYMENT_AWAITING_CHARGEBACK_REVERSAL` | Aguardando reversão     |
| `PAYMENT_DUNNING_RECEIVED`   | Recuperação recebida                |
| `PAYMENT_DUNNING_REQUESTED`  | Recuperação solicitada              |

### Segurança dos webhooks

O Asaas envia o header `asaas-access-token` com sua API Key para validação:

```java
// Validar webhook no seu backend
String receivedToken = request.getHeader("asaas-access-token");
if (!receivedToken.equals(SUA_API_KEY)) {
    response.setStatus(401);
    return;
}
```

**Regras importantes:**
- Retorne HTTP `200` para confirmar recebimento
- Após **15 falhas consecutivas**, o webhook é pausado
- Eventos ficam disponíveis por **14 dias** na fila de reprocessamento

### Exemplo de endpoint webhook (Java / Spring Boot)

```java
@RestController
@RequestMapping("/webhook")
public class AsaasWebhookController {

    @PostMapping("/asaas")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("asaas-access-token") String token,
            @RequestBody Map<String, Object> payload) {

        // Validar token
        if (!token.equals(System.getenv("ASAAS_API_KEY"))) {
            return ResponseEntity.status(401).build();
        }

        String event = (String) payload.get("event");
        Map<String, Object> payment = (Map<String, Object>) payload.get("payment");

        switch (event) {
            case "PAYMENT_RECEIVED":
            case "PAYMENT_CONFIRMED":
                // Ativar serviço/produto para o cliente
                activateService(payment);
                break;
            case "PAYMENT_OVERDUE":
                // Notificar cliente sobre atraso
                notifyOverdue(payment);
                break;
            case "PAYMENT_REFUNDED":
                // Processar estorno
                processRefund(payment);
                break;
        }

        return ResponseEntity.ok().build();
    }
}
```

---

## Sandbox e Testes

### Endpoints de teste

No ambiente sandbox, use endpoints especiais para simular pagamentos:

| Endpoint                                     | Descrição                    |
|----------------------------------------------|------------------------------|
| `POST /v3/sandbox/payment/{id}/confirm`      | Confirmar pagamento          |
| `POST /v3/sandbox/payment/{id}/receive`      | Receber pagamento            |
| `POST /v3/sandbox/payment/{id}/receiveInCash`| Receber em dinheiro          |
| `POST /v3/sandbox/payment/{id}/overdue`      | Marcar como vencido          |
| `POST /v3/sandbox/payment/{id}/refund`       | Estornar pagamento           |

### Cartões de teste

Use o número `4111 1111 1111 1111` (Visa) com qualquer CVV e data futura.

### Fluxo de teste recomendado

1. Criar conta sandbox em `sandbox.asaas.com`
2. Obter API Key (prefixo `$aact_hmlg_`)
3. Criar cliente de teste
4. Criar cobrança PIX/Boleto/Cartão
5. Usar endpoints de sandbox para simular pagamento
6. Verificar webhook recebido
7. Migrar para produção trocando URL base e API Key

---

## Principais Endpoints

### Clientes

| Método | Endpoint                | Descrição             |
|--------|-------------------------|-----------------------|
| POST   | `/v3/customers`         | Criar cliente         |
| GET    | `/v3/customers`         | Listar clientes       |
| GET    | `/v3/customers/{id}`    | Buscar cliente        |
| PUT    | `/v3/customers/{id}`    | Atualizar cliente     |
| DELETE | `/v3/customers/{id}`    | Remover cliente       |

### Cobranças

| Método | Endpoint                                | Descrição                  |
|--------|-----------------------------------------|----------------------------|
| POST   | `/v3/payments`                          | Criar cobrança             |
| GET    | `/v3/payments`                          | Listar cobranças           |
| GET    | `/v3/payments/{id}`                     | Buscar cobrança            |
| PUT    | `/v3/payments/{id}`                     | Atualizar cobrança         |
| DELETE | `/v3/payments/{id}`                     | Remover cobrança           |
| GET    | `/v3/payments/{id}/pixQrCode`           | QR Code PIX                |
| GET    | `/v3/payments/{id}/identificationField` | Linha digitável boleto     |
| POST   | `/v3/payments/{id}/refund`              | Estornar cobrança          |

### Assinaturas

| Método | Endpoint                            | Descrição                |
|--------|-------------------------------------|--------------------------|
| POST   | `/v3/subscriptions`                 | Criar assinatura         |
| GET    | `/v3/subscriptions`                 | Listar assinaturas       |
| GET    | `/v3/subscriptions/{id}`            | Buscar assinatura        |
| PUT    | `/v3/subscriptions/{id}`            | Atualizar assinatura     |
| DELETE | `/v3/subscriptions/{id}`            | Cancelar assinatura      |

### Links de Pagamento

| Método | Endpoint                | Descrição               |
|--------|-------------------------|-------------------------|
| POST   | `/v3/paymentLinks`      | Criar link              |
| GET    | `/v3/paymentLinks`      | Listar links            |
| GET    | `/v3/paymentLinks/{id}` | Buscar link             |
| DELETE | `/v3/paymentLinks/{id}` | Remover link            |

### Transferências

| Método | Endpoint           | Descrição              |
|--------|--------------------|------------------------|
| POST   | `/v3/transfers`    | Criar transferência    |
| GET    | `/v3/transfers`    | Listar transferências  |
| GET    | `/v3/transfers/{id}` | Buscar transferência |

### Tokenização de Cartão

| Método | Endpoint                    | Descrição           |
|--------|-----------------------------|---------------------|
| POST   | `/v3/creditCard/tokenize`   | Tokenizar cartão    |

### Webhooks

| Método | Endpoint       | Descrição              |
|--------|----------------|------------------------|
| POST   | `/v3/webhook`  | Configurar webhook     |
| GET    | `/v3/webhook`  | Consultar configuração |

---

## Implementação em Android (Java)

### Classe de integração com a API

```java
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class AsaasApi {

    // Use BuildConfig ou variável de ambiente - NUNCA hardcode a API Key
    private static final String BASE_URL = BuildConfig.ASAAS_BASE_URL;
    private static final String API_KEY = BuildConfig.ASAAS_API_KEY;

    /**
     * Requisição genérica para a API Asaas.
     */
    public static JSONObject request(String method, String endpoint, JSONObject body)
            throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", "JardulliCalculadora");
        conn.setRequestProperty("access_token", API_KEY);

        if (body != null && ("POST".equals(method) || "PUT".equals(method))) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes("UTF-8"));
            }
        }

        int responseCode = conn.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return new JSONObject(response.toString());
    }

    /**
     * Criar cliente.
     */
    public static JSONObject criarCliente(String nome, String cpfCnpj, String email)
            throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", nome);
        body.put("cpfCnpj", cpfCnpj);
        if (email != null) body.put("email", email);
        return request("POST", "/customers", body);
    }

    /**
     * Criar cobrança PIX.
     */
    public static JSONObject criarCobrancaPix(String customerId, double valor, String vencimento)
            throws Exception {
        JSONObject body = new JSONObject();
        body.put("customer", customerId);
        body.put("billingType", "PIX");
        body.put("value", valor);
        body.put("dueDate", vencimento);
        return request("POST", "/payments", body);
    }

    /**
     * Obter QR Code PIX.
     */
    public static JSONObject obterQrCodePix(String paymentId) throws Exception {
        return request("GET", "/payments/" + paymentId + "/pixQrCode", null);
    }

    /**
     * Criar cobrança Boleto.
     */
    public static JSONObject criarCobrancaBoleto(String customerId, double valor,
            String vencimento, String descricao) throws Exception {
        JSONObject body = new JSONObject();
        body.put("customer", customerId);
        body.put("billingType", "BOLETO");
        body.put("value", valor);
        body.put("dueDate", vencimento);
        if (descricao != null) body.put("description", descricao);
        return request("POST", "/payments", body);
    }

    /**
     * Criar cobrança Cartão de Crédito com token.
     */
    public static JSONObject criarCobrancaCartao(String customerId, double valor,
            String vencimento, String creditCardToken) throws Exception {
        JSONObject body = new JSONObject();
        body.put("customer", customerId);
        body.put("billingType", "CREDIT_CARD");
        body.put("value", valor);
        body.put("dueDate", vencimento);
        body.put("creditCardToken", creditCardToken);
        return request("POST", "/payments", body);
    }

    /**
     * Consultar status de cobrança.
     */
    public static JSONObject consultarCobranca(String paymentId) throws Exception {
        return request("GET", "/payments/" + paymentId, null);
    }
}
```

### Uso em Activity (com AsyncTask ou thread)

```java
// Exemplo: criar cobrança PIX e exibir QR Code
new Thread(() -> {
    try {
        // 1. Criar cliente
        JSONObject cliente = AsaasApi.criarCliente(
            "Maria Oliveira", "98765432100", "maria@email.com");
        String customerId = cliente.getString("id");

        // 2. Criar cobrança PIX
        JSONObject cobranca = AsaasApi.criarCobrancaPix(
            customerId, 150.00, "2026-08-01");
        String paymentId = cobranca.getString("id");

        // 3. Obter QR Code
        JSONObject qrCode = AsaasApi.obterQrCodePix(paymentId);
        String encodedImage = qrCode.getString("encodedImage");
        String pixCopiaECola = qrCode.getString("payload");

        // 4. Exibir na UI
        runOnUiThread(() -> {
            byte[] decoded = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            imageViewQrCode.setImageBitmap(bitmap);
            textViewPixCode.setText(pixCopiaECola);
        });

    } catch (Exception e) {
        runOnUiThread(() ->
            Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}).start();
```

### Boas práticas de implementação

1. **Nunca exponha a API Key no app mobile** - Use um backend intermediário para chamadas à API Asaas
2. **Use HTTPS** em todas as comunicações
3. **Valide CPF/CNPJ** antes de enviar à API
4. **Trate erros da API** - verifique o campo `errors` na resposta
5. **Comece pelo sandbox** - teste completamente antes de ir para produção
6. **Implemente webhooks** no seu backend para receber notificações de pagamento
7. **Guarde o `externalReference`** com o ID do seu sistema para facilitar reconciliação
8. **Use tokenização** para cartões recorrentes em vez de armazenar dados do cartão

### Estrutura recomendada para produção

```
App Android
    │
    ├── Tela de pagamento (escolha PIX/Boleto/Cartão)
    │
    └── Seu Backend (API intermediária)
            │
            ├── POST /api/pagamento → chama Asaas API
            ├── GET  /api/pagamento/{id}/status → consulta status
            ├── POST /webhook/asaas → recebe eventos
            │
            └── Asaas API (api.asaas.com/v3)
```

O app Android **nunca deve chamar a API Asaas diretamente** em produção. Use sempre um backend intermediário que guarde a API Key de forma segura.

---

## Referências

- [Documentação oficial Asaas](https://docs.asaas.com)
- [Sandbox Asaas](https://sandbox.asaas.com)
- [API Reference](https://docs.asaas.com/reference)
