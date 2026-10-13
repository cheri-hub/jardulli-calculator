package com.example.jardullicalculadora;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Rodapé "Powered by Agrosinergia". Soma a altura da barra de navegação ao
 * espaçamento inferior, para não ficar escondido quando o app desenha por baixo
 * das barras do sistema (Android 15+, targetSdk 35). Onde o sistema já reserva a
 * área das barras, o inset chega zerado e nada muda.
 */
public class RodapeAgrosinergia extends AppCompatTextView {

    public RodapeAgrosinergia(Context context, AttributeSet attrs) {
        super(context, attrs);
        final int paddingBase = getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(this, (view, insets) -> {
            int barraInferior = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), paddingBase + barraInferior);
            return insets;
        });
    }
}
