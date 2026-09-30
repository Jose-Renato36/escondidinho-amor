# Guia de estudo — como ler este projeto

Este guia é pra você entender o código **na ordem certa**, sem se perder. Cada
passo diz o que abrir, o que procurar, e o que perguntar a si mesmo.

---

## Parte 0 — O mapa em uma imagem

O projeto faz UMA coisa: transforma um arquivo de texto gigante (o diário
oficial) em informação organizada.

```
   diário (.txt)                          o que sai
   ─────────────                          ──────────
   16.338 linhas misturadas    ──▶  96 atos separados
                                     ├─ cada um com datas, valores, CNPJs
                                     ├─ cada um com uma nota (aceito/duvidoso/rejeitado)
                                     └─ e uma frase dizendo o que ele faz
```

Tudo que existe no código serve a uma destas cinco perguntas:

| pergunta | pasta | exemplo |
|---|---|---|
| **o que é** o dado? | `modelo/` | `Ato`, `Fato`, `Diario` |
| **de onde vem** o dado? | `coleta/` | `Coletor`, `Fontes` |
| **como limpar e separar** o dado? | `etapas/` | `Limpeza`, `Fragmentacao` |
| **o que achar** no texto? | `extratores/` | `ExtratorCnpj`, `ExtratorData` |
| **o que isso significa**? | `analise/` | `GrafoNormas`, `Interpretacao` |

E a pasta `servidor/` só **mostra** o resultado no navegador.

> **Regra pra não se perder:** se você está lendo a pasta `servidor/` e não
> entende algo, PARE. Ela é só apresentação. O coração do projeto está em
> `etapas/` e `extratores/`.

---

## Parte 1 — Comece pelas classes de dados (15 minutos)

Abra `modelo/Fato.java`. É a classe mais simples e a mais importante do projeto.

```java
public class Fato {
    private final String tipo;    // "CNPJ", "DATA", "VALOR"...
    private final String valor;   // "48.356.311/0001-14"
    private final int inicio;     // em que caractere do texto começa
    private final int fim;        // em que caractere termina
```

**O que entender:** um `Fato` não é só "achei um CNPJ". É "achei este CNPJ,
**entre o caractere 4502 e o 4520**". Guardar a posição é a ideia central do
trabalho. Com ela o método `provar()` consegue recortar o trecho original e
mostrar de onde o dado saiu.

**Conceitos de aula aqui:** atributos `private`, construtor, getters
(encapsulamento), `equals()` e `hashCode()` (comparar conteúdo, não referência).

Depois leia `modelo/Ato.java`. Um `Ato` é um decreto, uma portaria, um extrato
de contrato: **a unidade de trabalho do projeto**. Ele tem:
- um `texto` (o trecho do diário que é dele),
- uma lista de `Fato` (`ArrayList<Fato>`),
- uma `decisao` (ACEITO / DUVIDOSO / REJEITADO) e o `motivo`.

**Pergunta pra si mesmo:** por que a lista de fatos fica dentro do `Ato` e não
solta? *(Porque cada fato pertence a um ato; o ato é quem decide se os fatos
bastam.)*

Por último, `modelo/Diario.java`: o arquivo inteiro de um dia, com o texto
**bruto** e o texto **limpo** lado a lado.

---

## Parte 2 — Herança e polimorfismo (20 minutos)

Abra `extratores/Extrator.java`. É uma **classe abstrata**: não se cria um
`Extrator`, só se criam as filhas.

```java
public abstract class Extrator {
    public abstract ArrayList<Fato> extrair(String texto);   // cada filha implementa
    protected Fato criar(String valor, int inicio, int fim)  // atalho reaproveitado
    protected static boolean casaMolde(String texto, int posicao, String molde)
```

Agora abra `extratores/ExtratorCnpj.java` (tem só 25 linhas):

