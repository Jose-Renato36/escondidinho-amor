package diarios.coleta;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Fontes {

    private static final String ARQUIVO = "fontes.txt";

    private final Path arquivo;
    private final ArrayList<EstadoFonte> estados = new ArrayList<>();

    public Fontes(String pastaEstado) {
        this.arquivo = Paths.get(pastaEstado, ARQUIVO);
        Fonte catalogo = new Fonte();
        boolean temArquivo = Files.exists(arquivo);
        ArrayList<Municipio> padrao = catalogo.padrao();

        for (Municipio m : catalogo.todos()) {
            estados.add(new EstadoFonte(m, !temArquivo && contem(padrao, m.getCodigo()), false));
        }
        if (temArquivo) {
            carregar();
        }
    }

    public synchronized ArrayList<EstadoFonte> todas() {
        return new ArrayList<>(estados);
    }

    public synchronized ArrayList<EstadoFonte> ligadas() {
        ArrayList<EstadoFonte> lista = new ArrayList<>();
        for (EstadoFonte e : estados) {
            if (e.isLigada()) {
                lista.add(e);
            }
        }
        return lista;
    }

    public synchronized EstadoFonte doCodigo(String codigo) {
        for (EstadoFonte e : estados) {
            if (e.getMunicipio().getCodigo().equals(codigo)) {
                return e;
            }
        }
        return null;
    }

    public synchronized void alternar(String codigo) {
        EstadoFonte e = doCodigo(codigo);
        if (e != null) {
            e.setLigada(!e.isLigada());
            salvar();
        }
    }

    public synchronized void ligarTodas(boolean ligar) {
        for (EstadoFonte e : estados) {
            e.setLigada(ligar);
        }
        salvar();
    }

    public synchronized int adicionarDescobertas(ArrayList<Municipio> achados, String quando) {
        int novas = 0;
        for (Municipio m : achados) {
            EstadoFonte existente = doCodigo(m.getCodigo());
            if (existente == null) {
                existente = new EstadoFonte(m, false, true);
                estados.add(existente);
                novas++;
            }
            existente.marcarComoVistaNaDescoberta(quando);
        }
        salvar();
        return novas;
    }

    public synchronized void atualizarAcervo(String pastaDiarios) {
        for (EstadoFonte e : estados) {
            e.setDiariosNoAcervo(contarArquivos(pastaDiarios, e.getMunicipio().getApelido()));
        }
    }

    public synchronized void salvar() {
        ArrayList<String> linhas = new ArrayList<>();
        for (EstadoFonte e : estados) {
            Municipio m = e.getMunicipio();
            linhas.add(String.join(";", m.getCodigo(), limpo(m.getNome()), m.getUf(),
                    String.valueOf(e.isLigada()), e.getStatus(), limpo(e.getUltimaTentativa()),
                    limpo(e.getMotivo()), String.valueOf(e.isDescoberta()),
                    e.getCoberturaDesde(), e.getCoberturaAte()));
        }
        try {
            Files.createDirectories(arquivo.getParent());
            Files.write(arquivo, linhas, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.out.println("aviso: nao consegui salvar " + arquivo + ": " + ex.getMessage());
        }
    }

    private void carregar() {
        try {
            for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                String[] c = linha.split(";", -1);
                if (c.length < 8) {
                    continue;
                }
                EstadoFonte e = doCodigo(c[0]);
                if (e == null) {
                    e = new EstadoFonte(new Municipio(c[0], c[1], c[2]), false, true);
                    estados.add(e);
                }
                e.setLigada(Boolean.parseBoolean(c[3]));
                e.restaurar(c[4], c[5], c[6]);
                if (c.length >= 10) {
                    e.restaurarCobertura(c[8], c[9]);
                }
            }
        } catch (IOException ex) {
            System.out.println("aviso: nao consegui ler " + arquivo + ": " + ex.getMessage());
        }
    }

    private int contarArquivos(String pasta, String apelido) {
        int n = 0;
        try (DirectoryStream<Path> fluxo = Files.newDirectoryStream(Paths.get(pasta),
                apelido + "_*.txt")) {
            for (Path p : fluxo) {
                n++;
            }
        } catch (IOException ex) {
            return 0;
        }
        return n;
    }

    private boolean contem(ArrayList<Municipio> lista, String codigo) {
        for (Municipio m : lista) {
            if (m.getCodigo().equals(codigo)) {
                return true;
            }
        }
        return false;
    }

    private String limpo(String s) {
        return s == null ? "" : s.replace(';', ',').replace('\n', ' ');
    }
}
