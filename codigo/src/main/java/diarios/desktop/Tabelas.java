package diarios.desktop;

import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;

/** Montagem de colunas de tabela, para as abas nao repetirem o mesmo codigo. */
final class Tabelas {

    private Tabelas() {
    }

    /** Coluna de texto: o leitor diz como tirar o texto de cada linha da tabela. */
    static <T> TableColumn<T, String> texto(String nome, double largura, Function<T, String> leitor) {
        TableColumn<T, String> coluna = new TableColumn<>(nome);
        coluna.setPrefWidth(largura);
        coluna.setCellValueFactory(v -> new SimpleStringProperty(leitor.apply(v.getValue())));
        return coluna;
    }
}
