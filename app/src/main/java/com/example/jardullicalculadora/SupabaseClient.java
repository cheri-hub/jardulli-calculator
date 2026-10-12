package com.example.jardullicalculadora;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Singleton HTTP client para comunicação com Supabase.
 * Auth (signup/signin/signout) + consultas REST (subscriptions, plans).
 */
public class SupabaseClient {

    private static final String TAG = "SupabaseClient";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static SupabaseClient instance;

    private final String supabaseUrl;
    private final String anonKey;
    private final OkHttpClient httpClient;

    private String accessToken;
    private String refreshToken;
    private String userId;
    /**
     * true enquanto a sessão em memória veio da recuperação de senha. Nesse estado
     * não há refresh nem {@link SessionListener}: a sessão nunca é persistida.
     */
    private volatile boolean sessaoTemporaria;

    /** Listener opcional para persistir tokens renovados (sessão). */
    public interface SessionListener {
        void onTokensRefreshed(String accessToken, String refreshToken, String userId);
        void onSessionExpired();
    }
    private SessionListener sessionListener;

    public void setSessionListener(SessionListener listener) {
        this.sessionListener = listener;
    }

    private SupabaseClient(String supabaseUrl, String anonKey) {
        this.supabaseUrl = supabaseUrl;
        this.anonKey = anonKey;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized SupabaseClient getInstance() {
        if (instance == null) {
            String url = BuildConfig.SUPABASE_URL;
            String key = BuildConfig.SUPABASE_ANON_KEY;
            instance = new SupabaseClient(url, key);
        }
        return instance;
    }

    public interface SupabaseCallback {
        void onSuccess(JSONObject response);
        void onError(String errorMessage);
    }

    // ──────────────── Auth ────────────────

    public void signUp(String email, String password, SupabaseCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (JSONException e) {
            callback.onError("Erro ao montar requisição: " + e.getMessage());
            return;
        }

        Request request = buildAuthRequest("/auth/v1/signup", body);
        executeAsync(request, callback);
    }

    public void signIn(String email, String password, SupabaseCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (JSONException e) {
            callback.onError("Erro ao montar requisição: " + e.getMessage());
            return;
        }

        Request request = buildAuthRequest("/auth/v1/token?grant_type=password", body);
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "signIn falhou", e);
                callback.onError("Falha na conexão: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        accessToken = json.optString("access_token", null);
                        refreshToken = json.optString("refresh_token", null);
                        JSONObject user = json.optJSONObject("user");
                        if (user != null) {
                            userId = user.optString("id", null);
                        }
                        callback.onSuccess(json);
                    } catch (JSONException e) {
                        callback.onError("Erro ao processar resposta: " + e.getMessage());
                    }
                } else {
                    String msg = parseErrorMessage(responseBody);
                    callback.onError(msg);
                }
            }
        });
    }

    public void signOut() {
        if (accessToken != null) {
            Request request = new Request.Builder()
                    .url(supabaseUrl + "/auth/v1/logout")
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .post(RequestBody.create("", JSON))
                    .build();
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "signOut request falhou", e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    Log.d(TAG, "signOut: " + response.code());
                }
            });
        }
        accessToken = null;
        refreshToken = null;
        userId = null;
        sessaoTemporaria = false;
    }

    // ──────────────── Recuperação de senha ────────────────

    /** true se há uma sessão de recuperação de senha viva em memória. */
    public boolean isSessaoTemporaria() { return sessaoTemporaria; }

    /**
     * Pede o envio do código de recuperação por e-mail. O Supabase responde 200
     * mesmo quando o e-mail não existe (não revela quais contas existem).
     */
    public void recoverPassword(String email, SupabaseCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
        } catch (JSONException e) {
            callback.onError("Erro ao montar requisição: " + e.getMessage());
            return;
        }
        executeAsync(buildAuthRequest("/auth/v1/recover", body), callback);
    }

    /**
     * Troca o código de recuperação por uma sessão temporária, mantida só em
     * memória (não é persistida). Deve ser descartada com {@link #signOut()}.
     */
    public void verifyRecovery(String email, String codigo, SupabaseCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("type", "recovery");
            body.put("email", email);
            body.put("token", codigo);
        } catch (JSONException e) {
            callback.onError("Erro ao montar requisição: " + e.getMessage());
            return;
        }
        executeAsync(buildAuthRequest("/auth/v1/verify", body), new SupabaseCallback() {
            @Override
            public void onSuccess(JSONObject json) {
                String access = json.optString("access_token", "");
                if (access.isEmpty()) {
                    callback.onError("Não foi possível validar o código.");
                    return;
                }
                sessaoTemporaria = true;
                accessToken = access;
                refreshToken = json.optString("refresh_token", null);
                JSONObject user = json.optJSONObject("user");
                if (user != null) {
                    userId = user.optString("id", null);
                }
                callback.onSuccess(json);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    /** Define a nova senha da sessão atual (a sessão temporária de {@link #verifyRecovery}). */
    public void updatePassword(String novaSenha, SupabaseCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("password", novaSenha);
        } catch (JSONException e) {
            callback.onError("Erro ao montar requisição: " + e.getMessage());
            return;
        }
        Request request = new Request.Builder()
                .url(supabaseUrl + "/auth/v1/user")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .put(RequestBody.create(body.toString(), JSON))
                .build();
        executeAsync(request, callback);
    }

    public boolean isLoggedIn() {
        return accessToken != null && userId != null;
    }

    public String getAccessToken() { return accessToken; }
    public String getUserId() { return userId; }

    public void restoreSession(String token, String refresh, String uid) {
        this.sessaoTemporaria = false;
        this.accessToken = token;
        this.refreshToken = refresh;
        this.userId = uid;
    }

    // ──────────────── Subscription ────────────────

    public void fetchActiveSubscription(SupabaseCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("Usuário não autenticado");
            return;
        }

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/rpc/get_active_subscription")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create("{}", JSON))
                .build();

        executeAsync(request, callback);
    }

    public void callEdgeFunction(String functionName, JSONObject body, SupabaseCallback callback) {
        Request request = new Request.Builder()
                .url(supabaseUrl + "/functions/v1/" + functionName)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        executeAsync(request, callback);
    }

    /**
     * Cancela a assinatura ativa do usuário via Edge Function.
     * Acesso continua até {@code current_period_end}.
     */
    public void cancelSubscription(String subscriptionId, SupabaseCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("Usuário não autenticado");
            return;
        }
        JSONObject body = new JSONObject();
        try {
            body.put("subscription_id", subscriptionId);
        } catch (JSONException e) {
            callback.onError("Erro ao montar requisição: " + e.getMessage());
            return;
        }
        callEdgeFunction("cancel-subscription", body, callback);
    }

    /**
     * Conta balanços do usuário no mês corrente via RPC server-side.
     * Resposta vem como número primitivo embrulhado em {@code {"data": N}}
     * (ver {@link #executeAsyncInternal}). Use isso como fonte de verdade
     * para limites do plano quando o usuário estiver logado.
     */
    public void fetchBalancosMesAtual(SupabaseCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("Usuário não autenticado");
            return;
        }
        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/rpc/count_balancos_mes_atual")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create("{}", JSON))
                .build();
        executeAsync(request, callback);
    }

    // ──────────────── Profile ────────────────

    /**
     * Busca o perfil do usuário logado em {@code public.profiles}.
     * Resposta vem como array Supabase REST, embrulhada em {@code {"data":[{...}]}}
     * pelo {@link #executeAsyncInternal}. O caller deve extrair o primeiro elemento.
     */
    public void fetchProfile(SupabaseCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("Usuário não autenticado");
            return;
        }

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/profiles?id=eq." + userId
                        + "&select=full_name,cpf_cnpj,phone")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        executeAsync(request, callback);
    }

    public void updateProfile(String fullName, String cpfCnpj, String phone, SupabaseCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("Usuário não autenticado");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("full_name", fullName);
            body.put("cpf_cnpj", cpfCnpj);
            body.put("phone", phone);
        } catch (JSONException e) {
            callback.onError("Erro ao montar requisição: " + e.getMessage());
            return;
        }

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/profiles?id=eq." + userId)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(RequestBody.create(body.toString(), JSON))
                .build();

        executeAsync(request, callback);
    }

    // ──────────────── Histórico de Balanços (cloud) ────────────────

    /**
     * Persiste um balanço na tabela {@code historico_balancos} do Supabase.
     * O {@code user_id} é injetado automaticamente para casar com a policy RLS.
     * Retorna a row inserida via {@code Prefer: return=representation}.
     */
    public void saveBalanco(JSONObject balanco, SupabaseCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("Usuário não autenticado");
            return;
        }

        try {
            balanco.put("user_id", userId);
        } catch (JSONException e) {
            callback.onError("Erro ao montar requisição: " + e.getMessage());
            return;
        }

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/historico_balancos")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(RequestBody.create(balanco.toString(), JSON))
                .build();

        executeAsync(request, callback);
    }

    /**
     * Lista os balanços do usuário em ordem decrescente de data.
     * Resposta vem como array, embrulhado em {@code { "data": [...] }}.
     */
    public void fetchHistorico(SupabaseCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("Usuário não autenticado");
            return;
        }

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/historico_balancos"
                        + "?select=*&order=data_criacao.desc&limit=200")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        executeAsync(request, callback);
    }

    /**
     * Remove um balanço específico do servidor (RLS garante owner-only).
     */
    public void deleteBalanco(String balancoId, SupabaseCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("Usuário não autenticado");
            return;
        }

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/historico_balancos?id=eq." + balancoId)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Prefer", "return=minimal")
                .delete()
                .build();

        executeAsync(request, callback);
    }

    // ──────────────── Single-device enforcement ────────────────

    /**
     * Registra o dispositivo atual como o único autorizado para o usuário.
     * Sobrescreve o anterior, fazendo com que o app antigo (na próxima
     * verificação) detecte o mismatch e force logout.
     */
    public void registerDevice(String deviceId, SupabaseCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("Usuário não autenticado");
            return;
        }
        JSONObject body = new JSONObject();
        try {
            body.put("current_device_id", deviceId);
        } catch (JSONException e) {
            callback.onError("Erro ao montar requisição: " + e.getMessage());
            return;
        }
        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/profiles?id=eq." + userId)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(RequestBody.create(body.toString(), JSON))
                .build();
        executeAsync(request, callback);
    }

    /**
     * Lê {@code current_device_id} do perfil do usuário. Resposta vem como
     * array embrulhado em {@code {"data":[{"current_device_id":"..."}]}}.
     */
    public void fetchCurrentDeviceId(SupabaseCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("Usuário não autenticado");
            return;
        }
        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/profiles?id=eq." + userId
                        + "&select=current_device_id")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();
        executeAsync(request, callback);
    }

    // ──────────────── Helpers ────────────────

    private Request buildAuthRequest(String path, JSONObject body) {
        return new Request.Builder()
                .url(supabaseUrl + path)
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();
    }

    private void executeAsync(Request request, SupabaseCallback callback) {
        executeAsyncInternal(request, callback, true);
    }

    /**
     * Executa o request e, se receber 401 e houver refresh token,
     * tenta renovar a sessão e refazer o request uma única vez.
     */
    private void executeAsyncInternal(Request request, SupabaseCallback callback, boolean allowRefresh) {
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Request falhou: " + request.url(), e);
                callback.onError("Falha na conexão: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                int code = response.code();

                // Token expirado: tenta refresh + retry uma vez
                if (code == 401 && allowRefresh && !sessaoTemporaria
                        && refreshToken != null && !refreshToken.isEmpty()) {
                    Log.d(TAG, "401 recebido, tentando refresh token");
                    refreshAccessToken(new SupabaseCallback() {
                        @Override
                        public void onSuccess(JSONObject ignored) {
                            // Reconstrói o request com o novo access token
                            Request retried = rebuildWithNewToken(request);
                            executeAsyncInternal(retried, callback, false);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.w(TAG, "Refresh token falhou: " + errorMessage);
                            if (sessionListener != null) sessionListener.onSessionExpired();
                            callback.onError("Sessão expirada. Faça login novamente.");
                        }
                    });
                    return;
                }

                if (response.isSuccessful()) {
                    try {
                        JSONObject json;
                        if (responseBody.isEmpty() || responseBody.equals("null")) {
                            json = new JSONObject();
                        } else if (responseBody.startsWith("[")) {
                            json = new JSONObject();
                            json.put("data", new org.json.JSONArray(responseBody));
                        } else if (responseBody.startsWith("{")) {
                            json = new JSONObject(responseBody);
                        } else {
                            // Resposta primitiva (ex: RPC scalar retornando número/string/bool).
                            // Embrulhamos em {"data": <valor>} para uniformizar o consumo.
                            json = new JSONObject();
                            String trimmed = responseBody.trim();
                            try {
                                json.put("data", Long.parseLong(trimmed));
                            } catch (NumberFormatException nfe) {
                                try {
                                    json.put("data", Double.parseDouble(trimmed));
                                } catch (NumberFormatException nfe2) {
                                    json.put("data", trimmed);
                                }
                            }
                        }
                        callback.onSuccess(json);
                    } catch (JSONException e) {
                        callback.onError("Erro ao processar resposta: " + e.getMessage());
                    }
                } else {
                    Log.w(TAG, "HTTP " + code + " " + request.url() + " body=" + responseBody);
                    String msg = parseErrorMessage(responseBody);
                    callback.onError(msg);
                }
            }
        });
    }

    /**
     * Renova o access token usando o refresh token. Atualiza o estado interno
     * e dispara o {@link SessionListener} para persistência externa (PlanoManager).
     */
    private void refreshAccessToken(SupabaseCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("refresh_token", refreshToken);
        } catch (JSONException e) {
            callback.onError("Erro ao montar refresh: " + e.getMessage());
            return;
        }

        Request request = buildAuthRequest("/auth/v1/token?grant_type=refresh_token", body);
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Falha ao renovar sessão: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    callback.onError(parseErrorMessage(responseBody));
                    return;
                }
                try {
                    JSONObject json = new JSONObject(responseBody);
                    String newAccess = json.optString("access_token", null);
                    String newRefresh = json.optString("refresh_token", refreshToken);
                    if (newAccess == null || newAccess.isEmpty()) {
                        callback.onError("Resposta de refresh sem access_token");
                        return;
                    }
                    accessToken = newAccess;
                    refreshToken = newRefresh;
                    if (sessionListener != null) {
                        sessionListener.onTokensRefreshed(newAccess, newRefresh, userId);
                    }
                    callback.onSuccess(json);
                } catch (JSONException e) {
                    callback.onError("Erro ao processar refresh: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Reconstrói um request preservando método/headers/body, mas trocando
     * o header {@code Authorization} pelo novo access token.
     */
    private Request rebuildWithNewToken(Request original) {
        Request.Builder b = original.newBuilder();
        if (original.header("Authorization") != null) {
            b.removeHeader("Authorization");
            b.addHeader("Authorization", "Bearer " + accessToken);
        }
        return b.build();
    }

    private String parseErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) return "Erro desconhecido";
        try {
            JSONObject root = new JSONObject(responseBody);

            // Edge Functions v2: {"data":null,"error":{"code":"...","message":"..."}}
            JSONObject errObj = root.optJSONObject("error");
            if (errObj != null) {
                String code = errObj.optString("code", "");
                String message = errObj.optString("message", "");
                if (!message.isEmpty()) return code.isEmpty() ? message : code + ": " + message;
                if (!code.isEmpty()) return code;
            }

            // Edge Functions v1 / outros: {"error":"code", "message":"..."}
            String message = root.optString("message", null);
            String code    = root.optString("error", null);
            if (message != null && !message.isEmpty()) {
                return code != null && !code.isEmpty() && !code.startsWith("{") ? code + ": " + message : message;
            }
            if (code != null && !code.isEmpty() && !code.startsWith("{")) return code;

            // Supabase Auth: {"error_description":"..."} ou {"msg":"..."}
            if (root.has("error_description")) return root.getString("error_description");
            if (root.has("msg")) return root.getString("msg");
        } catch (JSONException ignored) {
        }
        // Fallback: devolve o body cru (truncado) para não esconder a causa.
        String trimmed = responseBody.trim();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) + "…" : trimmed;
    }
}
