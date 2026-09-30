package diarios.etapas;

import diarios.extratores.Extrator;
import diarios.modelo.Ato;
import diarios.modelo.Diario;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class Visualizador {

    private static final int LINHAS_POR_PAGINA = 30;

    private final Scanner teclado;

    public Visualizador(Scanner teclado) {
        this.teclado = teclado;
    }

    public void abrir(String pasta, ArrayList<Diario> processados) {
        while (true) {
            ArrayList<Path> arquivos = listar(pasta);
            if (arquivos.isEmpty()) {
                System.out.println("  pasta '" + pasta + "' vazia.");
                return;
            }

            System.out.println();
            System.out.println("  ARQUIVOS EM " + pasta + "/");
            for (int i = 0; i < arquivos.size(); i++) {
                System.out.printf("   %2d - %-42s %s%n", i,
                        arquivos.get(i).getFileName(), tamanho(arquivos.get(i)));
            }
            System.out.print("  numero do arquivo (enter volta): ");

            String entrada = ler();
            if (entrada.isEmpty()) {
                return;
            }
            int indice = numero(entrada, arquivos.size());
            if (indice < 0) {
                System.out.println("  fora da lista.");
                continue;
            }
            mostrarArquivo(arquivos.get(indice), processados);
        }
    }

    private void mostrarArquivo(Path caminho, ArrayList<Diario> processados) {
        String nome = caminho.getFileName().toString();
        Diario processado = acharProcessado(nome, processados);

        while (true) {
            System.out.println();
            System.out.println("  " + nome);
            System.out.println("   1 - Ver o texto como veio (bruto)");
            if (processado != null && processado.isUtilizavel()) {
                System.out.println("   2 - Ver o texto depois da limpeza");
                System.out.println("   3 - Ver um ato separado, inteiro");
                System.out.println("   4 - Procurar uma palavra");
            } else if (processado != null) {
                System.out.println("      (recusado: " + processado.getMotivoRecusa() + ")");
            }
            System.out.print("  escolha (enter volta): ");

            String escolha = ler();
            if (escolha.isEmpty()) {
                return;
            }
            if (escolha.equals("1")) {
                paginar(lerLinhas(caminho), "texto bruto");
            } else if (escolha.equals("2") && processado != null) {
                paginar(quebrar(processado.getTextoLimpo()), "texto limpo");
            } else if (escolha.equals("3") && processado != null) {
                verAto(processado);
            } else if (escolha.equals("4") && processado != null) {
                procurar(processado);
            } else {
                System.out.println("  opcao invalida.");
            }
        }
    }

    private void verAto(Diario diario) {
        ArrayList<Ato> atos = diario.getAtos();
        if (atos.isEmpty()) {
            System.out.println("  nenhum ato neste diario.");
            return;
        }
        System.out.println();
        int mostrar = Math.min(25, atos.size());
        for (int i = 0; i < mostrar; i++) {
            System.out.printf("   %2d - %-9s %-46s%n", i,
                    atos.get(i).getDecisao(), corta(atos.get(i).getTitulo(), 46));
        }
        if (atos.size() > mostrar) {
            System.out.println("   ... e mais " + (atos.size() - mostrar));
        }
        System.out.print("  numero do ato: ");

        int indice = numero(ler(), atos.size());
        if (indice < 0) {
            System.out.println("  fora da lista.");
            return;
        }
        Ato ato = atos.get(indice);

        System.out.println();
        System.out.println("  identidade : " + ato.getIdentidade());
        System.out.println("  decisao    : " + ato.getDecisao() + " - " + ato.getMotivo());
        System.out.println("  fatos      : " + ato.getFatos().size());
        System.out.println("  posicao    : caractere " + ato.getPosicao() + " do diario");
        paginar(quebrar(ato.getTexto()), "texto do ato");
    }

    private void procurar(Diario diario) {
        System.out.print("  palavra: ");
        String palavra = ler();
        if (palavra.isEmpty()) {
            return;
        }
        String alvo = Extrator.semAcento(palavra.toLowerCase());
        ArrayList<String> achados = new ArrayList<>();
        int numeroDaLinha = 0;

        for (String linha : quebrar(diario.getTextoLimpo())) {
            numeroDaLinha++;
            if (Extrator.semAcento(linha.toLowerCase()).contains(alvo)) {
                achados.add(String.format("%5d| %s", numeroDaLinha, linha));
            }
        }
        if (achados.isEmpty()) {
            System.out.println("  nao encontrei '" + palavra + "'.");
            return;
        }
        System.out.println("  " + achados.size() + " linha(s) com '" + palavra + "'");
        paginar(achados, "ocorrencias");
    }

    private void paginar(ArrayList<String> linhas, String titulo) {
        System.out.println();
        System.out.println("  --- " + titulo + " (" + linhas.size() + " linhas) ---");

        int posicao = 0;
        while (posicao < linhas.size()) {
            int fim = Math.min(posicao + LINHAS_POR_PAGINA, linhas.size());
            for (int i = posicao; i < fim; i++) {
                System.out.println("  " + linhas.get(i));
            }
            posicao = fim;
            if (posicao >= linhas.size()) {
                break;
            }
            System.out.print("  -- " + posicao + "/" + linhas.size()
                    + " -- enter continua, 's' sai: ");
            if (ler().equalsIgnoreCase("s")) {
                return;
            }
        }
        System.out.println("  --- fim ---");
    }

    private ArrayList<Path> listar(String pasta) {
        ArrayList<Path> arquivos = new ArrayList<>();
        try (DirectoryStream<Path> fluxo = Files.newDirectoryStream(Paths.get(pasta))) {
            for (Path p : fluxo) {
                if (Files.isRegularFile(p)) {
                    arquivos.add(p);
                }
            }
        } catch (IOException e) {
            System.out.println("  nao consegui abrir '" + pasta + "': " + e.getMessage());
        }
        Collections.sort(arquivos);
        return arquivos;
    }

    private ArrayList<String> lerLinhas(Path caminho) {
        ArrayList<String> linhas = new ArrayList<>();
        try {
            linhas.addAll(Files.readAllLines(caminho, StandardCharsets.UTF_8));
        } catch (IOException e) {
            linhas.add("(nao consegui ler o arquivo: " + e.getMessage() + ")");
        }
        return linhas;
    }

    private ArrayList<String> quebrar(String texto) {
        ArrayList<String> linhas = new ArrayList<>();
        for (String l : texto.split("\n")) {
            linhas.add(l);
        }
        return linhas;
    }

    private Diario acharProcessado(String nome, ArrayList<Diario> processados) {
        for (Diario d : processados) {
            if (d.getArquivo().equals(nome)) {
                return d;
            }
        }
        return null;
    }

    private String tamanho(Path caminho) {
        try {
            long bytes = Files.size(caminho);
            return bytes < 1024 ? bytes + " B" : (bytes / 1024) + " KB";
        } catch (IOException e) {
            return "?";
        }
    }

    private int numero(String entrada, int limite) {
        try {
            int n = Integer.parseInt(entrada.trim());
            return (n >= 0 && n < limite) ? n : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String ler() {
        return teclado.hasNextLine() ? teclado.nextLine().trim() : "";
    }

    private static String corta(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + ".";
    }
}
