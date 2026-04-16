# Copilot Instructions — JardulliCalculadora

## Visão Geral
Calculadora agrícola Android para parametrização de tratores: distribuição de pesos, lastro líquido, rodados, pesos frontais/traseiros e geração de relatórios PDF.

## Stack & Ambiente
- **Linguagem:** Java 11
- **Plataforma:** Android (minSdk 24, targetSdk 35, compileSdk 35)
- **Build:** Gradle 8.9.0 com Kotlin DSL
- **Pacote:** `com.example.jardullicalculadora`
- **UI:** Material Design + ConstraintLayout
- **Banco:** SQLite via `DBHelper` (SQLiteOpenHelper)
- **PDF:** iText 7.2.5 (`com.itextpdf:itext7-core:7.2.5`)
- **FileProvider:** `com.example.jardullicalculadora.fileprovider`
- **Dependências:** version catalog em `gradle/libs.versions.toml`

## Arquitetura

### Fluxo Linear de Activities
Cada tela coleta dados e passa adiante via `DadosJardulli` (Parcelable) em Intent extras:
```
MainActivity
  → TratorOperacaoActivity   (série, modelo, operação, engate, peso operador)
    → RodadosActivity         (rodados dianteiro/traseiro, eixos, grupos)
      → LastroLiquidoActivity (lastro líquido diant./tras. com multiplicador %)
        → PesoFrontalActivity (pesos frontais, traseiros, cabine)
          → RelatorioFinalActivity (exibe resultados + gera PDF)
```

### Activities Declaradas mas Sem Código Fonte
- `AvancoRatio` e `AvancoRatioActivity` — registradas no Manifest, **sem .java criado** (crashará se acessadas).
- `PesosTraseirosActivity` e `RelatorioActivity2` — existem mas são **stubs vazios** (apenas EdgeToEdge).

## Classes Principais

### `DadosJardulli.java` — DTO Parcelable
Acumula todos os dados entre telas. Campos-chave:
- `serieTrator`, `trator` (modelo), `operacao`, `pontoEngate`
- `rodadoDianteiro/Traseiro`, `grupoDianteiro/Traseiro`, `tipoEixoDianteiro/Traseiro`
- `lastroDianteiro/Traseiro` (resultado do cálculo de lastro líquido)
- `pesoTrator` (potência CV), `potenciaKW`, `pesoMaximo`
- `capacidadeDiesel`, `distanciaEntreEixos`, `pesoOperador`
- `pesoFrontal`, `pesoEixoDianteiro/Traseiro`
- `quantidadePesos91/227/454/625` — quantidades de cada peso traseiro
- `pesoRealTraseiro1/2/3/4` — peso real em kg de cada tipo (varia por série)
- `cabideStr`, `pesoFrontalStr`, `pesoTotal` — strings de exibição

### `DBHelper.java` — Banco SQLite
- **Versão:** 2. Nome: `jardulli.db`
- **Tabelas:** `tratores` (modelo, potencia, potencia_kw, peso_maximo), `ponto_engate`, `operacoes`, `lastros_liquidos` (tipo, rodado, valor_agua), `rodados_dianteiros` (rodado, grupo), `rodados_traseiros` (rodado, grupo)
- **Dados iniciais:** inseridos no `onCreate()` via `inserirDadosIniciais()`
- NOTA: O DBHelper tem dados apenas da série JD 8R. As demais séries utilizam arrays hardcoded nas Activities.

### `TratorOperacaoActivity.java`
- Arrays locais com dados de **9 séries** (6 ativas, 3 desativadas/comentadas)
- Séries ativas: `John Deere 8R`, `New Holand T7`, `New Holand T.8`, `Case Puma`, `John Deere 7J/M`, `John Deere 6J/M`
- Séries desativadas: `Case Magunum`, `Magnum Afs Connect`, `Valtra Tcvt`
- Mapa de índice ativo → original: `indiceParaOriginal = {0, 1, 2, 3, 7, 8}`
- Arrays paralelos: `modelosPorSerie[][]`, `potenciasCV[][]`, `potenciasKW[][]`, `pesosMaximos[][]`, `capacidadeDiesel[][]`, `distanciaEntreEixos[][]`

### `RodadosActivity.java`
- Rodados e grupos específicos por série (arrays independentes por série)
- Tipos de eixo: `SINGLE/DUAL` (padrão), `1.500 SINGLE/DUAL, ILS SINGLE/DUAL` (JD 8R), `FIXO PADRÃO/REFORÇADO` (NH)
- Configura spinner baseado na série recebida via switch/case

