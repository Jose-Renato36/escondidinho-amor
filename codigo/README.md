# Diários Oficiais municipais — coleta, leitura e análise

Java puro, sem nenhuma biblioteca externa. Só o JDK (17 ou mais novo).

O programa sobe um **painel no navegador**, coleta diários oficiais de
municípios brasileiros, separa cada diário em atos individuais, lê o que cada
ato faz e produz informação que **não está escrita em nenhum dos documentos**:

1. quanto cada município contratou, com quem, e quais fornecedores se repetem;
2. um grafo de quais normas foram alteradas, revogadas ou prorrogadas;
3. uma leitura, em português, do que cada ato faz;
4. um relatório do que o programa **não conseguiu ler**, e por quê.

Tudo é **determinístico**: a mesma pasta de diários produz sempre os mesmos
números e as mesmas frases. Nenhuma interpretação usa IA.

---

> **Quer entender o código?** Leia o [GUIA_DE_ESTUDO.md](GUIA_DE_ESTUDO.md): a ordem
> certa de leitura, com os trechos reais e um plano de estudo.

## Como rodar

### No IntelliJ (recomendado)

1. **File → Open** e escolha a pasta **`projeto-diarios`** (a que tem o `pom.xml`).
2. Se perguntar, escolha **"Open as Project"** / **"Trust Project"**. O IntelliJ
   reconhece o Maven e configura tudo sozinho (espere a barra de progresso
   embaixo terminar).
3. Se aparecer *"Project JDK is not defined"*, clique em **Setup SDK** e escolha
   um JDK 17 ou mais novo.
4. Abra `src/main/java/diarios/servidor/Servidor.java`, clique na setinha ▶
   verde ao lado de `public static void main` e escolha **Run**.

O navegador abre sozinho em `http://localhost:8080` e a coleta de 1 dia para
trás começa em segundo plano. Para parar: o botão vermelho ■.

Para rodar os testes: abra `src/test/java/diarios/Testes.java` e aperte ▶ do
mesmo jeito. Para o menu de terminal: `src/main/java/diarios/Principal.java`.

> Se o IntelliJ marcar o código todo em vermelho, clique com o botão direito
> no `pom.xml` → **Maven → Reload Project**.

### Pelo cmd (sem IntelliJ)

Precisa só do JDK 17+ (`javac -version` para conferir).

```
rodar.bat      abre o painel
testar.bat     roda os 75 testes
```

### Pelo Maven (se tiver instalado)

```
mvn package                                  gera target/diarios-oficiais-1.0.jar
java -jar target/diarios-oficiais-1.0.jar    abre o painel
```

Opções da linha de comando: `--sem-coleta`, `--sem-navegador`, `--porta=9090`.

---

## O que tem no painel

| seção | o que faz |
|---|---|
| **Coleta** | escolhe quantos dias para trás (1 a 90) e coleta das fontes ligadas |
| **Descobrir municípios** | pergunta à API quais municípios publicaram texto nos últimos 3 dias e os adiciona à lista |
| **Histórico de rodadas** | cada coleta e cada descoberta: quando, período, quantos baixados, quantas falhas |
| **Fontes** | todas as fontes com botão de ligar/desligar, status da última tentativa e quantos diários já estão no acervo |
| **Busca** | procura uma palavra em todos os diários e leva direto ao ato |
| **O que esses dados dizem** | a interpretação do período, em frases geradas por regra |
| **Por município** | os números separados por município, para comparar fontes |
| **Diários lidos** | cada diário, clicável: atos, leitura de cada um, texto bruto e texto limpo |
| **Página do ato** | o que o ato faz, os fatos extraídos, a prova de cada um recortada do texto original, e o texto com os fatos marcados |
| funil, portão, fornecedores, grafo, contratos | as análises que já existiam |

### Status de uma fonte

| status | significa |
|---|---|
| `COM_DADO` | baixou diário com texto, ou já tinha na pasta |
| `SEM_EDICAO` | a API respondeu, mas não houve edição no período |
| `SO_PDF` | houve edição, mas só em PDF escaneado — sem texto |
| `FALHOU` | a rede ou a API não respondeu (o motivo aparece ao passar o mouse) |
| `NUNCA_TENTADA` | ainda não foi coletada |

