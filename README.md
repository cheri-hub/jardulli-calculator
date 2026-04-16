# JardulliCalculadora

Calculadora agrícola Android para parametrização de tratores. O aplicativo auxilia no cálculo de distribuição de pesos, especificações de rodados, lastro líquido, razão de avanço e dados operacionais, com geração de relatórios em PDF.

## Tecnologias

| Item | Valor |
|------|-------|
| Linguagem | Java 11 |
| SDK mínimo | 24 (Android 7.0) |
| SDK alvo | 35 (Android 15) |
| Build | Gradle 8.9.0 (Kotlin DSL) |
| UI | Material Design + ConstraintLayout |
| PDF | iText 7.2.5 |
| Banco de dados | SQLite (DBHelper) |

## Estrutura do projeto

```
app/src/main/java/com/example/jardullicalculadora/
├── MainActivity.java              # Tela inicial
├── TratorOperacaoActivity.java    # Dados de operação do trator
├── RodadosActivity.java           # Especificações de rodados
├── PesoFrontalActivity.java       # Cálculo de peso frontal
├── PesosTraseirosActivity.java    # Cálculo de pesos traseiros
├── LastroLiquidoActivity.java     # Cálculo de lastro líquido
├── DadosJardulli.java             # Dados/configurações Jardulli
├── DBHelper.java                  # Helper do banco SQLite
├── RelatorioActivity2.java        # Tela de relatório
└── RelatorioFinalActivity.java    # Relatório final com exportação PDF
```

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

2. Abra o projeto no Android Studio (`File > Open` e selecione a pasta raiz).

3. Aguarde o Gradle sincronizar as dependências automaticamente.

4. Conecte um dispositivo físico via USB (com depuração USB ativa) ou crie um emulador com API 24+.

5. Clique em **Run ▶** ou pressione `Shift+F10`.

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

Executam localmente sem dispositivo/emulador:

```bash
./gradlew test
```

Relatório HTML gerado em `app/build/reports/tests/testDebugUnitTest/`.

### Testes instrumentados (dispositivo/emulador)

Requerem um dispositivo ou emulador em execução:

```bash
./gradlew connectedAndroidTest
```

Relatório HTML gerado em `app/build/reports/androidTests/connected/`.

### No Android Studio

- Clique com botão direito em `app/src/test/` → **Run Tests** (unitários)
- Clique com botão direito em `app/src/androidTest/` → **Run Tests** (instrumentados)

## Licença

A definir.
