package diarios.etapas;

import diarios.modelo.Diario;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public class Ingestao {

    private final String pasta;

    public Ingestao(String pasta) {
        this.pasta = pasta;
    }

    public ArrayList<Diario> lerTodos(ArrayList<String> falhas) {
        ArrayList<Diario> lidos = new ArrayList<>();
        ArrayList<Path> arquivos = new ArrayList<>();

        try (DirectoryStream<Path> fluxo = Files.newDirectoryStream(Paths.get(pasta), "*.txt")) {
            for (Path p : fluxo) {
                arquivos.add(p);
            }
        } catch (IOException e) {
            falhas.add("nao foi possivel abrir a pasta '" + pasta + "': " + e.getMessage());
            return lidos;
        }

        Collections.sort(arquivos);

        for (Path p : arquivos) {
            String nome = p.getFileName().toString();
            try {
                lidos.add(new Diario(nome, municipioDe(nome), dataDe(nome),
                        Files.readString(p, StandardCharsets.UTF_8)));
            } catch (IOException e) {
                falhas.add(nome + ": falha de leitura - " + e.getMessage());
            }
        }
        return lidos;
    }

    public static String municipioDe(String nomeArquivo) {
        int sep = nomeArquivo.indexOf('_');
        return sep > 0 ? nomeArquivo.substring(0, sep) : "desconhecido";
    }

    public static String dataDe(String nomeArquivo) {
        int sep = nomeArquivo.indexOf('_');
        if (sep < 0 || nomeArquivo.length() < sep + 11) {
            return "0000-00-00";
        }
        return nomeArquivo.substring(sep + 1, sep + 11);
    }
}
