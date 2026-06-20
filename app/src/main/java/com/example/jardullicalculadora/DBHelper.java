package com.example.jardullicalculadora;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "jardulli.db";
    private static final int DATABASE_VERSION = 3; // versão 3 — adicionada tabela historico_balancos

    public static final String TABLE_TRATORES = "tratores";
    public static final String TABLE_PONTO_ENGATE = "ponto_engate";
    public static final String TABLE_OPERACOES = "operacoes";
    public static final String TABLE_LASTROS_LIQUIDOS = "lastros_liquidos";
    public static final String TABLE_RODADOS_DIANTEIROS = "rodados_dianteiros";
    public static final String TABLE_RODADOS_TRASEIROS = "rodados_traseiros";
    public static final String TABLE_HISTORICO_BALANCOS = "historico_balancos";

    // TABELA TRATORES ATUALIZADA ↓
    private static final String CREATE_TABLE_TRATORES =
            "CREATE TABLE " + TABLE_TRATORES + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "modelo TEXT, " +
                    "potencia INTEGER, " +           // CV (já existia)
                    "potencia_kw REAL, " +          // NOVO: Potência em kW
                    "peso_maximo REAL);";           // NOVO: Peso Máximo

    // OUTRAS TABELAS PERMANECEM IGUAIS ↓
    private static final String CREATE_TABLE_PONTO_ENGATE =
            "CREATE TABLE " + TABLE_PONTO_ENGATE + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "ponto TEXT);";

    private static final String CREATE_TABLE_OPERACOES =
            "CREATE TABLE " + TABLE_OPERACOES + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "operacao TEXT);";

    private static final String CREATE_TABLE_LASTROS_LIQUIDOS =
            "CREATE TABLE " + TABLE_LASTROS_LIQUIDOS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "tipo TEXT, " +
                    "rodado TEXT, " +
                    "valor_agua INTEGER);";

    private static final String CREATE_TABLE_RODADOS_DIANTEIROS =
            "CREATE TABLE " + TABLE_RODADOS_DIANTEIROS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "rodado TEXT, " +
                    "grupo INTEGER);";

    private static final String CREATE_TABLE_RODADOS_TRASEIROS =
            "CREATE TABLE " + TABLE_RODADOS_TRASEIROS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "rodado TEXT, " +
                    "grupo INTEGER);";

    private static final String CREATE_TABLE_HISTORICO_BALANCOS =
            "CREATE TABLE " + TABLE_HISTORICO_BALANCOS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "data_criacao TEXT, " +
                    "serie_trator TEXT, " +
                    "modelo TEXT, " +
                    "operacao TEXT, " +
                    "ponto_engate TEXT, " +
                    "rodado_dianteiro TEXT, " +
                    "rodado_traseiro TEXT, " +
                    "peso_total REAL, " +
                    "peso_dianteiro REAL, " +
                    "peso_traseiro REAL, " +
                    "divisao_dianteira REAL, " +
                    "divisao_traseira REAL, " +
                    "peso_por_cv REAL, " +
                    "peso_por_kw REAL, " +
                    "consumo_diesel REAL, " +
                    "json_dados_completos TEXT);";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TRATORES);
        db.execSQL(CREATE_TABLE_PONTO_ENGATE);
        db.execSQL(CREATE_TABLE_OPERACOES);
        db.execSQL(CREATE_TABLE_LASTROS_LIQUIDOS);
        db.execSQL(CREATE_TABLE_RODADOS_DIANTEIROS);
        db.execSQL(CREATE_TABLE_RODADOS_TRASEIROS);
        db.execSQL(CREATE_TABLE_HISTORICO_BALANCOS);
        inserirDadosIniciais(db);
    }

    private void inserirDadosIniciais(SQLiteDatabase db) {
        // TRATORES ATUALIZADOS COM OS NOVOS DADOS ↓
        inserirTrator(db, "8250R", 250, 210, 14296);
        inserirTrator(db, "8270R", 270, 227, 14696);
        inserirTrator(db, "8295R", 295, 248, 16192);
        inserirTrator(db, "8320R", 320, 269, 17600);
        inserirTrator(db, "8335R", 335, 281, 18425);
        inserirTrator(db, "8345R", 345, 290, 19008);
        inserirTrator(db, "8370R", 370, 311, 19805);
        inserirTrator(db, "8400R", 400, 336, 19805);

        // Ponto de engate (PERMANECE IGUAL)
        inserirPontoEngate(db, "3 PONTOS");
        inserirPontoEngate(db, "B.T.O");

        // Operações (PERMANECE IGUAL)
        inserirOperacao(db, "LEVE");
        inserirOperacao(db, "MÉDIO");
        inserirOperacao(db, "PESADO");

        // Rodados dianteiros (PERMANECE IGUAL)
        inserirRodadoDianteiro(db, "320/85R38", 42);
        inserirRodadoDianteiro(db, "14.9R34", 42);
        inserirRodadoDianteiro(db, "420/90R30", 42);
        inserirRodadoDianteiro(db, "16.9R30", 42);
        inserirRodadoDianteiro(db, "420/85R34", 43);
        inserirRodadoDianteiro(db, "480/70R30", 42);
        inserirRodadoDianteiro(db, "480/70R34", 43);
        inserirRodadoDianteiro(db, "540/65R34", 43);
        inserirRodadoDianteiro(db, "600/70R30", 43);
        inserirRodadoDianteiro(db, "LSW1000/40R32", 0);
        inserirRodadoDianteiro(db, "620/75R30", 44);
        inserirRodadoDianteiro(db, "600/70R28", 42);
        inserirRodadoDianteiro(db, "320/80R42", 43);
        inserirRodadoDianteiro(db, "570/75R34", 44);

        // Rodados traseiros (PERMANECE IGUAL)
        inserirRodadoTraseiro(db, "380/90R50", 47);
        inserirRodadoTraseiro(db, "380/90R54", 48);
        inserirRodadoTraseiro(db, "480/80R50", 48);
        inserirRodadoTraseiro(db, "520/85R42", 47);
        inserirRodadoTraseiro(db, "520/85R42 162A8", 47);
        inserirRodadoTraseiro(db, "620/70R42", 47);
        inserirRodadoTraseiro(db, "620/70R46", 47);
        inserirRodadoTraseiro(db, "710/70R38", 47);
        inserirRodadoTraseiro(db, "710/70R42", 48);
        inserirRodadoTraseiro(db, "800/70R38", 48);
        inserirRodadoTraseiro(db, "850/60-38", 0);
        inserirRodadoTraseiro(db, "LSW1100/45R46", 0);
        inserirRodadoTraseiro(db, "14.9R50", 47);
        inserirRodadoTraseiro(db, "18.4R46", 47);
        inserirRodadoTraseiro(db, "20.8R42", 47);
        inserirRodadoTraseiro(db, "520/85R46", 48);

        // Lastros líquidos dianteiros (PERMANECE IGUAL)
        inserirLastro(db, "DIANTEIRO", "320/85R38", 172);
        inserirLastro(db, "DIANTEIRO", "14.9R34", 0);
        inserirLastro(db, "DIANTEIRO", "420/90R30", 334);
        inserirLastro(db, "DIANTEIRO", "16.9R30", 0);
        inserirLastro(db, "DIANTEIRO", "420/85R34", 328);
        inserirLastro(db, "DIANTEIRO", "480/70R30", 172);
        inserirLastro(db, "DIANTEIRO", "480/70R34", 176);
        inserirLastro(db, "DIANTEIRO", "540/65R34", 156);
        inserirLastro(db, "DIANTEIRO", "600/70R30", 245);
        inserirLastro(db, "DIANTEIRO", "LSW1000/40R32", 754);
        inserirLastro(db, "DIANTEIRO", "620/75R30", 700);
        inserirLastro(db, "DIANTEIRO", "600/70R28", 448);
        inserirLastro(db, "DIANTEIRO", "320/80R42", 0);
        inserirLastro(db, "DIANTEIRO", "570/75R34", 0);

        // Lastros líquidos traseiros (PERMANECE IGUAL)
        inserirLastro(db, "TRASEIRO", "380/90R50", 195);
        inserirLastro(db, "TRASEIRO", "380/90R54", 194);
        inserirLastro(db, "TRASEIRO", "480/80R50", 288);
        inserirLastro(db, "TRASEIRO", "520/85R42", 349);
        inserirLastro(db, "TRASEIRO", "520/85R42 162A8", 375);
        inserirLastro(db, "TRASEIRO", "620/70R42", 411);
        inserirLastro(db, "TRASEIRO", "620/70R46", 421);
        inserirLastro(db, "TRASEIRO", "710/70R38", 909);
        inserirLastro(db, "TRASEIRO", "710/70R42", 909);
        inserirLastro(db, "TRASEIRO", "800/70R38", 1087);
        inserirLastro(db, "TRASEIRO", "850/60-38", 650);
        inserirLastro(db, "TRASEIRO", "LSW1100/45R46", 1484);
        inserirLastro(db, "TRASEIRO", "14.9R50", 0);
        inserirLastro(db, "TRASEIRO", "18.4R46", 0);
        inserirLastro(db, "TRASEIRO", "20.8R42", 0);
        inserirLastro(db, "TRASEIRO", "520/85R46", 0);
    }

    // MÉTODO INSERIR TRATOR ATUALIZADO ↓
    private void inserirTrator(SQLiteDatabase db, String modelo, int potencia, double potenciaKW, double pesoMaximo) {
        ContentValues values = new ContentValues();
        values.put("modelo", modelo);
        values.put("potencia", potencia);
        values.put("potencia_kw", potenciaKW);
        values.put("peso_maximo", pesoMaximo);
        db.insert(TABLE_TRATORES, null, values);
    }

    // MÉTODOS EXISTENTES (PERMANECEM IGUAIS) ↓
    private void inserirPontoEngate(SQLiteDatabase db, String ponto) {
        ContentValues values = new ContentValues();
        values.put("ponto", ponto);
        db.insert(TABLE_PONTO_ENGATE, null, values);
    }

    private void inserirOperacao(SQLiteDatabase db, String operacao) {
        ContentValues values = new ContentValues();
        values.put("operacao", operacao);
        db.insert(TABLE_OPERACOES, null, values);
    }

    private void inserirLastro(SQLiteDatabase db, String tipo, String rodado, int valorAgua) {
        ContentValues values = new ContentValues();
        values.put("tipo", tipo);
        values.put("rodado", rodado);
        values.put("valor_agua", valorAgua);
        db.insert(TABLE_LASTROS_LIQUIDOS, null, values);
    }

    private void inserirRodadoDianteiro(SQLiteDatabase db, String rodado, int grupo) {
        ContentValues values = new ContentValues();
        values.put("rodado", rodado);
        values.put("grupo", grupo);
        db.insert(TABLE_RODADOS_DIANTEIROS, null, values);
    }

    private void inserirRodadoTraseiro(SQLiteDatabase db, String rodado, int grupo) {
        ContentValues values = new ContentValues();
        values.put("rodado", rodado);
        values.put("grupo", grupo);
        db.insert(TABLE_RODADOS_TRASEIROS, null, values);
    }

    // MÉTODOS DE CONSULTA (PERMANECEM IGUAIS) ↓
    public int getGrupoRodadoDianteiro(String rodado) {
        SQLiteDatabase db = this.getReadableDatabase();
        int grupo = 0;
        Cursor cursor = db.rawQuery("SELECT grupo FROM " + TABLE_RODADOS_DIANTEIROS + " WHERE rodado = ?", new String[]{rodado});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                grupo = cursor.getInt(0);
            }
            cursor.close();
        }
        db.close();
        return grupo;
    }

    public int getGrupoRodadoTraseiro(String rodado) {
        SQLiteDatabase db = this.getReadableDatabase();
        int grupo = 0;
        Cursor cursor = db.rawQuery("SELECT grupo FROM " + TABLE_RODADOS_TRASEIROS + " WHERE rodado = ?", new String[]{rodado});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                grupo = cursor.getInt(0);
            }
            cursor.close();
        }
        db.close();
        return grupo;
    }

    public int getLastro(String tipo, String rodado) {
        SQLiteDatabase db = this.getReadableDatabase();
        int valor = 0;
        Cursor cursor = db.rawQuery("SELECT valor_agua FROM " + TABLE_LASTROS_LIQUIDOS + " WHERE tipo = ? AND rodado = ?", new String[]{tipo, rodado});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                valor = cursor.getInt(0);
            }
            cursor.close();
        }
        db.close();
        return valor;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // SE a versão antiga for 1, adicionamos as novas colunas
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_TRATORES + " ADD COLUMN potencia_kw REAL");
            db.execSQL("ALTER TABLE " + TABLE_TRATORES + " ADD COLUMN peso_maximo REAL");

            // Atualizar os dados existentes com os novos valores
            atualizarDadosTratores(db);
        }
        if (oldVersion < 3) {
            db.execSQL(CREATE_TABLE_HISTORICO_BALANCOS);
        }
    }

    // MÉTODO PARA ATUALIZAR OS DADOS EXISTENTES
    private void atualizarDadosTratores(SQLiteDatabase db) {
        // Dados dos tratores: modelo -> [potencia_kw, peso_maximo]
        Object[][] dadosTratores = {
                {"8250R", 210, 14296},
                {"8270R", 227, 14696},
                {"8295R", 248, 16192},
                {"8320R", 269, 17600},
                {"8335R", 281, 18425},
                {"8345R", 290, 19008},
                {"8370R", 311, 19805},
                {"8400R", 336, 19805}
        };

        for (Object[] trator : dadosTratores) {
            ContentValues values = new ContentValues();
            values.put("potencia_kw", (Double) trator[1]);
            values.put("peso_maximo", (Double) trator[2]);

            db.update(TABLE_TRATORES, values, "modelo = ?", new String[]{(String) trator[0]});
        }
    }

    // ──────────────── Histórico de Balanços ────────────────

    public long salvarBalanco(DadosJardulli dados, double pesoTotal, double pesoDianteiro,
                              double pesoTraseiro, double divisaoDianteira, double divisaoTraseira,
                              double pesoPorCV, double pesoPorKW, double consumoDiesel) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        values.put("data_criacao", sdf.format(new Date()));
        values.put("serie_trator", dados.getSerieTrator());
        values.put("modelo", dados.getModelo());
        values.put("operacao", dados.getOperacao());
        values.put("ponto_engate", dados.getPontoEngate());
        values.put("rodado_dianteiro", dados.getRodadoDianteiro());
        values.put("rodado_traseiro", dados.getRodadoTraseiro());
        values.put("peso_total", pesoTotal);
        values.put("peso_dianteiro", pesoDianteiro);
        values.put("peso_traseiro", pesoTraseiro);
        values.put("divisao_dianteira", divisaoDianteira);
        values.put("divisao_traseira", divisaoTraseira);
        values.put("peso_por_cv", pesoPorCV);
        values.put("peso_por_kw", pesoPorKW);
        values.put("consumo_diesel", consumoDiesel);
        values.put("json_dados_completos", dadosToJson(dados));

        long id = db.insert(TABLE_HISTORICO_BALANCOS, null, values);
        db.close();
        return id;
    }

    public List<ContentValues> listarBalancos() {
        List<ContentValues> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, data_criacao, serie_trator, modelo, operacao, peso_total, " +
                        "peso_dianteiro, peso_traseiro, divisao_dianteira, divisao_traseira " +
                        "FROM " + TABLE_HISTORICO_BALANCOS + " ORDER BY data_criacao DESC", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                ContentValues item = new ContentValues();
                item.put("id", cursor.getLong(0));
                item.put("data_criacao", cursor.getString(1));
                item.put("serie_trator", cursor.getString(2));
                item.put("modelo", cursor.getString(3));
                item.put("operacao", cursor.getString(4));
                item.put("peso_total", cursor.getDouble(5));
                item.put("peso_dianteiro", cursor.getDouble(6));
                item.put("peso_traseiro", cursor.getDouble(7));
                item.put("divisao_dianteira", cursor.getDouble(8));
                item.put("divisao_traseira", cursor.getDouble(9));
                lista.add(item);
            }
            cursor.close();
        }
        db.close();
        return lista;
    }

    public ContentValues getBalanco(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues result = null;
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_HISTORICO_BALANCOS + " WHERE id = ?",
                new String[]{String.valueOf(id)});
        if (cursor != null && cursor.moveToFirst()) {
            result = new ContentValues();
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                String colName = cursor.getColumnName(i);
                switch (cursor.getType(i)) {
                    case Cursor.FIELD_TYPE_INTEGER:
                        result.put(colName, cursor.getLong(i));
                        break;
                    case Cursor.FIELD_TYPE_FLOAT:
                        result.put(colName, cursor.getDouble(i));
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        result.put(colName, cursor.getString(i));
                        break;
                }
            }
            cursor.close();
        }
        db.close();
        return result;
    }

    public boolean deletarBalanco(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_HISTORICO_BALANCOS, "id = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

    public int contarBalancosMes(String anoMes) {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_HISTORICO_BALANCOS +
                        " WHERE data_criacao LIKE ?", new String[]{anoMes + "%"});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        db.close();
        return count;
    }

    private String dadosToJson(DadosJardulli dados) {
        try {
            JSONObject json = new JSONObject();
            json.put("serieTrator", dados.getSerieTrator());
            json.put("modelo", dados.getModelo());
            json.put("operacao", dados.getOperacao());
            json.put("pontoEngate", dados.getPontoEngate());
            json.put("rodadoDianteiro", dados.getRodadoDianteiro());
            json.put("rodadoTraseiro", dados.getRodadoTraseiro());
            json.put("pesoTrator", dados.getPesoTrator());
            json.put("potenciaKW", dados.getPotenciaKW());
            json.put("pesoMaximo", dados.getPesoMaximo());
            json.put("pesoOperador", dados.getPesoOperador());
            json.put("capacidadeDiesel", dados.getCapacidadeDiesel());
            json.put("lastroDianteiro", dados.getLastroDianteiro());
            json.put("lastroTraseiro", dados.getLastroTraseiro());
            json.put("pesoFrontal", dados.getPesoFrontal());
            json.put("pesoEixoDianteiro", dados.getPesoEixoDianteiro());
            json.put("pesoEixoTraseiro", dados.getPesoEixoTraseiro());
            json.put("quantidadePesos91", dados.getQuantidadePesos91());
            json.put("quantidadePesos227", dados.getQuantidadePesos227());
            json.put("quantidadePesos454", dados.getQuantidadePesos454());
            json.put("quantidadePesos625", dados.getQuantidadePesos625());
            return json.toString();
        } catch (Exception e) {
            return "{}";
        }
    }
}