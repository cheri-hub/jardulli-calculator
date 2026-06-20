# JardulliCalculadora

Calculadora agrícola Android para parametrização de tratores. O aplicativo auxilia no cálculo de distribuição de pesos, especificações de rodados, lastro líquido, razão de avanço (patinagem) e dados operacionais, com geração de relatórios em PDF, autenticação Supabase, histórico de balanços e sistema de planos/assinaturas.

## Tecnologias

| Item | Valor |
|------|-------|
| Linguagem | Java 11 |
| SDK mínimo | 24 (Android 7.0) |
| SDK alvo / compile | 35 (Android 15) |
| Build | Gradle 8.9.0 (Kotlin DSL) |
| UI | Material Design + ConstraintLayout + RecyclerView |
| PDF | iText 7.2.5 |
| HTTP | OkHttp 4.12.0 |
| Backend | Supabase (Auth + REST + RLS) |
| Cripto | androidx.security:security-crypto (EncryptedSharedPreferences) |
| Banco local | SQLite (DBHelper) |

## Estrutura do projeto

```
app/src/main/java/com/example/jardullicalculadora/
├── MainActivity.java              # Tela inicial / hub
├── AuthActivity.java              # Login/cadastro Supabase
├── PerfilActivity.java            # Perfil do usuário (CPF/CNPJ validados Mod 11)
├── PlanosActivity.java            # Seleção/visualização de planos
├── SuporteActivity.java           # Canais de suporte (email/WhatsApp)
├── HistoricoActivity.java         # Lista de balanços salvos (RecyclerView)
├── HistoricoAdapter.java          # Adapter do histórico
│
├── TratorOperacaoActivity.java    # Série, modelo, operação, engate, peso operador
├── RodadosActivity.java           # Rodados dianteiro/traseiro, eixos, grupos
├── LastroLiquidoActivity.java     # Lastro líquido com multiplicador %
├── PesoFrontalActivity.java       # Pesos frontais, traseiros, cabine
├── RelatorioFinalActivity.java    # Cálculos finais + PDF (iText)
├── AvancoRatioActivity.java       # Cálculo de patinagem / razão de avanço
│
├── DadosJardulli.java             # DTO Parcelable (passa entre Activities)
├── DBHelper.java                  # SQLite v3 (tratores, rodados, histórico)
│
├── SupabaseClient.java            # Singleton OkHttp p/ Supabase REST/Auth
├── PlanoManager.java              # Gerencia plano ativo + sessão (cripto)
└── Plano.java                     # Enum de planos (TESTDRIVE, etc.)
```

## Configuração Supabase

Crie/edite `local.properties` na raiz (não commitado):

```properties
sdk.dir=...
SUPABASE_URL=https://<projeto>.supabase.co
SUPABASE_ANON_KEY=<anon-public-key>
```

Esses valores são injetados em `BuildConfig` via `app/build.gradle.kts`.
A `anon_key` é pública por design — **garanta Row Level Security (RLS)** ativo
em todas as tabelas no Supabase.

## Segurança

- **Tokens** (`access_token`, `refresh_token`, `user_id`) armazenados em
  `EncryptedSharedPreferences` (AES256-GCM).
- `backup_rules.xml` e `data_extraction_rules.xml` excluem
  `jardulli_plano_prefs` do auto-backup e device-transfer.
- CPF/CNPJ validados por dígitos verificadores (Módulo 11).

## Como executar pela primeira vez

### Pré-requisitos

- **Android Studio** (Ladybug ou superior recomendado)
- **JDK 11+**
- **Android SDK 35** instalado via SDK Manager

### Passos

1. Clone o repositório:
   ```bash
   git clone <url-do-repositorio>
   cd JardulliCalculadora
   ```

2. Configure `local.properties` com credenciais Supabase (ver acima).

3. Abra o projeto no Android Studio (`File > Open` e selecione a pasta raiz).

4. Aguarde o Gradle sincronizar as dependências automaticamente.

5. Conecte um dispositivo físico via USB (com depuração USB ativa) ou crie um emulador com API 24+.

6. Clique em **Run ▶** ou pressione `Shift+F10`.

### Via linha de comando

```bash
# Build do APK debug
./gradlew assembleDebug

# Instalar no dispositivo conectado
./gradlew installDebug
```

O APK gerado fica em `app/build/outputs/apk/debug/`.

## Como testar

### Testes unitários (JVM)

```bash
./gradlew test
```

Relatório HTML em `app/build/reports/tests/testDebugUnitTest/`.

### Testes instrumentados (dispositivo/emulador)

```bash
./gradlew connectedAndroidTest
```

Relatório HTML em `app/build/reports/androidTests/connected/`.

## Licença

A definir.
