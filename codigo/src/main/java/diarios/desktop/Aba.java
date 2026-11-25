package diarios.desktop;

import javafx.scene.control.Tab;

/** Contrato de toda aba da janela: ela se monta uma vez e se atualiza quando os dados mudam. */
public interface Aba {

    Tab getTab();

    /** Chamada na thread da interface sempre que o processamento termina. */
    void atualizar();
}
