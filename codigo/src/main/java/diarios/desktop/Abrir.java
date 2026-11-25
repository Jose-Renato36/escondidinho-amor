package diarios.desktop;

import javafx.application.Application;

/**
 * Ponto de entrada da janela. Fica separado de Janela porque o Java recusa iniciar uma
 * classe que estende Application quando o JavaFX nao esta no module-path.
 */
public class Abrir {

    public static void main(String[] args) {
        java.util.Locale.setDefault(java.util.Locale.forLanguageTag("pt-BR"));
        Application.launch(Janela.class, args);
    }
}
