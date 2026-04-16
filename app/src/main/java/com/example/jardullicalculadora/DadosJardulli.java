package com.example.jardullicalculadora;

import android.os.Parcel;
import android.os.Parcelable;

public class DadosJardulli implements Parcelable {
    // Campos principais usados no app
    private String trator;
    private String operacao;
    private String modelo;
    private String rodadoDianteiro;
    private String rodadoTraseiro;
    private int grupoDianteiro;
    private int grupoTraseiro;
    private int lastroDianteiro;
    private int lastroTraseiro;
    private int pesoTrator;
    private int pesoImplemento;
    private int pesoRodados;
    private int lastroLiquido;
    private int pesoFrontal;

    // Novos campos para armazenar pesos dos eixos
    private int pesoEixoDianteiro;
    private int pesoEixoTraseiro;

    // Campos para PesoFrontalActivity
    private String pesoFrontalStr;
    private String pesoTraseiro72Str;
    private String pesoTraseiro205Str;
    private String pesoTraseiro625Str;
    private String cabideStr;
    private String pesoTotal;

    // NOVOS CAMPOS ADICIONADOS ↓
    private double potenciaKW;
    private double pesoMaximo;

    // NOVO CAMPO PARA PONTO DE ENGATE ↓
    private String pontoEngate;

    // NOVOS CAMPOS ADICIONADOS AGORA ↓
    private String serieTrator;
    private int pesoOperador;

    // NOVOS CAMPOS: Capacidade de Diesel e Distância entre eixos
    private int capacidadeDiesel;
    private double distanciaEntreEixos;

    // NOVOS CAMPOS PARA TIPOS DE EIXO
    private String tipoEixoDianteiro;
    private String tipoEixoTraseiro;

    // NOVOS CAMPOS PARA PESOS TRASEIROS ESPECÍFICOS
    private int quantidadePesos91;
    private int quantidadePesos227;
    private int quantidadePesos454;
    private int quantidadePesos625; // ✅ NOVO CAMPO PARA PESO 4 (625kg)

    // ✅ NOVOS CAMPOS PARA ARMAZENAR OS PESOS REAIS DE CADA SÉRIE
    private int pesoRealTraseiro1;  // Ex: 72kg para John Deere 8R
    private int pesoRealTraseiro2;  // Ex: 205kg para John Deere 8R
    private int pesoRealTraseiro3;  // Ex: 160kg para John Deere 8R
    private int pesoRealTraseiro4;  // Ex: 625kg para John Deere 8R

    // Construtor padrão
    public DadosJardulli() {
    }

    // Getters e Setters para todos os campos
    public String getTrator() { return trator; }
    public void setTrator(String trator) { this.trator = trator; }

