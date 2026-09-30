package diarios;

import diarios.analise.GrafoNormas;
import diarios.analise.LeituraDoAto;
import diarios.analise.PainelContratos;
import diarios.coleta.EstadoFonte;
import diarios.coleta.Fonte;
import diarios.coleta.Fontes;
import diarios.coleta.Json;
import diarios.coleta.Municipio;
import diarios.coleta.ResultadoColeta;
import diarios.coleta.Rodada;
import diarios.erros.DiarioIlegivelException;
import diarios.etapas.Extracao;
import diarios.etapas.Fragmentacao;
import diarios.etapas.Ingestao;
import diarios.etapas.Limpeza;
import diarios.etapas.Portao;
import diarios.etapas.Privacidade;
import diarios.servidor.Formulario;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import diarios.extratores.ExtratorCnpj;
import diarios.extratores.ExtratorReferencia;
import diarios.extratores.ExtratorValor;
import diarios.modelo.Ato;
import diarios.modelo.Diario;
import diarios.modelo.Fato;
import java.util.ArrayList;

public class Testes {

    private static int passou = 0;
    private static int falhou = 0;

    public static void main(String[] args) {
        System.out.println("TESTES");
        System.out.println("======");
        System.out.println();

        java.util.Locale.setDefault(java.util.Locale.forLanguageTag("pt-BR"));
        conferir("formata real em Windows portugues", "R$ 8.323.312,07",
                diarios.modelo.Contrato.formatarReal(8323312.07));
        testarExtratorValor();
        testarExtratorCnpj();
        testarExtratorReferencia();
        testarIngestao();
        testarFonte();
        testarJson();
        testarPapeis();
        testarFontes();
        testarFormulario();
        testarPrivacidade();
        testarRodada();
        testarPipelineCompleto();

        System.out.println();
        System.out.println("passou: " + passou + "   falhou: " + falhou);
        if (falhou > 0) {
            System.exit(1);
        }
    }

    private static void testarExtratorValor() {
        System.out.println("-- ExtratorValor");
        ExtratorValor e = new ExtratorValor();

        ArrayList<Fato> achados = e.extrair("valor global de R$ 184.500,00 para o exercicio");
        conferir("acha um valor", 1, achados.size());
        conferir("valor certo", "R$ 184.500,00", achados.get(0).getValor());
        conferir("converte para numero", 184500.0, ExtratorValor.paraNumero("R$ 184.500,00"));
        conferir("ignora R$ sem centavos", 0, e.extrair("multa de R$ 18 por dia").size());

        String texto = "abcde R$ 1.000,00 fim";
        Fato f = e.extrair(texto).get(0);
        conferir("span aponta certo", "R$ 1.000,00",
                texto.substring(f.getInicio(), f.getFim()));
    }

    private static void testarExtratorCnpj() {
        System.out.println("-- ExtratorCnpj");
        ExtratorCnpj e = new ExtratorCnpj();

        String real = "Deferidos: AKASULO, CNPJ: 48.356.311/0001-14; FLB ATACADO LTDA";
        ArrayList<Fato> achados = e.extrair(real);
        conferir("acha o CNPJ", 1, achados.size());
        conferir("CNPJ certo", "48.356.311/0001-14", achados.get(0).getValor());
        conferir("nao confunde com CPF", 0, e.extrair("CPF 069.555.516-20").size());
    }

    private static void testarExtratorReferencia() {
        System.out.println("-- ExtratorReferencia");
        ExtratorReferencia e = new ExtratorReferencia();

        conferir("lei simples", "lei:11065",
                e.extrair("da Lei nº 11.065, de 2017").get(0).getValor());

        ArrayList<Fato> comSigla =
                e.extrair("O Art. 2º da Portaria SMC nº 073/2025 passa a vigorar");
        conferir("portaria com sigla de orgao", 1, comSigla.size());
        conferir("numero da portaria", "portaria:073", comSigla.get(0).getValor());

        conferir("grafias diferentes, mesma norma",
                e.extrair("Lei nº 11.065").get(0).getValor(),
                e.extrair("LEI MUNICIPAL 11065").get(0).getValor());
    }