### `LastroLiquidoActivity.java`
- Multiplicadores: 5% a 100% (em passos de 5)
- **Dois algoritmos de cálculo:**
  - Cálculo 1: `(valorLastro × multiplicador) / 75 × 2`
  - Cálculo 2: `(valorLastro × multiplicador) / 40 × 2`
- Seleção via `usarCalculo1(serie, isDianteiro)`:
  - JD 8R: dianteiro=Cálc.2, traseiro=Cálc.1
  - NH T7, NH T.8, Case Magunum, Valtra, JD 6J/M: Cálc.1
  - Case Puma, Magnum Afs, JD 7J/M: Cálc.2
- Valores de lastro por rodado: hardcoded em switch/case por série

### `PesoFrontalActivity.java`
- Pesos específicos por série (frontais e traseiros variam)
- JD 8R: frontais 50kg, traseiros 72/205/160/625 kg
- NH T7: frontais 45/450 kg, traseiros 91/227 kg
- NH T.8: frontais 40 kg, traseiros 91/225/454 kg
- Case Puma: frontais 45/250 kg, traseiros 91/227 kg
- JD 7J/M: frontais 50/140 kg, traseiros 72/205/160 kg
- JD 6J/M: frontais 50(mala)/80(along.), traseiros 49/72/205 kg
- Multiplicadores de quantidade: 0,1,2,4,6,8,10,12,14,16,18,20,22,24,26,28,30
- Cabine: Sim=250kg / Não=0

### `RelatorioFinalActivity.java`
- **`calcularPesosCompletos()`:** Fórmula principal:
  - `pesoDianteiro = eixoDiant + lastroDiant + pesoFrontal + (operador/2) + (diesel×0.86/2)`
  - `pesoTraseiro = eixoTras + lastroTras + (operador/2) + (diesel×0.86/2) + cabine + implemento + pesosTraseirosAdicionais`
  - `pesoTotal = pesoDianteiro + pesoTraseiro`
- **Resultados calculados:** peso total, peso/CV nominal, peso/kW, divisão %, divisão entre eixos, diesel L/h
- **Recomendações:** baseadas em `pesoMaximo` e `pontoEngate`:
  - 3 PONTOS: 40% diant / 60% tras
  - B.T.O.: 35% diant / 65% tras
  - REBOQUE: 30% diant / 70% tras
- **Consumo diesel:** `potenciaCV × fatorOperacao × 0.15` (LEVE=0.45, MÉDIO=0.55, PESADO=0.75)
- **Geração PDF:** iText 7, salva em Downloads, abre via FileProvider + Intent ACTION_VIEW
- **Formatação numérica:** `formatSemDecimais()` e `format()` — padrão brasileiro (ponto=milhar, vírgula=decimal)

## Convenções de Código

### Nomenclatura
- Layouts: `activity_*.xml`
- IDs XML: camelCase (`btnAvancar`, `spinnerModelo`, `textResultadoDianteiro`, `txtPesoTotalGeral`)
- Prefixos de ID: `btn` (botões), `spinner` (spinners), `text`/`txt` (TextViews), `edit` (EditText)
- Variáveis: camelCase em português (`pesoDianteiroCalculado`, `lastroTraseiroCalculado`)

### Idioma
- **Português brasileiro** em: UI, strings, variáveis, comentários, logs

### Padrões Recorrentes
- Sem injeção de dependência — dados passados via `Intent.putExtra("DADOS_JARDULLI", dados)`
- Recuperação: `getIntent().getParcelableExtra("DADOS_JARDULLI")`
- Validação null com `finish()` em caso de dados faltantes
- Logging via `Log.d(TAG, ...)` / `Log.e(TAG, ...)`
- Toasts para feedback ao usuário
- Dados de referência em arrays hardcoded (não no SQLite) para a maioria das séries
- Orientação forçada: `android:screenOrientation="portrait"` para todas as Activities

## Build & Testes
```bash
./gradlew assembleDebug          # APK debug → app/build/outputs/apk/debug/
./gradlew test                   # Testes unitários JVM
./gradlew connectedAndroidTest   # Testes instrumentados (requer device/emulador)
```

## Funcionalidades Pendentes
- **Patinagem/Avanço:** Activities declaradas no Manifest sem código (`AvancoRatio`, `AvancoRatioActivity`)
- **Histórico de Frota:** sem persistência de balanços realizados
- **Sistema de Assinatura:** sem billing, login ou controle de features por plano
- **Suporte:** sem tela de FAQ, links de email ou WhatsApp
