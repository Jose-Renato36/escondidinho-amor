# Preparo para a arguição do código

O professor vai **abrir o código e perguntar**. Este material é pra treinar
isso. Não decore: entenda, e ensaie dizendo em voz alta.

---

## 1. Três regras que valem mais que saber tudo

1. **Nunca invente.** Dizer *"essa parte foi além do que o senhor deu, eu
   entendo o que ela faz, mas não domino o detalhe"* é muito melhor que
   inventar e ser pego. Isso vale principalmente para a Seção 4.
2. **Responda em duas frases:** *o que faz* + *por que foi feito assim*.
   Exemplo: *"`temCorpo` só deixa um título virar ato se tiver texto depois
   dele. Sem isso o sumário do diário virava 30 atos vazios."*
3. **Aponte o arquivo e a linha** enquanto fala. Quem mostra o código
   transmite que conhece o código.

---

## 2. Quem defende o quê (sugestão para 4 pessoas)

| pessoa | pacote | o que dominar |
|---|---|---|
| 1 | `modelo/` + `extratores/` | `Fato`, `Ato`, `Extrator` e as filhas (herança) |
| 2 | `etapas/` | `Limpeza`, `Fragmentacao`, `Portao`, `Ingestao`, `Relatorio` |
| 3 | `analise/` | `PainelContratos`, `GrafoNormas`, `Interpretacao` |
| 4 | `Aplicacao` + `coleta/` + `servidor/` | o fluxo e a parte "fora da aula" |

**Todos** precisam saber explicar `Aplicacao.processarPasta()`.

---

## 3. Perguntas por classe

### `Fato.java`
- **O que é um Fato?** Um dado achado no texto (CNPJ, data, valor) junto com a
  posição exata onde foi achado (`inicio` e `fim`).
- **Por que guardar a posição?** Para provar de onde saiu. O método `provar()`
  recorta o trecho original em volta. Sem posição, o programa pediria pra
  acreditar nele.
- **Por que `equals` e `hashCode`?** Dois fatos são iguais se tiverem o mesmo
  tipo e valor, ignorando a posição. Comparar conteúdo, não referência (Aula 04).
- **Por que os atributos são `private final`?** Encapsulamento: ninguém altera
  um fato depois de criado.

### `Ato.java`
- **O que é um Ato?** Um decreto, portaria ou extrato: a unidade de trabalho.
- **Como nasce a identidade?** `getIdentidade()` monta
  `municipio:especie:numero:data` a partir do conteúdo.
- **Por que não um contador (1, 2, 3)?** Com contador, rodar de novo geraria
  identificadores diferentes.
- **E se dois atos tiverem a mesma identidade?** `desambiguar()` acrescenta
  `#2`, `#3`. Foi um bug real: duas portarias começavam com a mesma linha.

### `Extrator` e as filhas (herança e polimorfismo)
- **Onde está a herança?** `ExtratorCnpj extends Extrator`.
- **Por que `Extrator` é abstrata?** Não faz sentido criar um "extrator
  genérico". Ela define o contrato (`extrair`) e reaproveita `criar` e
  `casaMolde`.
- **Onde está o polimorfismo?** Em `Extracao`: uma lista de `Extrator` com
  cinco classes diferentes, e o laço chama `e.extrair()` sem saber qual é qual.
- **O que é o `@Override`?** Indica que o método sobrescreve o da classe mãe;
  o compilador avisa se a assinatura estiver errada.
- **Como o `casaMolde` funciona?** `9` = qualquer dígito; o resto tem que ser
  idêntico. Anda caractere por caractere com `charAt`.

### `Limpeza.java`
- **O que faz?** Descarta linha com até 3 caracteres (lixo da marca d'água do
  PDF) e recusa o diário que não tem texto suficiente.
- **Por que descartar e não remontar as letras?** Foi testado: remontar colava
  o título de um ato no parágrafo anterior e o programa perdia atos.
- **Por que lançar exceção?** "Diário ilegível" é um resultado possível, não
  um bug. `DiarioIlegivelException` (exceção própria, Aula 05) carrega o
  motivo e a evidência.
- **Por que 10 linhas longas como mínimo?** O diário de Aracaju (PDF
  escaneado) tem 1. O de BH tem mais de 1.000. O limite separa os dois casos.