    private static void testarIngestao() {
        System.out.println("-- Ingestao");
        conferir("le o municipio do nome", "belo-horizonte",
                Ingestao.municipioDe("belo-horizonte_2026-03-25.txt"));
        conferir("le a data do nome", "2026-03-25",
                Ingestao.dataDe("belo-horizonte_2026-03-25.txt"));
    }

    private static void testarFonte() {
        System.out.println("-- Fonte");
        Fonte f = new Fonte();
        conferir("27 capitais", 27, f.todos().size());
        conferir("acha por nome", "3106200", f.porNome("belo").getCodigo());
        conferir("acha por codigo", "Recife", f.porNome("2611606").getNome());
        conferir("nome de arquivo sem acento", "belo-horizonte",
                f.porNome("3106200").getApelido());
        conferir("sao paulo vira sao-paulo", "sao-paulo", f.porNome("3550308").getApelido());
        conferir("conjunto padrao tem 5", 5, f.padrao().size());
        conferir("municipio inexistente devolve nulo", true, f.porNome("xyz123") == null);
    }

    private static void testarJson() {
        System.out.println("-- Json");
        String resposta = "{\"total_gazettes\":2,\"gazettes\":["
                + "{\"date\":\"2026-03-25\",\"txt_url\":\"http://a/1.txt\",\"edition\":\"7466\"},"
                + "{\"date\":\"2026-03-26\",\"txt_url\":null}]}";

        ArrayList<String> objetos = Json.objetos(resposta, "gazettes");
        conferir("separa as duas edicoes", 2, objetos.size());
        conferir("le a data", "2026-03-25", Json.campo(objetos.get(0), "date"));
        conferir("le a url", "http://a/1.txt", Json.campo(objetos.get(0), "txt_url"));
        conferir("url nula vira vazio", "", Json.campo(objetos.get(1), "txt_url"));
        conferir("chave ausente vira vazio", "", Json.campo(objetos.get(1), "edition"));

        String comAcento = "{\"gazettes\":[{\"territory_name\":\"Macei\\u00f3\"}]}";
        conferir("decodifica acento escapado", "Maceió",
                Json.campo(Json.objetos(comAcento, "gazettes").get(0), "territory_name"));

        String semAspas = "{\"territory_id\": 3106200, \"is_extra_edition\": false, \"x\": null}";
        conferir("le numero sem aspas", "3106200", Json.campo(semAspas, "territory_id"));
        conferir("le booleano", "false", Json.campo(semAspas, "is_extra_edition"));
        conferir("null sem aspas vira vazio", "", Json.campo(semAspas, "x"));
    }

