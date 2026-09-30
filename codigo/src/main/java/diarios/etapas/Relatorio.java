package diarios.etapas;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Relatorio {

    private final String pastaSaida;

    public Relatorio(String pastaSaida) {
        this.pastaSaida = pastaSaida;
    }

    public String gravar(String nomeArquivo, ArrayList<String> linhas) throws IOException {
        Path pasta = Paths.get(pastaSaida);
        if (!Files.exists(pasta)) {
            Files.createDirectories(pasta);
        }
        Path destino = pasta.resolve(nomeArquivo);
        try (BufferedWriter escritor = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            for (String l : linhas) {
                escritor.write(l);
                escritor.newLine();
            }
        }
        return destino.toString();
    }
}