```java
public class ExtratorCnpj extends Extrator {
    private static final String MOLDE = "99.999.999/9999-99";

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

**Como funciona o `MOLDE`:** cada `9` significa "qualquer dígito"; qualquer
outro caractere tem que ser igual. O método `casaMolde` (em `Extrator.java`)
anda caractere por caractere comparando. Nenhuma expressão regular: é só
`charAt`, `substring`, `for` (Aula 04).

**Exercício:** abra `ExtratorData.java` e `ExtratorValor.java`. Todos têm o
mesmo método `extrair`, e cada um faz do seu jeito. Isso é **polimorfismo**
(Aula 02, o exemplo do `FazerSom()` do Cachorro e do Gato).

Onde o polimorfismo aparece? Em `etapas/Extracao.java`:

```java
extratores.add(new ExtratorData());
extratores.add(new ExtratorValor());
extratores.add(new ExtratorCnpj());
...
for (Extrator e : extratores) {
    for (Fato f : e.extrair(ato.getTexto())) { ... }
}
```

O laço chama `e.extrair(...)` sem saber qual filha é. Cada uma responde do seu
jeito. **Se você quiser achar CEPs, cria uma classe filha e acrescenta uma
linha. Nada mais muda.**

---

## Parte 3 — O pipeline, que é o esqueleto do projeto (30 minutos)

Abra `Aplicacao.java` e ache o método `processarPasta()`. **Este método é o
projeto inteiro em 30 linhas.** Leia devagar:

```java
ArrayList<Diario> lidos = new Ingestao(PASTA_DIARIOS).lerTodos(falhas);   // 1