    public String getOperacao() { return operacao; }
    public void setOperacao(String operacao) { this.operacao = operacao; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getRodadoDianteiro() { return rodadoDianteiro; }
    public void setRodadoDianteiro(String rodadoDianteiro) { this.rodadoDianteiro = rodadoDianteiro; }

    public String getRodadoTraseiro() { return rodadoTraseiro; }
    public void setRodadoTraseiro(String rodadoTraseiro) { this.rodadoTraseiro = rodadoTraseiro; }

    public int getGrupoDianteiro() { return grupoDianteiro; }
    public void setGrupoDianteiro(int grupoDianteiro) { this.grupoDianteiro = grupoDianteiro; }

    public int getGrupoTraseiro() { return grupoTraseiro; }
    public void setGrupoTraseiro(int grupoTraseiro) { this.grupoTraseiro = grupoTraseiro; }

    public int getLastroDianteiro() { return lastroDianteiro; }
    public void setLastroDianteiro(int lastroDianteiro) { this.lastroDianteiro = lastroDianteiro; }

    public int getLastroTraseiro() { return lastroTraseiro; }
    public void setLastroTraseiro(int lastroTraseiro) { this.lastroTraseiro = lastroTraseiro; }

    public int getPesoTrator() { return pesoTrator; }
    public void setPesoTrator(int pesoTrator) { this.pesoTrator = pesoTrator; }

    public int getPesoImplemento() { return pesoImplemento; }
    public void setPesoImplemento(int pesoImplemento) { this.pesoImplemento = pesoImplemento; }

    public int getPesoRodados() { return pesoRodados; }
    public void setPesoRodados(int pesoRodados) { this.pesoRodados = pesoRodados; }

    public int getLastroLiquido() { return lastroLiquido; }
    public void setLastroLiquido(int lastroLiquido) { this.lastroLiquido = lastroLiquido; }

    public int getPesoFrontal() { return pesoFrontal; }
    public void setPesoFrontal(int pesoFrontal) { this.pesoFrontal = pesoFrontal; }

    // Getters e Setters para os pesos dos eixos
    public int getPesoEixoDianteiro() { return pesoEixoDianteiro; }
    public void setPesoEixoDianteiro(int pesoEixoDianteiro) { this.pesoEixoDianteiro = pesoEixoDianteiro; }

    public int getPesoEixoTraseiro() { return pesoEixoTraseiro; }
    public void setPesoEixoTraseiro(int pesoEixoTraseiro) { this.pesoEixoTraseiro = pesoEixoTraseiro; }

    // Getters e Setters para os campos de string
    public String getPesoFrontalStr() { return pesoFrontalStr; }
    public void setPesoFrontalStr(String pesoFrontalStr) { this.pesoFrontalStr = pesoFrontalStr; }

    public String getPesoTraseiro72Str() { return pesoTraseiro72Str; }
    public void setPesoTraseiro72Str(String pesoTraseiro72Str) { this.pesoTraseiro72Str = pesoTraseiro72Str; }

    public String getPesoTraseiro205Str() { return pesoTraseiro205Str; }
    public void setPesoTraseiro205Str(String pesoTraseiro205Str) { this.pesoTraseiro205Str = pesoTraseiro205Str; }

    public String getPesoTraseiro625Str() { return pesoTraseiro625Str; }
    public void setPesoTraseiro625Str(String pesoTraseiro625Str) { this.pesoTraseiro625Str = pesoTraseiro625Str; }

    public String getCabideStr() { return cabideStr; }
    public void setCabideStr(String cabideStr) { this.cabideStr = cabideStr; }

    public String getPesoTotal() { return pesoTotal; }
    public void setPesoTotal(String pesoTotal) { this.pesoTotal = pesoTotal; }

    // NOVOS GETTERS E SETTERS ADICIONADOS ↓
    public double getPotenciaKW() { return potenciaKW; }
    public void setPotenciaKW(double potenciaKW) { this.potenciaKW = potenciaKW; }

    public double getPesoMaximo() { return pesoMaximo; }
    public void setPesoMaximo(double pesoMaximo) { this.pesoMaximo = pesoMaximo; }

    // NOVO GETTER/SETTER PARA PONTO DE ENGATE ↓
    public String getPontoEngate() { return pontoEngate; }
    public void setPontoEngate(String pontoEngate) { this.pontoEngate = pontoEngate; }

    // NOVOS GETTERS/SETTERS ADICIONADOS AGORA ↓
    public String getSerieTrator() { return serieTrator; }
    public void setSerieTrator(String serieTrator) { this.serieTrator = serieTrator; }

    public int getPesoOperador() { return pesoOperador; }
    public void setPesoOperador(int pesoOperador) { this.pesoOperador = pesoOperador; }

    // NOVOS GETTERS/SETTERS PARA CAPACIDADE DE DIESEL E DISTÂNCIA ENTRE EIXOS
    public int getCapacidadeDiesel() { return capacidadeDiesel; }
    public void setCapacidadeDiesel(int capacidadeDiesel) { this.capacidadeDiesel = capacidadeDiesel; }

    public double getDistanciaEntreEixos() { return distanciaEntreEixos; }
    public void setDistanciaEntreEixos(double distanciaEntreEixos) { this.distanciaEntreEixos = distanciaEntreEixos; }

    // NOVOS GETTERS E SETTERS PARA TIPOS DE EIXO
    public String getTipoEixoDianteiro() { return tipoEixoDianteiro; }
    public void setTipoEixoDianteiro(String tipoEixoDianteiro) { this.tipoEixoDianteiro = tipoEixoDianteiro; }

    public String getTipoEixoTraseiro() { return tipoEixoTraseiro; }
    public void setTipoEixoTraseiro(String tipoEixoTraseiro) { this.tipoEixoTraseiro = tipoEixoTraseiro; }

    // NOVOS GETTERS E SETTERS PARA PESOS TRASEIROS ESPECÍFICOS
    public int getQuantidadePesos91() { return quantidadePesos91; }
    public void setQuantidadePesos91(int quantidadePesos91) { this.quantidadePesos91 = quantidadePesos91; }

    public int getQuantidadePesos227() { return quantidadePesos227; }
    public void setQuantidadePesos227(int quantidadePesos227) { this.quantidadePesos227 = quantidadePesos227; }

    public int getQuantidadePesos454() { return quantidadePesos454; }
    public void setQuantidadePesos454(int quantidadePesos454) { this.quantidadePesos454 = quantidadePesos454; }

    public int getQuantidadePesos625() { return quantidadePesos625; }
    public void setQuantidadePesos625(int quantidadePesos625) { this.quantidadePesos625 = quantidadePesos625; }

    // ✅ NOVOS GETTERS E SETTERS PARA PESOS REAIS
    public int getPesoRealTraseiro1() { return pesoRealTraseiro1; }
    public void setPesoRealTraseiro1(int pesoRealTraseiro1) { this.pesoRealTraseiro1 = pesoRealTraseiro1; }

    public int getPesoRealTraseiro2() { return pesoRealTraseiro2; }
    public void setPesoRealTraseiro2(int pesoRealTraseiro2) { this.pesoRealTraseiro2 = pesoRealTraseiro2; }

    public int getPesoRealTraseiro3() { return pesoRealTraseiro3; }
    public void setPesoRealTraseiro3(int pesoRealTraseiro3) { this.pesoRealTraseiro3 = pesoRealTraseiro3; }

    public int getPesoRealTraseiro4() { return pesoRealTraseiro4; }
    public void setPesoRealTraseiro4(int pesoRealTraseiro4) { this.pesoRealTraseiro4 = pesoRealTraseiro4; }

    // ✅ MÉTODO CONVENIENTE PARA SALVAR TODOS OS PESOS DE UMA VEZ
    public void setPesosTraseirosReais(int peso1, int peso2, int peso3, int peso4) {
        this.pesoRealTraseiro1 = peso1;
        this.pesoRealTraseiro2 = peso2;
        this.pesoRealTraseiro3 = peso3;
        this.pesoRealTraseiro4 = peso4;
    }

    // Parcelable
    protected DadosJardulli(Parcel in) {
        trator = in.readString();
        operacao = in.readString();
        modelo = in.readString();
        rodadoDianteiro = in.readString();
        rodadoTraseiro = in.readString();
        grupoDianteiro = in.readInt();
        grupoTraseiro = in.readInt();
        lastroDianteiro = in.readInt();
        lastroTraseiro = in.readInt();
        pesoTrator = in.readInt();
        pesoImplemento = in.readInt();
        pesoRodados = in.readInt();
        lastroLiquido = in.readInt();
        pesoFrontal = in.readInt();
        pesoEixoDianteiro = in.readInt();
        pesoEixoTraseiro = in.readInt();
        pesoFrontalStr = in.readString();
        pesoTraseiro72Str = in.readString();
        pesoTraseiro205Str = in.readString();
        pesoTraseiro625Str = in.readString();
        cabideStr = in.readString();
        pesoTotal = in.readString();
        potenciaKW = in.readDouble();
        pesoMaximo = in.readDouble();
        pontoEngate = in.readString();
        serieTrator = in.readString();
        pesoOperador = in.readInt();
        capacidadeDiesel = in.readInt();
        distanciaEntreEixos = in.readDouble();
        tipoEixoDianteiro = in.readString();
        tipoEixoTraseiro = in.readString();
        quantidadePesos91 = in.readInt();
        quantidadePesos227 = in.readInt();
        quantidadePesos454 = in.readInt();
        quantidadePesos625 = in.readInt(); // ✅ LER NOVO CAMPO
        pesoRealTraseiro1 = in.readInt();
        pesoRealTraseiro2 = in.readInt();
        pesoRealTraseiro3 = in.readInt();
        pesoRealTraseiro4 = in.readInt();
    }

    public static final Creator<DadosJardulli> CREATOR = new Creator<DadosJardulli>() {
        @Override
        public DadosJardulli createFromParcel(Parcel in) {
            return new DadosJardulli(in);
        }

        @Override
        public DadosJardulli[] newArray(int size) {
            return new DadosJardulli[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(trator);
        dest.writeString(operacao);
        dest.writeString(modelo);
        dest.writeString(rodadoDianteiro);
        dest.writeString(rodadoTraseiro);
        dest.writeInt(grupoDianteiro);
        dest.writeInt(grupoTraseiro);
        dest.writeInt(lastroDianteiro);
        dest.writeInt(lastroTraseiro);
        dest.writeInt(pesoTrator);
        dest.writeInt(pesoImplemento);
        dest.writeInt(pesoRodados);
        dest.writeInt(lastroLiquido);
        dest.writeInt(pesoFrontal);
        dest.writeInt(pesoEixoDianteiro);
        dest.writeInt(pesoEixoTraseiro);
        dest.writeString(pesoFrontalStr);
        dest.writeString(pesoTraseiro72Str);
        dest.writeString(pesoTraseiro205Str);
        dest.writeString(pesoTraseiro625Str);
        dest.writeString(cabideStr);
        dest.writeString(pesoTotal);
        dest.writeDouble(potenciaKW);
        dest.writeDouble(pesoMaximo);
        dest.writeString(pontoEngate);
        dest.writeString(serieTrator);
        dest.writeInt(pesoOperador);
        dest.writeInt(capacidadeDiesel);
        dest.writeDouble(distanciaEntreEixos);
        dest.writeString(tipoEixoDianteiro);
        dest.writeString(tipoEixoTraseiro);
        dest.writeInt(quantidadePesos91);
        dest.writeInt(quantidadePesos227);
        dest.writeInt(quantidadePesos454);
        dest.writeInt(quantidadePesos625); // ✅ ESCREVER NOVO CAMPO
        dest.writeInt(pesoRealTraseiro1);
        dest.writeInt(pesoRealTraseiro2);
        dest.writeInt(pesoRealTraseiro3);
        dest.writeInt(pesoRealTraseiro4);
    }

    // Método toString para logging
    @Override
    public String toString() {
        return "DadosJardulli{" +
                "trator='" + trator + '\'' +
                ", operacao='" + operacao + '\'' +
                ", modelo='" + modelo + '\'' +
                ", rodadoDianteiro='" + rodadoDianteiro + '\'' +
                ", rodadoTraseiro='" + rodadoTraseiro + '\'' +
                ", pesoEixoDianteiro=" + pesoEixoDianteiro +
                ", pesoEixoTraseiro=" + pesoEixoTraseiro +
                ", potenciaKW=" + potenciaKW +
                ", pesoMaximo=" + pesoMaximo +
                ", pontoEngate='" + pontoEngate + '\'' +
                ", serieTrator='" + serieTrator + '\'' +
                ", pesoOperador=" + pesoOperador +
                ", capacidadeDiesel=" + capacidadeDiesel +
                ", distanciaEntreEixos=" + distanciaEntreEixos +
                ", tipoEixoDianteiro='" + tipoEixoDianteiro + '\'' +
                ", tipoEixoTraseiro='" + tipoEixoTraseiro + '\'' +
                ", quantidadePesos91=" + quantidadePesos91 +
                ", quantidadePesos227=" + quantidadePesos227 +
                ", quantidadePesos454=" + quantidadePesos454 +
                ", quantidadePesos625=" + quantidadePesos625 + // ✅ INCLUIR NO toString
                ", pesoRealTraseiro1=" + pesoRealTraseiro1 +
                ", pesoRealTraseiro2=" + pesoRealTraseiro2 +
                ", pesoRealTraseiro3=" + pesoRealTraseiro3 +
                ", pesoRealTraseiro4=" + pesoRealTraseiro4 +
                '}';
    }
}