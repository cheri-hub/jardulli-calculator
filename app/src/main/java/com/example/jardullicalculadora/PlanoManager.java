package com.example.jardullicalculadora;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Gerencia o plano ativo do usuário com cache em SharedPreferences.
 * Sincroniza com Supabase quando online; fallback para cache offline.
 */
public class PlanoManager {

    private static final String TAG = "PlanoManager";
    private static final String PREFS_NAME = "jardulli_plano_prefs";
    private static final String KEY_PLANO_ID = "plano_id";
    private static final String KEY_LAST_SYNC = "last_sync_millis";
    private static final String KEY_ACCESS_TOKEN = "supabase_access_token";
    private static final String KEY_REFRESH_TOKEN = "supabase_refresh_token";
    private static final String KEY_USER_ID = "supabase_user_id";
    private static final String KEY_PLAN_FEATURES = "plan_features_json";
    private static final String KEY_SUBSCRIPTION_ID = "subscription_id";
    private static final String KEY_CLOUD_BALANCO_COUNT = "cloud_balanco_count";
    private static final String KEY_CLOUD_BALANCO_MONTH = "cloud_balanco_month";
    private static final String KEY_DEVICE_ID = "device_id";
    // Cache curto (1h) para reduzir janela de uso premium pós-cancelamento.
    // Features críticas (PDF, Histórico) devem chamar syncComSupabase() antes do uso.
    private static final long CACHE_TTL_MILLIS = 60 * 60 * 1000L; // 1h

    private final SharedPreferences prefs;
    private Plano planoAtual;

    public PlanoManager(Context context) {
        this.prefs = createPrefs(context);
        String planoId = prefs.getString(KEY_PLANO_ID, Plano.TESTDRIVE.getId());
        this.planoAtual = Plano.fromId(planoId);
        restaurarSessao();
    }

    /**
     * Cria SharedPreferences criptografado (AES256). Faz fallback para o legado
     * em caso de falha (ex.: KeyStore corrompido) para evitar crash.
     */
    private static SharedPreferences createPrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context.getApplicationContext(),
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.e(TAG, "Falha ao criar EncryptedSharedPreferences, usando fallback", e);
            return context.getApplicationContext()
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    // ──────────────── Plano e Permissões ────────────────

    public Plano getPlanoAtual() {
        return planoAtual;
    }

    public boolean podeFazerBalanco() {
        int limite = getLimiteBalancosMes();
        if (limite < 0) return true;
        return getBalancosMesAtual() < limite;
    }

    /**
     * Retorna o id da assinatura ativa salvo no último sync. Pode ser null
     * se o usuário não tiver assinatura paga (plano testdrive).
     */
    public String getSubscriptionId() {
        return prefs.getString(KEY_SUBSCRIPTION_ID, null);
    }

    public boolean podeGerarPDF() {
        JSONObject f = getFeaturesCache();
        if (f != null && f.has("pdf")) return f.optBoolean("pdf", false);
        return planoAtual.isPodePDF();
    }

    public boolean podeVerHistorico() {
        JSONObject f = getFeaturesCache();
        if (f != null && f.has("historico")) return f.optBoolean("historico", false);
        return planoAtual.isPodeHistorico();
    }

    public String getTipoSuporte() {
        JSONObject f = getFeaturesCache();
        if (f != null && f.has("suporte")) return f.optString("suporte", planoAtual.getTipoSuporte());
        return planoAtual.getTipoSuporte();
    }

    public int getLimiteBalancosMes() {
        JSONObject f = getFeaturesCache();
        if (f != null && f.has("limite_balancos_mes")) {
            return f.optInt("limite_balancos_mes", planoAtual.getLimiteBalancosMes());
        }
        return planoAtual.getLimiteBalancosMes();
    }

    // ──────────────── Contador de Balanços Mensais ────────────────

    public void registrarBalanco() {
        String chave = getChaveMesAtual();
        int atual = prefs.getInt(chave, 0);
        prefs.edit().putInt(chave, atual + 1).apply();
    }

    public int getBalancosMesAtual() {
        // Quando temos contagem da nuvem válida para o mês corrente, ela
        // é a fonte de verdade (resiste a clear-data e troca de aparelho).
        String mesAtual = getChaveMesAtual();
        String mesCloud = prefs.getString(KEY_CLOUD_BALANCO_MONTH, null);
        if (mesAtual.equals(mesCloud)) {
            int cloud = prefs.getInt(KEY_CLOUD_BALANCO_COUNT, -1);
            int local = prefs.getInt(mesAtual, 0);
            // Pega o maior dos dois para não subestimar quando o local
            // foi incrementado depois do último sync.
            return cloud >= 0 ? Math.max(cloud, local) : local;
        }
        return prefs.getInt(mesAtual, 0);
    }