for (Diario d : lidos) {
    try {
        limpeza.limpar(d);                                                // 2
    } catch (DiarioIlegivelException e) {
        novaCobertura.registrarRecusa(...);                               // 2b
        novos.add(d);
        continue;
    }
    new Fragmentacao().fragmentar(d);                                     // 3
    extracao.extrair(d);                                                  // 4
    portao.avaliar(d);                                                    // 5
    ...
}
novoPainel.processar(novos);                                              // 6
novoGrafo.processar(novos);                                               // 7
novaInterpretacao.interpretar(...);                                       // 8
```

Siga cada número até a classe correspondente:

### 1. `etapas/Ingestao.java` — ler a pasta
Lista os `.txt` de `dados/diarios/` e cria um `Diario` para cada um. O nome do
arquivo (`belo-horizonte_2026-03-25.txt`) carrega município e data.
**Aula 05:** `Files`, `DirectoryStream`, `try-with-resources`, `IOException`.

### 2. `etapas/Limpeza.java` — limpar e recusar
Leia o método `limpar`. Duas ideias:
- linha com 3 caracteres ou menos é **lixo** (a marca d'água do PDF sai
  picotada: `D`, `oc`, `um`...) e é **descartada**;
- se sobrarem menos de 10 linhas longas, o diário é **ilegível** e lança
  `DiarioIlegivelException`.

**Por que lançar exceção, em vez de devolver um número?** Porque "diário
ilegível" não é um erro do programa: é um resultado possível que precisa ser
tratado. Por isso é uma **exceção própria** (Aula 05). Veja em
`erros/DiarioIlegivelException.java` que ela carrega o **motivo** e a
**evidência**.

### 3. `etapas/Fragmentacao.java` — cortar em atos
A parte mais difícil. Leia `temCorpo`:

```java
private boolean temCorpo(String bloco, String titulo) {
    ...
    if (semTitulo.trim().length() < CORPO_MINIMO) return false;
    for (String l : semTitulo.split("\n")) {
        if (l.trim().length() > PARAGRAFO_MINIMO) return true;
    }
    return false;
}
```

**A ideia:** uma linha que começa com "DECRETO" só vira ato se tiver **texto de
verdade depois**. Senão é item de sumário. Na dúvida, **não corta**.

### 4. `etapas/Extracao.java` — chamar os extratores
Já vimos na Parte 2.

### 5. `etapas/Portao.java` — dar a nota
Leia `avaliarAto`. É só uma sequência de `if`:

```
texto curto demais?           → REJEITADO  (com o motivo)
poucos fatos?                 → DUVIDOSO   (com o motivo)
sem número/valor/norma?       → DUVIDOSO
sem nenhuma data?             → DUVIDOSO
passou em tudo                → ACEITO
```

**Repare:** só chega em ACEITO quem passou por todos os `if`. **Nenhum caminho
leva a "aceito" por omissão.** E toda decisão grava o `motivo`.

### 6, 7, 8. `analise/` — o que significa
Depois de ter os atos com fatos, `analise/` responde perguntas:
- `PainelContratos`: quem foi contratado, por quanto?
- `GrafoNormas`: qual ato altera qual norma?
- `Interpretacao`: as frases do "o que esses dados dizem".

---

## Parte 4 — Os trechos mais difíceis (leia por último)

Quando o resto estiver claro, estes três são os que mais custam:

**`analise/GrafoNormas.classificar()`** — decide se um ato ALTERA, REVOGA,
PRORROGA ou só CITA uma norma. A dificuldade: a palavra "altera" aparece 145
vezes no diário de BH e quase todas são cláusula de contrato ("alteração dos
preços"), não norma. Por isso o método segue uma ordem, e a ordem
importa: (1) olha o trecho **depois** da citação ("...passa a vigorar" ⇒
ALTERA); (2) descarta "fundamento legal" (`nos termos do Decreto X` ⇒ só
CITA); (3) só então procura o verbo **antes** da citação ("revoga o...").
Se nada bater, é só CITA — na dúvida, a relação mais fraca.

**`analise/PainelContratos.papelENomeAntesDe()`** — descobre se um CNPJ é
fornecedor, ou é o órgão que está multando (`NOTIFICANTE`), ou a empresa
multada (`NOTIFICADO`). Nasceu de um erro real: o programa somava multas como
contratos.

**`coleta/Coletor.consultarApi()`** — tenta de novo com espera crescente
(5s, 15s, 45s) quando a API recusa por excesso de pedidos.

---

## Parte 5 — Como ver o código funcionando (o melhor jeito de aprender)

No IntelliJ, use o **Debug**:

1. Abra `Aplicacao.java`, vá ao método `processarPasta()`.
2. Clique na **margem esquerda** ao lado da linha `limpeza.limpar(d);` —
   aparece uma bolinha vermelha (breakpoint).
3. Rode `Servidor.java` com o botão do **inseto 🐞** (Debug), não o ▶.
4. O programa **para** nessa linha. Embaixo aparece a aba **Variables**:
   abra `d` e veja o texto bruto do diário.
5. Aperte **F8** (Step Over) para avançar uma linha por vez, e **F7**
   (Step Into) para entrar dentro do método.

Faça isso com `limpeza.limpar(d)`, depois `fragmentar(d)`, depois
`extracao.extrair(d)`. Em cada passo olhe `d.getAtos()` crescer: primeiro vazio,
depois com 96 atos, depois cada ato com seus fatos.

**Atalhos que salvam a vida:**
- `Ctrl + clique` num nome → vai pra onde ele foi escrito
- `Alt + ←` → volta
- `Ctrl + Shift + F` → busca um texto no projeto inteiro

---

## Parte 6 — Como responder na apresentação

| se o professor perguntar... | aponte para... |
|---|---|
| "Mostra o uso de herança." | `Extrator` + `ExtratorCnpj` (Parte 2) |
| "Mostra tratamento de exceção." | `Limpeza.limpar()` lança, `Aplicacao.processarPasta()` captura |
| "Onde você usa arquivos?" | `Ingestao` (lê), `Relatorio` (escreve), ambos com `try-with-resources` |
| "Onde você usa ArrayList?" | quase em todo lugar: `Ato.fatos`, `Diario.atos`... |
| "Como você sabe que não inventou o dado?" | `Fato.provar()` e a página de um ato no painel |
| "Por que não usou banco?" | não foi dado; arquivo texto dá pra abrir e conferir |
| "O que é essa parte de servidor?" | só mostra o resultado; o trabalho está em `etapas/` |

---

## Um plano de estudo realista

| dia | o que fazer | tempo |
|---|---|---|
| 1 | Partes 0 e 1: as classes de `modelo/` | 30 min |
| 1 | Parte 2: `Extrator` e as filhas | 30 min |
| 2 | Parte 3: `processarPasta()` + `Limpeza` + `Portao` | 1 h |
| 2 | Parte 5: debugar `processarPasta()` | 30 min |
| 3 | `Fragmentacao` e `Ingestao` | 1 h |
| 3 | Parte 4: `GrafoNormas` | 1 h |

**Se sobrar pouco tempo antes da arguição:** domine a Parte 2 (herança) e
a Parte 3 (o pipeline). Com essas duas você responde a maioria das perguntas.
