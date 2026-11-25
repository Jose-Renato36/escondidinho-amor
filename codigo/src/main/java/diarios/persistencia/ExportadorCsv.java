package diarios.persistencia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Escreve uma tabela em CSV no formato que o Excel em portugues abre direto:
 * separador ponto e virgula e UTF-8 com BOM (sem o BOM, os acentos quebram).
 */
public class ExportadorCsv {

    private static final String SEPARADOR = ";";
    private static final String BOM = "﻿";

    public static void escrever(Path destino, String[] cabecalho, List<String[]> linhas)
            throws IOException {
        StringBuilder s = new StringBuilder(BOM);
        s.append(linha(cabecalho));
        for (String[] l : linhas) {
            s.append(linha(l));
        }
        Files.writeString(destino, s.toString(), StandardCharsets.UTF_8);
    }

    static String linha(String[] campos) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                s.append(SEPARADOR);
            }
            s.append(campo(campos[i]));
        }
        return s.append("\r\n").toString();
    }

    static String campo(String valor) {
        if (valor == null) {
            return "";
        }
        boolean precisaAspas = valor.contains(SEPARADOR) || valor.contains("\"")
                || valor.contains("\n") || valor.contains("\r");
        String limpo = valor.replace("\"", "\"\"");
        return precisaAspas ? "\"" + limpo + "\"" : limpo;
    }
}
