package com.example.jardullicalculadora;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class HistoricoAdapter extends RecyclerView.Adapter<HistoricoAdapter.ViewHolder> {

    public static class BalancoItem {
        public long id;             // SQLite local row id (0 quando vier só da nuvem)
        public String remoteId;     // UUID da row no Supabase (null quando só local)
        public String data;
        public String serie;
        public String modelo;
        public String operacao;
        public double pesoTotal;
    }

    public interface OnDeleteListener {
        void onDelete(BalancoItem item);
    }

    private final List<BalancoItem> items;
    private final OnDeleteListener deleteListener;

    public HistoricoAdapter(List<BalancoItem> items, OnDeleteListener deleteListener) {
        this.items = items;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historico, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BalancoItem item = items.get(position);
        holder.txtData.setText(item.data);
        holder.txtTrator.setText(item.serie + " - " + item.modelo);

        String pesoFormatado = String.format(Locale.getDefault(), "%,.0f", item.pesoTotal)
                .replace(",", "X").replace(".", ",").replace("X", ".");
        holder.txtDetalhes.setText("Peso Total: " + pesoFormatado + " kg | " + item.operacao);

        holder.btnDeletar.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtData;
        TextView txtTrator;
        TextView txtDetalhes;
        ImageButton btnDeletar;

        ViewHolder(View view) {
            super(view);
            txtData = view.findViewById(R.id.txtData);
            txtTrator = view.findViewById(R.id.txtTrator);
            txtDetalhes = view.findViewById(R.id.txtDetalhes);
            btnDeletar = view.findViewById(R.id.btnDeletar);
        }
    }
}