    private static void testarFontes() {
        System.out.println("-- Fontes: ligar, desligar e lembrar");
        Path pasta;
        try {
            pasta = Files.createTempDirectory("fontes-teste");
        } catch (IOException e) {
            marcar(false, "criar pasta temporaria", e.getMessage());
            return;
        }

        Fontes primeira = new Fontes(pasta.toString());
        conferir("comeca com as 27 capitais", 27, primeira.todas().size());
        conferir("sem arquivo, liga as 5 do padrao", 5, primeira.ligadas().size());

        primeira.alternar("3550308");
        conferir("ligar Sao Paulo soma uma", 6, primeira.ligadas().size());
        primeira.alternar("3106200");
        conferir("desligar BH tira uma", 5, primeira.ligadas().size());

        ArrayList<Municipio> achados = new ArrayList<>();
        achados.add(new Municipio("3509502", "Campinas", "SP"));
        achados.add(new Municipio("3550308", "Sao Paulo", "SP"));
        conferir("descoberta so conta o que e novo", 1,
                primeira.adicionarDescobertas(achados, "agora"));
        conferir("descoberta entra desligada", false, primeira.doCodigo("3509502").isLigada());
        conferir("descoberta vira COM_DADO", EstadoFonte.COM_DADO,
                primeira.doCodigo("3509502").getStatus());

        Fontes reaberta = new Fontes(pasta.toString());
        conferir("ao reabrir lembra quantas estao ligadas", 5, reaberta.ligadas().size());
        conferir("ao reabrir lembra que SP esta ligada", true, reaberta.doCodigo("3550308").isLigada());
        conferir("ao reabrir lembra que BH esta desligada", false,
                reaberta.doCodigo("3106200").isLigada());
        conferir("ao reabrir lembra a descoberta", 28, reaberta.todas().size());

        reaberta.ligarTodas(false);
        conferir("desligar todas zera", 0, new Fontes(pasta.toString()).ligadas().size());

        ResultadoColeta falha = new ResultadoColeta(new Municipio("1", "X", "XX"));
        falha.marcarFalhaDeRede("sem rede");
        EstadoFonte e = new EstadoFonte(new Municipio("1", "X", "XX"), true, false);
        e.registrarResultado("agora", falha, "2026-03-20", "2026-03-25");
        conferir("falha de rede vira FALHOU", EstadoFonte.FALHOU, e.getStatus());

        ResultadoColeta vazio = new ResultadoColeta(new Municipio("1", "X", "XX"));
        e.registrarResultado("agora", vazio, "2026-03-20", "2026-03-25");
        conferir("sem edicao vira SEM_EDICAO", EstadoFonte.SEM_EDICAO, e.getStatus());

        System.out.println("-- Fontes: quando pular e quando coletar");
        conferir("sem edicao no periodo: pula o mesmo periodo", true,
                e.jaCobre("2026-03-20", "2026-03-25"));
        conferir("sem edicao: pula periodo menor, dentro do coberto", true,
                e.jaCobre("2026-03-24", "2026-03-25"));
        conferir("periodo maior que o coberto: coleta", false,
                e.jaCobre("2026-03-10", "2026-03-25"));
        conferir("dia seguinte (periodo novo): coleta", false,
                e.jaCobre("2026-03-21", "2026-03-26"));

        ResultadoColeta baixou = new ResultadoColeta(new Municipio("1", "X", "XX"));
        baixou.contarBaixado();
        e.registrarResultado("agora", baixou, "2026-03-20", "2026-03-25");
        conferir("com dado: pula", true, e.jaCobre("2026-03-20", "2026-03-25"));

        ResultadoColeta soPdf = new ResultadoColeta(new Municipio("1", "X", "XX"));
        soPdf.contarSemTexto();
        e.registrarResultado("agora", soPdf, "2026-03-20", "2026-03-25");
        conferir("so PDF conta como sem conteudo: pula", true, e.jaCobre("2026-03-20", "2026-03-25"));

        e.registrarResultado("agora", falha, "2026-03-20", "2026-03-25");
        conferir("falhou: nao pula, coleta de novo", false, e.jaCobre("2026-03-20", "2026-03-25"));

        EstadoFonte nunca = new EstadoFonte(new Municipio("2", "Y", "YY"), true, false);
        conferir("nunca tentada: nao pula", false, nunca.jaCobre("2026-03-20", "2026-03-25"));

        Path pasta2;
        try {
            pasta2 = Files.createTempDirectory("fontes-cobertura");
        } catch (IOException ex) {
            marcar(false, "criar pasta temporaria", ex.getMessage());
            return;
        }
        Fontes antes = new Fontes(pasta2.toString());
        antes.doCodigo("3106200").registrarResultado("agora", baixou, "2026-03-20", "2026-03-25");
        antes.salvar();
        Fontes depois = new Fontes(pasta2.toString());
        conferir("a cobertura sobrevive a fechar e abrir o programa", true,
                depois.doCodigo("3106200").jaCobre("2026-03-20", "2026-03-25"));
        conferir("fonte nao coletada continua sem cobertura", false,
                depois.doCodigo("2611606").jaCobre("2026-03-20", "2026-03-25"));    }