### `Fragmentacao.java`
- **O que faz?** Corta o diário em atos.
- **Qual o problema principal?** O sumário parece uma lista de atos.
- **Como resolve?** `temCorpo`: o título só vira ato se houver texto de verdade
  depois. **Na dúvida, não corta.**
- **Por que `LEI COMPLEMENTAR` vem antes de `LEI` na lista?** Porque a busca
  para no primeiro que casa; se `LEI` viesse antes, `LEI COMPLEMENTAR` seria
  lida como `LEI`.

### `Portao.java`
- **Por que três saídas e não duas?** Com duas, o caso duvidoso é empurrado pra
  um lado e some. Com três, fica contável.
- **Como um ato chega em ACEITO?** Passando por todos os `if`. Nenhum caminho
  leva a aceito por omissão.
- **E o motivo?** Toda decisão grava o motivo, inclusive as rejeitadas.

### `Ingestao.java` e `Relatorio.java` (Aula 05)
- **Onde usa arquivo?** `Ingestao` lê (`Files.readString`, `DirectoryStream`),
  `Relatorio` grava (`BufferedWriter`).
- **Por que `try-with-resources`?** Fecha o arquivo sozinho, mesmo se der erro.
- **E se a pasta não existir?** `Ingestao` captura `IOException`, anota a falha
  e devolve lista vazia; o programa não cai.

### `PainelContratos.java`
- **Como calcula o ranking?** Agrupa por CNPJ (`indexOf` num `ArrayList`) e
  ordena por **seleção**: acha o maior, marca como usado, repete.
- **Qual a complexidade?** O(n²). Com dezenas de fornecedores é instantâneo.
  Com milhares, valeria outra estrutura (`HashMap`, ainda não dada).
- **Por que agrupar por CNPJ e não por nome?** O nome vem escrito de várias
  formas; o CNPJ não.
- **O que são os papéis (NOTIFICANTE, CONTRATADA)?** Descobrem se o CNPJ é
  fornecedor ou é o órgão que está multando. Nasceu de um erro real.

### `GrafoNormas.java`
- **O que é o grafo?** Uma lista de arestas: *este ato ALTERA/REVOGA/PRORROGA/
  CITA aquela norma*.
- **Por que não procurar só a palavra "altera"?** Ela aparece 145 vezes no
  diário de BH e quase todas são cláusula de contrato.
- **Qual a ordem das regras?** (1) verbo depois da citação, (2) fundamento
  legal, (3) verbo antes. Na dúvida, a relação mais fraca (`CITA`).

### `Coletor.java`
- **Como baixa?** `HttpClient` faz um GET na API do Querido Diário; o JSON é
  lido à mão por `Json.java` com `indexOf` e `substring`.
- **E se a API falhar?** Tenta de novo com espera crescente (5s, 15s, 45s) e,
  depois de 3 fontes seguidas com falha, a rodada para.
- **Por que `Json.java` à mão?** Não usar biblioteca externa.

---

## 4. A zona vermelha: o que está além da aula

O professor já sabe que o scraper é extra. Estes são os outros pontos que ele
pode atacar. **Saiba onde estão e tenha a resposta honesta pronta.**

| o quê | onde | resposta curta e honesta |
|---|---|---|
| `Thread` | `Aplicacao.coletarEmSegundoPlano` | "A coleta demora, então roda numa linha de execução separada pra página não travar." |
| `synchronized` | `Aplicacao` (vários métodos) | "Duas linhas de execução mexem nos mesmos dados; `synchronized` deixa uma de cada vez. Entendo o motivo, não domino o detalhe." |
| `volatile` | `Aplicacao.ocupado` | "Garante que a outra linha enxergue o valor atualizado." |
| `() ->` (lambda) | `new Thread(() -> {...})` | "É um jeito curto de passar um bloco de código. Equivale a uma classe que implementa `Runnable`." |
| `this::tratarPainel` | `Servidor.iniciar` | "Referência a um método: diz ao servidor qual método chamar em cada endereço." |
| `HttpServer` | `Servidor` | "Vem no JDK; recebe pedido do navegador e devolve a página." |
| `Locale.ROOT` | `Contrato.formatarReal` | "No Windows em português o `String.format` usa vírgula; foi um erro real que só aparecia lá." |
| Maven (`pom.xml`) | raiz | "Formato que o IntelliJ entende; não tem dependência nenhuma." |

