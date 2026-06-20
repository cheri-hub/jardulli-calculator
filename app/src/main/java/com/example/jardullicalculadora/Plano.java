package com.example.jardullicalculadora;

public enum Plano {

    TESTDRIVE("testdrive", 0, 1, false, false, "FAQ"),
    FLEXIVEL("flexivel", 3000, -1, false, true, "EMAIL"),
    PROFISSIONAL("profissional", 15000, -1, true, true, "WHATSAPP"),
    FROTISTA("frotista", 24000, -1, true, true, "PRIORITARIO");

    private final String id;
    private final int precoEmCentavos;
    private final int limiteBalancosMes; // -1 = ilimitado
    private final boolean podePDF;
    private final boolean podeHistorico;
    private final String tipoSuporte;

    Plano(String id, int precoEmCentavos, int limiteBalancosMes,
          boolean podePDF, boolean podeHistorico, String tipoSuporte) {
        this.id = id;
        this.precoEmCentavos = precoEmCentavos;
        this.limiteBalancosMes = limiteBalancosMes;
        this.podePDF = podePDF;
        this.podeHistorico = podeHistorico;
        this.tipoSuporte = tipoSuporte;
    }

    public String getId() { return id; }
    public int getPrecoEmCentavos() { return precoEmCentavos; }
    public int getLimiteBalancosMes() { return limiteBalancosMes; }
    public boolean isPodePDF() { return podePDF; }
    public boolean isPodeHistorico() { return podeHistorico; }
    public String getTipoSuporte() { return tipoSuporte; }

    public boolean isBalancoIlimitado() {
        return limiteBalancosMes == -1;
    }

    public static Plano fromId(String id) {
        if (id == null) return TESTDRIVE;
        for (Plano plano : values()) {
            if (plano.id.equalsIgnoreCase(id)) {
                return plano;
            }
        }
        return TESTDRIVE;
    }
}
