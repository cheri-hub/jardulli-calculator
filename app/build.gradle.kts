import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.jardullicalculadora"
    compileSdk = 36

    val localProps = rootProject.file("local.properties")
    val properties = Properties()
    if (localProps.exists()) {
        localProps.inputStream().use { properties.load(it) }
    }

    defaultConfig {
        applicationId = "br.com.agrosinergia.jardulli"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Supabase config from local.properties
        buildConfigField("String", "SUPABASE_URL",
            "\"${properties.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY",
            "\"${properties.getProperty("SUPABASE_ANON_KEY", "")}\"")
        // Número de WhatsApp para suporte (formato internacional sem '+').
        // Vazio => botão de WhatsApp ficará oculto na SuporteActivity.
        buildConfigField("String", "WHATSAPP_SUPORTE",
            "\"${properties.getProperty("WHATSAPP_SUPORTE", "")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    val storeFile = properties.getProperty("STORE_FILE", "")
    val storePassword = properties.getProperty("STORE_PASSWORD", "")
    val keyAlias = properties.getProperty("KEY_ALIAS", "")
    val keyPassword = properties.getProperty("KEY_PASSWORD", "")

    signingConfigs {
        if (storeFile.isNotBlank()) {
            create("release") {
                this.storeFile = rootProject.file(storeFile)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (storeFile.isNotBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // RecyclerView (usado em HistoricoActivity / HistoricoAdapter)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Criptografia para SharedPreferences (tokens Supabase).
    // Mantemos o 1.1.0-alpha06 porque:
    //  - 1.0.0 usa a API legada MasterKeys (deprecada, com bugs de migração de chaves)
    //  - Não há release estável no canal Google até hoje (apr/2026)
    //  - 1.1.0-alpha06 é a versão mais corrigida disponível
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ADICIONE ESTA LINHA PARA O PDF:
    implementation("com.itextpdf:itext7-core:7.2.5")

    // OkHttp para comunicação com Supabase
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}