**Se ele perguntar "por que usou isso se não foi dado?"** a resposta é:
*"Sem `HttpClient` não haveria como buscar o dado real, e sem `HttpServer` não
haveria o painel. Usamos só o que já vem no JDK, sem biblioteca externa."*

---

## 5. Exercícios: "altere o código aqui na minha frente"

Muito professor pede isso. **Treinem os três até fazer sem olhar.**

### Exercício A — achar um tipo novo de dado (CEP)
1. Em `Fato.java`, acrescente `public static final String CEP = "CEP";`
2. Crie `extratores/ExtratorCep.java`:
```java
public class ExtratorCep extends Extrator {
    private static final String MOLDE = "99999-999";

    public ExtratorCep() { super(Fato.CEP); }

    @Override
    public ArrayList<Fato> extrair(String texto) {
        ArrayList<Fato> achados = new ArrayList<>();
        for (int i = 0; i + MOLDE.length() <= texto.length(); i++) {
            if (casaMolde(texto, i, MOLDE)) {
                achados.add(criar(texto.substring(i, i + MOLDE.length()),
                                  i, i + MOLDE.length()));
                i += MOLDE.length() - 1;
            }
        }
        return achados;
    }
}
```
3. Em `Extracao.java`, no construtor: `extratores.add(new ExtratorCep());`

**Testado:** no diário de BH acha 4 CEPs reais (ex.: `03333-050`), e os testes
continuam passando.

**O que isso prova:** polimorfismo. Só acrescentou uma classe e uma linha;
nada mais mudou. (Cuidado: o molde pode pegar pedaço de outro número; se ele
notar, diga que é uma limitação e que o passo seguinte seria checar o que vem
antes e depois.)

### Exercício B — deixar o portão mais rígido
Em `Portao.java`, mude `FATOS_MINIMOS = 2` para `3`. **Resultado medido:**
o diário de BH passa de **66 aceitos / 30 duvidosos** para **58 aceitos / 38
duvidosos**. Mostre no painel ("Reler a pasta", depois de recompilar). *Por que isso é bom de mostrar:*
comprova que o limite é um parâmetro, e que o número de "duvidosos" responde
a ele.

### Exercício C — reconhecer outro tipo de ato
Em `Fragmentacao.java`, acrescente `"CONVENIO"` à lista `ESPECIES`. Atenção à
ordem: nomes mais longos antes dos curtos.

---

## 6. Perguntas gerais que costumam aparecer

- **Por que `ArrayList` e não `HashMap`?** `HashMap` não foi dado. Com dezenas
  ou centenas de itens a diferença de tempo não aparece.
- **Por que não usou banco de dados?** Não foi dado; arquivo texto pode ser
  aberto e conferido por qualquer pessoa.
- **O que acontece se o diário vier vazio?** `Limpeza` lança
  `DiarioIlegivelException`, o diário é recusado com motivo e o programa segue.
- **Como você sabe que o programa acerta?** 85 testes sobre diários reais, mais
  a página do ato, que mostra cada fato com o trecho original.
- **Quais são as limitações?** Só foi validado com diários de texto; PDF
  escaneado não é lido (precisaria de OCR); alguns nomes de empresa não são
  identificados; 31% dos atos ficam em dúvida, e isso aparece no painel.
- **Por que um projeto de diários oficiais?** Dado público, real e bagunçado,
  que exige decisões de programação, não só ler arquivo.

---

## 7. Autoteste (10 minutos, em voz alta)

1. Explique o que `Aplicacao.processarPasta()` faz, em ordem.
2. O que é um `Fato` e por que guarda a posição?
3. Mostre herança no projeto e diga onde está o polimorfismo.
4. O que a `Limpeza` faz e quando lança exceção?
5. Por que o `Portao` tem três saídas?
6. Como o `Fragmentacao` evita contar o sumário como atos?
7. Por que `ranking()` é O(n²) e por que isso não é problema aqui?
8. Faça o Exercício A sem olhar.
9. Explique o que `synchronized` faz e onde aparece.
10. O que acontece se a API estiver fora do ar?

Se você errou mais de 3, releia as Partes 2 e 3 do `GUIA_DE_ESTUDO.md` e
debug o `processarPasta()` no IntelliJ.
