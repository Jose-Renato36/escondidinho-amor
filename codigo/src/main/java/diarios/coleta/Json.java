package diarios.coleta;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;

/**
 * Leitura das respostas JSON da API. Usa o Jackson; esta classe so mantem a interface
 * simples que o Coletor ja usava (objetos de uma lista, campo de um objeto como texto).
 * JSON malformado nao derruba a coleta: vira lista vazia ou texto vazio.
 */
public class Json {

    private static final ObjectMapper LEITOR = new ObjectMapper();

    /** Cada elemento (objeto) da lista com esse nome, devolvido como texto JSON. */
    public static ArrayList<String> objetos(String resposta, String lista) {
        ArrayList<String> partes = new ArrayList<>();
        try {
            for (JsonNode objeto : LEITOR.readTree(resposta).path(lista)) {
                if (objeto.isObject()) {
                    partes.add(objeto.toString());
                }
            }
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
        return partes;
    }

    /** O valor do campo como texto; campo ausente ou null vira "". */
    public static String campo(String objeto, String chave) {
        try {
            JsonNode valor = LEITOR.readTree(objeto).path(chave);
            return valor.isValueNode() && !valor.isNull() ? valor.asText() : "";
        } catch (JsonProcessingException e) {
            return "";
        }
    }
}