    private static void testarFormulario() {
        System.out.println("-- Formulario");
        conferir("le parametro", "7", Formulario.parametro("dias=7&x=1", "dias"));
        conferir("decodifica espaco e acento", "sao joao",
                Formulario.parametro("q=sao+jo%C3%A3o", "q").replace("ã", "a"));
        conferir("inteiro invalido cai no padrao", 1, Formulario.inteiro("dias=abc", "dias", 1));
        conferir("parametro ausente vira vazio", "", Formulario.parametro("a=1", "b"));
        conferir("codifica e decodifica o nome do arquivo", "belo-horizonte_2026-03-25.txt",
                Formulario.parametro("arquivo=" + Formulario.codificar(
                        "belo-horizonte_2026-03-25.txt"), "arquivo"));
    }

    private static void testarPrivacidade() {
        System.out.println("-- Privacidade");
        String original = "servidor, CPF 069.555.516-20, lotado";
        String mascarado = Privacidade.mascararCpf(original);
        conferir("mascara o CPF", "servidor, CPF ***.***.***-20, lotado", mascarado);
        conferir("mantem o tamanho (os spans continuam valendo)", original.length(),
                mascarado.length());
        conferir("nao mexe em CNPJ", "48.356.311/0001-14",
                Privacidade.mascararCpf("48.356.311/0001-14"));
    }

    private static void testarRodada() {
        System.out.println("-- Rodada: historico gravado e relido");
        Rodada r = new Rodada("2026-03-25 10:00:00", "coleta", 3, "2026-03-22", "2026-03-25");
        ResultadoColeta rc = new ResultadoColeta(new Municipio("1", "X", "XX"));
        rc.contarBaixado();
        rc.contarBaixado();
        rc.contarJaExistia();
        r.adicionar(rc);
        r.finalizar("2026-03-25 10:01:00", "ok; tudo certo");

        Rodada relida = Rodada.daLinha(r.paraLinha());
        conferir("relida nao e nula", true, relida != null);
        conferir("mantem os baixados", 2, relida.baixados());
        conferir("mantem os que ja existiam", 1, relida.jaExistiam());
        conferir("ponto e virgula da observacao nao quebra a linha", "ok, tudo certo",
                relida.getObservacao());
    }

    private static void testarPapeis() {
        System.out.println("-- PainelContratos: papel de cada CNPJ");

        String trechoReal = "NOTIFICADO: ACACIA COMERCIO DE MEDICAMENTOS LTDA., CNPJ N "
                + "03.945.035/0001-91\nNOTIFICANTE: HOSPITAL METROPOLITANO ODILON BEHRENS, "
                + "sito na Rua Formiga, n. 50, Bairro Sao Cristovao, Belo Horizonte/MG, "
                + "inscrito no CNPJ sob o n\n16.692.121/0001-81, neste ato representado. "
                + "Multa de R$ 2.461,28 devida.";

        Diario d = new Diario("teste_2026-03-25.txt", "teste", "2026-03-25", trechoReal);
        d.registrarLimpeza(trechoReal, 0);
        d.adicionarAto(new Ato("teste", "2026-03-25", "TERMO", "1",
                "TERMO", trechoReal, 0));
        new Extracao().extrair(d);

        ArrayList<Diario> lista = new ArrayList<>();
        lista.add(d);

        PainelContratos p = new PainelContratos();
        p.processar(lista);

        conferir("o orgao que multa nao vira fornecedor", 1,
                p.contarDescartadosDoPapel(PainelContratos.LADO_MUNICIPIO));
        conferir("a empresa multada nao vira fornecedor", 1,
                p.contarDescartadosDoPapel(PainelContratos.NOTIFICADO));
        conferir("nenhum contrato neste trecho", 0, p.getContratos().size());
        conferir("multa nao entra no total", 0.0, p.total());
    }

