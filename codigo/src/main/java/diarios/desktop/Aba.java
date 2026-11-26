package diarios.desktop;

import javafx.scene.Node;

/** Contrato de toda tela da janela: tem titulo e conteudo, e se atualiza quando os dados mudam. */
public interface Aba {

    String getTitulo();

    Node getConteudo();

    /** Chamada na thread da interface sempre que o processamento termina. */
    void atualizar();
}