    private String getChaveMesAtual() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        return "balancos_" + sdf.format(new Date());
    }

    // ──────────────── Sync com Supabase ────────────────

    public void syncComSupabase(SyncCallback callback) {
        SupabaseClient client = SupabaseClient.getInstance();
        if (!client.isLoggedIn()) {
            Log.d(TAG, "Não logado, mantendo plano do cache");
            if (callback != null) callback.onSyncComplete(false);
            return;
        }

        // Antes de sincronizar plano, verifica se este aparelho ainda
        // é o autorizado. Se foi destronado, abandona o sync.
        verificarDispositivo(authorized -> {
            if (!SupabaseClient.getInstance().isLoggedIn()) {
                if (callback != null) callback.onSyncComplete(false);
                return;
            }
            sincronizarPlanoInterno(callback);
        });
    }

    private void sincronizarPlanoInterno(SyncCallback callback) {
        SupabaseClient client = SupabaseClient.getInstance();
        client.fetchActiveSubscription(new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    // RPC get_active_subscription retorna TABLE — vem como array.
                    // SupabaseClient embrulha arrays em { "data": [...] }.
                    JSONObject row = null;
                    org.json.JSONArray arr = response.optJSONArray("data");
                    if (arr != null && arr.length() > 0) {
                        row = arr.optJSONObject(0);
                    } else if (response.has("plan_slug")) {
                        // fallback: alguns gateways retornam objeto direto
                        row = response;
                    }

                    if (row == null) {
                        // Sem assinatura ativa = Testdrive
                        atualizarPlano(Plano.TESTDRIVE, null);
                        prefs.edit().remove(KEY_SUBSCRIPTION_ID).apply();
                        Log.d(TAG, "Sync OK: sem assinatura ativa, plano=TESTDRIVE");
                        sincronizarContagemCloud();
                        if (callback != null) callback.onSyncComplete(true);
                        return;
                    }

                    String planSlug = row.optString("plan_slug", Plano.TESTDRIVE.getId());
                    Plano novoPlano = Plano.fromId(planSlug);
                    JSONObject features = row.optJSONObject("plan_features");
                    String subId = row.optString("subscription_id", null);
                    atualizarPlano(novoPlano, features);
                    prefs.edit().putString(KEY_SUBSCRIPTION_ID, subId).apply();
                    Log.d(TAG, "Sync OK: plano=" + novoPlano.getId());
                    // Em paralelo, atualiza contagem cloud do mês (best-effort)
                    sincronizarContagemCloud();
                    if (callback != null) callback.onSyncComplete(true);
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao processar sync", e);
                    if (callback != null) callback.onSyncComplete(false);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Sync falhou: " + errorMessage);
                verificarCacheExpirado();
                if (callback != null) callback.onSyncComplete(false);
            }
        });
    }

    private void atualizarPlano(Plano plano, JSONObject features) {
        this.planoAtual = plano;
        SharedPreferences.Editor editor = prefs.edit()
                .putString(KEY_PLANO_ID, plano.getId())
                .putLong(KEY_LAST_SYNC, System.currentTimeMillis());
        if (features != null) {
            editor.putString(KEY_PLAN_FEATURES, features.toString());
        } else {
            editor.remove(KEY_PLAN_FEATURES);
        }
        editor.apply();
    }

    /**
     * Lê os features JSONB do plano (vindos do backend) com fallback
     * para o enum {@link Plano} caso o cache esteja vazio ou inválido.
     * Permite ao backend ajustar limites/permissões sem rebuild do APK.
     */
    private JSONObject getFeaturesCache() {
        String raw = prefs.getString(KEY_PLAN_FEATURES, null);
        if (raw == null || raw.isEmpty()) return null;
        try {
            return new JSONObject(raw);
        } catch (Exception e) {
            Log.w(TAG, "plan_features cache corrompido, ignorando", e);
            return null;
        }
    }

    private void verificarCacheExpirado() {
        long lastSync = prefs.getLong(KEY_LAST_SYNC, 0);
        if (lastSync > 0 && (System.currentTimeMillis() - lastSync) > CACHE_TTL_MILLIS) {
            Log.w(TAG, "Cache expirado (>1h sem sync), forçando Testdrive");
            atualizarPlano(Plano.TESTDRIVE, null);
        }
    }

    /**
     * Busca a contagem real de balanços do mês via RPC server-side e
     * armazena no cache para uso por {@link #getBalancosMesAtual()}.
     * Resultado primitivo (integer) chega embrulhado em {"data": N}.
     */
    private void sincronizarContagemCloud() {
        SupabaseClient.getInstance().fetchBalancosMesAtual(new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                int count = response.optInt("data", -1);
                if (count < 0) return;
                prefs.edit()
                        .putInt(KEY_CLOUD_BALANCO_COUNT, count)
                        .putString(KEY_CLOUD_BALANCO_MONTH, getChaveMesAtual())
                        .apply();
                Log.d(TAG, "Contagem cloud atualizada: " + count);
            }

            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "Falha ao sincronizar contagem cloud: " + errorMessage);
            }
        });
    }

    // ──────────────── Sessão Auth ────────────────

    public void salvarSessao(String accessToken, String refreshToken, String userId) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_USER_ID, userId)
                .apply();
    }

    public void limparSessao() {
        prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_PLAN_FEATURES)
                .remove(KEY_SUBSCRIPTION_ID)
                .remove(KEY_CLOUD_BALANCO_COUNT)
                .remove(KEY_CLOUD_BALANCO_MONTH)
                .putString(KEY_PLANO_ID, Plano.TESTDRIVE.getId())
                .putLong(KEY_LAST_SYNC, 0)
                .apply();
        this.planoAtual = Plano.TESTDRIVE;
    }

    public boolean temSessaoSalva() {
        return prefs.getString(KEY_ACCESS_TOKEN, null) != null;
    }

    /**
     * Retorna (e gera, se necessário) o UUID persistente que identifica este
     * aparelho para o backend. Usado para enforcement de 1-dispositivo-por-conta.
     */
    public String getDeviceId() {
        String id = prefs.getString(KEY_DEVICE_ID, null);
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString(KEY_DEVICE_ID, id).apply();
        }
        return id;
    }

    /**
     * Consulta {@code profiles.current_device_id} no servidor e compara com
     * o {@link #getDeviceId()} local. Se diferente (ou null), significa que
     * outro aparelho assumiu a conta — limpa a sessão localmente.
     *
     * @return true se este aparelho ainda é o autorizado.
     */
    private void verificarDispositivo(SyncCallback callback) {
        SupabaseClient.getInstance().fetchCurrentDeviceId(new SupabaseClient.SupabaseCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                org.json.JSONArray arr = response.optJSONArray("data");
                if (arr == null || arr.length() == 0) {
                    if (callback != null) callback.onSyncComplete(true);
                    return;
                }
                JSONObject row = arr.optJSONObject(0);
                String remoto = row != null ? row.optString("current_device_id", null) : null;
                String local = getDeviceId();
                // Se o backend ainda não tem device id (perfil novo / migração antiga),
                // não invalida nada — o login subsequente vai registrar.
                if (remoto == null || remoto.isEmpty() || "null".equals(remoto)) {
                    if (callback != null) callback.onSyncComplete(true);
                    return;
                }
                if (!remoto.equals(local)) {
                    Log.w(TAG, "Outro aparelho assumiu a conta — forçando logout local");
                    forcarLogout();
                    if (callback != null) callback.onSyncComplete(false);
                } else {
                    if (callback != null) callback.onSyncComplete(true);
                }
            }

            @Override
            public void onError(String errorMessage) {
                // Falha de rede não deve deslogar — best-effort.
                Log.w(TAG, "verificarDispositivo falhou: " + errorMessage);
                if (callback != null) callback.onSyncComplete(false);
            }
        });
    }

    /**
     * Limpa a sessão e dispara o callback de expiração para a UI navegar
     * para a tela de login. Chamado quando outro aparelho assume a conta.
     */
    public void forcarLogout() {
        SupabaseClient.getInstance().signOut();
        limparSessao();
    }

    private void restaurarSessao() {
        String token = prefs.getString(KEY_ACCESS_TOKEN, null);
        String refresh = prefs.getString(KEY_REFRESH_TOKEN, null);
        String uid = prefs.getString(KEY_USER_ID, null);
        SupabaseClient client = SupabaseClient.getInstance();
        if (token != null && uid != null) {
            client.restoreSession(token, refresh, uid);
        }
        // Persiste tokens renovados pelo refresh-token loop e limpa sessão expirada.
        client.setSessionListener(new SupabaseClient.SessionListener() {
            @Override
            public void onTokensRefreshed(String accessToken, String refreshToken, String userId) {
                salvarSessao(accessToken, refreshToken, userId);
                Log.d(TAG, "Tokens renovados e persistidos");
            }

            @Override
            public void onSessionExpired() {
                Log.w(TAG, "Sessão expirada — limpando");
                limparSessao();
            }
        });
    }

    // ──────────────── Callback ────────────────

    public interface SyncCallback {
        void onSyncComplete(boolean success);
    }
}