    private static void testarPipelineCompleto() {
        System.out.println("-- pipeline completo, sobre os diarios reais");

        ArrayList<String> falhas = new ArrayList<>();
        ArrayList<Diario> lidos = new Ingestao(Aplicacao.PASTA_DIARIOS).lerTodos(falhas);
        conferirMaiorQue("leu diarios da pasta", lidos.size(), 1);

        Limpeza limpeza = new Limpeza();
        Extracao extracao = new Extracao();
        Portao portao = new Portao();

        int recusados = 0;
        ArrayList<Diario> ok = new ArrayList<>();

        for (Diario d : lidos) {
            try {
                limpeza.limpar(d);
            } catch (DiarioIlegivelException ex) {
                recusados++;
                continue;
            }
            new Fragmentacao().fragmentar(d);
            extracao.extrair(d);
            portao.avaliar(d);
            ok.add(d);
        }

        conferir("recusa o diario escaneado", 1, recusados);
        conferirMaiorQue("processa diario bom", ok.size(), 0);

        Diario bh = ok.get(0);
        conferirMaiorQue("encontrou atos", bh.getAtos().size(), 50);
        conferirMaiorQue("descartou titulos sem corpo", bh.getTitulosDescartados(), 50);
        conferirMaiorQue("extraiu fatos", contarFatos(bh), 500);
        conferirMaiorQue("aceitou atos", bh.contar(Ato.ACEITO), 30);

        boolean todosDecididos = true;
        for (Ato a : bh.getAtos()) {
            if (a.getMotivo().isEmpty() || a.getDecisao().isEmpty()) {
                todosDecididos = false;
            }
        }
        conferir("todo ato tem decisao e motivo", true, todosDecididos);

        ArrayList<String> identidades = new ArrayList<>();
        boolean repetida = false;
        for (Ato a : bh.getAtos()) {
            if (identidades.contains(a.getIdentidade())) {
                repetida = true;
            }
            identidades.add(a.getIdentidade());
        }
        conferir("nenhuma identidade repetida no mesmo diario", false, repetida);

        boolean spansValidos = true;
        String texto = bh.getTextoLimpo();
        for (Ato a : bh.getAtos()) {
            for (Fato f : a.getFatos()) {
                if (f.getInicio() < 0 || f.getFim() > texto.length()
                        || f.getInicio() >= f.getFim()) {
                    spansValidos = false;
                }
            }
        }
        conferir("todo span cabe no texto", true, spansValidos);

        GrafoNormas grafo = new GrafoNormas();
        grafo.processar(ok);
        conferirMaiorQue("montou o grafo", grafo.getArestas().size(), 50);

        PainelContratos painel = new PainelContratos();
        painel.processar(ok);
        LeituraDoAto leitor = new LeituraDoAto(painel, grafo);
        int comLeitura = 0;
        int contratosLidos = 0;
        for (Ato a : bh.getAtos()) {
            String leitura = leitor.ler(a, bh.getData());
            if (!leitura.isEmpty()) {
                comLeitura++;
            }
            if (leitura.startsWith("Contrata ")) {
                contratosLidos++;
            }
        }
        conferir("todo ato ganha uma leitura", bh.getAtos().size(), comLeitura);
        conferirMaiorQue("leitura reconhece contratacoes", contratosLidos, 5);
        conferir("mesma entrada, mesma leitura (deterministico)",
                leitor.ler(bh.getAtos().get(0), bh.getData()),
                new LeituraDoAto(painel, grafo).ler(bh.getAtos().get(0), bh.getData()));
    }

    private static int contarFatos(Diario d) {
        int n = 0;
        for (Ato a : d.getAtos()) {
            n += a.getFatos().size();
        }
        return n;
    }

    private static void conferir(String nome, Object esperado, Object obtido) {
        marcar(esperado.equals(obtido), nome,
                "esperado <" + esperado + ">, obtido <" + obtido + ">");
    }

    private static void conferirMaiorQue(String nome, int obtido, int minimo) {
        marcar(obtido > minimo, nome, "esperado > " + minimo + ", obtido " + obtido);
    }

    private static void marcar(boolean ok, String nome, String detalhe) {
        if (ok) {
            passou++;
            System.out.println("   ok   " + nome);
        } else {
            falhou++;
            System.out.println("   FALHOU  " + nome + " - " + detalhe);
        }
    }
}