O estado das fontes (ligada/desligada, último status) fica em
`dados/estado/fontes.txt`, e o histórico em `dados/estado/rodadas.txt` — os dois são
texto, dá para abrir e conferir.

---

## A coleta

Fonte: **[Querido Diário](https://queridodiario.ok.org.br)** (API em `api.queridodiario.org.br`, endereço novo desde set/2026), projeto aberto da
Open Knowledge Brasil que agrega diários oficiais de milhares de municípios, com
API pública e gratuita.

A lista começa com as **27 capitais**, com 5 ligadas (Belo Horizonte, Recife,
Fortaleza, Curitiba, Aracaju). Nem todo município tem diário com texto no
Querido Diário — por isso existe o botão **Descobrir municípios com publicação
recente**: em vez de adivinhar quais fontes têm conteúdo, o programa pergunta à
API quem publicou texto nos últimos dias e acrescenta esses municípios à lista
(desligados; você liga os que quiser).

Cuidados que estão no código:

- **1 segundo entre requisições** — o servidor é público e gratuito;
- **User-Agent identificando o trabalho**, em vez de fingir ser navegador;
- **diário que já está na pasta não é baixado de novo**;
- **falha de uma fonte não derruba a rodada**: ela fica marcada `FALHOU` com o
  motivo, e as outras continuam;
- **uma coleta por vez**: clicar de novo durante uma coleta mostra um aviso.

---

## Duas peças que não foram dadas em aula

O professor ensinou até a Aula 05. Duas peças do JDK foram usadas além disso,
porque sem elas o programa não teria como buscar o dado real nem mostrar o
painel:

| peça | para quê | por que ela |
|---|---|---|
| `java.net.http.HttpClient` | baixar os diários da API | vem no JDK; nenhuma biblioteca externa |
| `com.sun.net.httpserver.HttpServer` | servir o painel em `localhost` | vem no JDK; evita Spring, Tomcat, Maven |

O resto — ler o JSON da API, ler os formulários do painel, montar o HTML — é
feito à mão com `indexOf`, `substring`, `split` e `StringBuilder`, que são as
Aulas 03 e 04. O painel **não tem JavaScript**: cada botão é um formulário HTML
comum; o Java recebe o clique, faz a ação e devolve a página de novo.

A coleta roda numa `Thread` separada só para a página não ficar travada
enquanto baixa.

---

## Arquitetura: um cérebro, duas portas

```
                 ┌──────────────────────────┐
  navegador ───▶ │ servidor/Servidor        │──┐
                 └──────────────────────────┘  │
                 ┌──────────────────────────┐  ▼
  terminal  ───▶ │ Principal (menu)         │─▶ Aplicacao ─▶ coleta · etapas · analise
                 └──────────────────────────┘
```

`Aplicacao` concentra todo o trabalho (coletar, ler, interpretar). O menu de
console e o painel web só chamam os métodos dela — por isso os números são
sempre os mesmos nos dois.

---

## O pipeline

```
   [0] Coleta            API do Querido Diário → diarios/<municipio>_<data>.txt
   [1] Ingestão          lista a pasta, lê, guarda o bruto
   [2] Limpeza           remove lixo da conversão de PDF
        │                RECUSA o diário sem texto aproveitável
   [3] Fragmentação      corta o diário em atos individuais
        │                só corta com evidência positiva de corpo
   [4] Extração          datas, valores, CNPJ, processos, normas
        │                cada fato guarda a POSIÇÃO no texto
   [5] Portão            ACEITO · DUVIDOSO · REJEITADO, com motivo
   [6] Análise           contratos · grafo · interpretação · leitura de cada ato
```

---

## Sobre os dados pessoais

Diários oficiais publicam **CPF completo** de servidores nomeados, exonerados,
licenciados. O painel mostra esses CPFs mascarados (`***.***.***-20`). A máscara
troca caractere por caractere, então o texto mantém o mesmo tamanho e as
posições dos fatos continuam apontando para o lugar certo.

---

## O que o programa produz

Sobre os dois diários do corpus que vem junto:

```
aracaju_2026-03-11.txt          RECUSADO
   motivo: PDF sem texto extraível (provavelmente escaneado)
   evidência: 1 linha de corpo em 1.715 linhas

belo-horizonte_2026-03-25.txt   PROCESSADO
   limpeza      : 16.338 linhas → 5.478 removidas como lixo
   fragmentação : 225 títulos candidatos → 129 descartados → 96 atos
   portão       : ACEITO 66 · DUVIDOSO 30 · REJEITADO 0
   contratações : 19, somando R$ 8.323.312,07
   grafo        : 6 alterações reais sobre 4 portarias
```

Exemplos de leitura de ato, gerados por regra:

```
Contrata AÇÕES PARA A CIDADANIA (13.630.795/0001-63) por R$ 700.000,00.
  Processo 31.00204095/2026-45.
Ato normativo que altera portaria:074. Vigora desde a publicacao (2026-03-25).
Ato de pessoal: exoneracao. Leitura com ressalva: poucos fatos extraidos (1).
Notificacao ou multa envolvendo 3 CNPJ(s); nao conta como contratacao.
```

---

## As decisões de projeto

Esta seção é a documentação do **porquê**. O código não tem comentários: as
razões estão aqui, onde dá para ler de uma vez.

### 1. O texto bruto nunca é alterado

`Diario` guarda `textoBruto` e `textoLimpo` lado a lado. Se a limpeza ou a
fragmentação tiverem defeito, conserta-se o código e roda de novo. Alterar o
original é a única perda que nenhum conserto futuro alcança.

### 2. Recusar é melhor que fingir que leu

O diário de Aracaju tem **269 páginas** e o texto extraído tem **1 linha de
corpo** — é um PDF escaneado, o conteúdo está em imagem.

Sem a recusa explícita, o programa devolveria "0 atos", que é indistinguível de
"dia sem publicação". São coisas diferentes, e `DiarioIlegivelException` carrega
o motivo **e a evidência numérica** que sustenta a recusa.

### 3. Descartar, nunca remontar

A marca d'água vertical do PDF ("Documento assinado digitalmente") sai picotada,
uma ou duas letras por linha, no meio do texto — **5.478 das 16.338 linhas** do
diário de BH.

A tentação é colar os fragmentos de volta em palavras. Foi testado e **piora**:
colar junta o título do ato no parágrafo anterior, e o programa passa a perder
atos que antes achava. Descartar linha curta é seguro — o corpo do documento
está intacto por baixo.

### 4. Só fragmenta com evidência positiva

O diário de Aracaju tem isto:

```
DECRETO NUMERADOS - 8512
    DECRETO SIMPLES - 2026-02-23
    DECRETO SIMPLES - 2026-02-24
    ...
```

Parece lista de atos. É o **sumário**. Um fragmentador que corta sempre que vê
um título cria 30 atos vazios ali, e a partir daí toda contagem do programa
mente.

A regra: título só vira ato se houver corpo de texto depois dele. No diário de
BH isso descarta **129 dos 225** títulos candidatos.

Perder um ato é ruim; inventar 30 é pior, porque o erro não aparece — vira
número no relatório.

### 5. Todo fato guarda a posição no texto

Não é "achei a data 24/03/2026". É "achei `24/03/2026` entre os caracteres
4.502 e 4.512". O método `Fato.provar()` recorta o trecho original em volta, e
a opção 9 do menu mostra isso ato por ato.

Um extrator sem posição pede para acreditar nele. Um extrator com posição
mostra a prova.

### 6. Três saídas no portão, nunca duas

| erro | acontece o quê |
|---|---|
| deixa passar lixo | **aparece** — alguém lê a saída e vê |
| descarta coisa boa | **não aparece nunca** |

Com duas saídas, o caso duvidoso é empurrado para um dos lados e some. Com a
terceira, ele fica contável — os 30 DUVIDOSO do BH são a lista do que o
programa ainda não trata.

`ACEITO` exige sinal positivo. Nenhum caminho leva a "aceita porque não achei
nada errado".

### 7. Procurar a palavra não é classificar

No diário de BH a palavra "altera" aparece **145 vezes** e "prorrog" **125**.
Quase nenhuma fala de norma:

```
"Não haverá alteração dos preços ora registrados"        → contrato
"As demais condições permanecem inalteradas"             → contrato
"prorrogação da vigência da Ata de Registro de Preços"   → contrato
"revoga as contratações dos candidatos"                  → pessoal
```

Um classificador que só procura a palavra criaria **270 relações falsas** num
diário que tem **5 verdadeiras**. Por isso `GrafoNormas.classificar()` começa
eliminando: citação de fundamento legal ("nos termos do Decreto X") e forma
não-dispositiva viram apenas `CITA`.

A ordem das regras também importa. Em `O Art. 2º da Portaria SMC nº 073/2025
passa a vigorar`, o trecho começa com "Art.", que é marca de fundamento — mas o
"passa a vigorar" logo depois é evidência mais forte. Na ordem inversa, a única
alteração real do diário virava menção.

### 8. Sigla de órgão no meio da citação

`Portaria SMC nº 073/2025` não era encontrada porque o extrator não passava
pela sigla do órgão — e essa era a **única** alteração de norma do diário
inteiro. `ExtratorReferencia` aceita a sigla, mas só em MAIÚSCULA no texto
original: aceitar minúscula faria "lei que trata do artigo 5" virar citação a
uma lei número 5.

### 9. Agrupar por CNPJ, não por nome

A mesma empresa aparece escrita de várias formas no diário. O CNPJ é o único
campo estável, então o ranking soma por ele.

---

### 10. Identidade única dentro do diário

Várias portarias de BH começam com a mesma frase ("Lei Federal nº 13.019/2014 e
ao Decreto Municipal nº 16.746/2017...") e a fragmentação tirava o número
dessa linha. Resultado: dois atos diferentes com a mesma identidade
`lei:13019`, e a leitura de um herdava a alteração do outro — o painel dizia
"altera portaria:073" num ato que, no texto logo abaixo, alterava a **074**.

Achado olhando a página do ato, não num teste. Agora, dentro do mesmo diário,
uma identidade repetida ganha `#2`, `#3`, e há um teste que trava isso.

### 11. A janela depois da citação

Em `Portaria SMC Nº 074/2025, publicada no DOM em 25 de novembro de 2025,
passa a vigorar`, o verbo fica a ~60 caracteres da citação. Com a janela de
60 a alteração se perdia; com 90 ela aparece, e as 6 alterações encontradas
foram conferidas uma a uma no texto (a busca do painel faz isso em segundos).

---

## Onde está cada conteúdo de aula

| pacote | o que faz | conteúdo |
|---|---|---|
| `modelo/` | Diario, Ato, Fato, Contrato, Relacao, Achado | classes, encapsulamento, `equals()` |
| `extratores/` | família de extratores | **herança e polimorfismo** |
| `etapas/` | as etapas do pipeline | `String`, `ArrayList`, arquivos |
| `analise/` | contratos, grafo, interpretação, leitura do ato | agregação, ordenação, percurso em grafo |
| `coleta/` | API, JSON, fontes, rodadas | `String`, `ArrayList`, arquivos, exceções |
| `servidor/` | o painel web | `String`, `StringBuilder` (+ `HttpServer`, ver acima) |
| `erros/` | duas exceções próprias | **exceção personalizada** |
| `Principal.java` | menu de console | `Scanner`, `switch` |

---

## Estrutura

```
projeto-diarios/
├── pom.xml                     configuracao do Maven (Java 17, sem dependencias)
├── README.md
├── rodar.bat · testar.bat      rodar pelo cmd do Windows
├── dados/
│   ├── diarios/                entrada: um .txt por diario
│   ├── estado/                 fontes ligadas e historico de rodadas (gerado)
│   └── saida/                  relatorios gravados pelo menu (gerado)
└── src/
    ├── main/java/diarios/
    │   ├── Aplicacao.java      o "cerebro": coletar, ler, interpretar
    │   ├── Principal.java      menu de terminal
    │   ├── servidor/           painel web: Servidor, PainelWeb, PaginaDiario, Formulario
    │   ├── coleta/             API, JSON, fontes, rodadas
    │   ├── etapas/             ingestao, limpeza, fragmentacao, extracao, portao
    │   ├── extratores/         data, valor, CNPJ, processo, norma (heranca)
    │   ├── analise/            contratos, grafo, interpretacao, leitura do ato
    │   ├── modelo/             Diario, Ato, Fato, Contrato, Relacao, Achado
    │   └── erros/              excecoes proprias
    └── test/java/diarios/
        └── Testes.java         75 testes sobre dados reais
```
