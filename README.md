# 🛡️ Sentinel

![Testes](https://github.com/joaohaze4/sentinel/actions/workflows/testes.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-17%2B-blue)

**Verificador de boletos antes do pagamento.** O Sentinel lê a linha digitável (ou o código de barras) de um boleto bancário, confere se os números são válidos e aponta sinais de fraude, explicando cada um em linguagem simples.

Funciona de dois jeitos: **pelo terminal** ou por uma **interface web** (front-end em HTML/CSS/JS conversando com uma API em Java).

![Interface web do Sentinel](docs/sentinel-web.png)

> Projeto acadêmico de Ciência da Computação (ULBRA/CEULP) sobre prevenção a fraudes em pagamentos.

---

## O problema

O golpe do boleto falso funciona de dois jeitos:

1. **Adulteração "amadora":** alguém troca alguns números do boleto. Os dígitos verificadores deixam de bater.
2. **Adulteração "bem feita":** o golpista gera um código **novo e válido**, mas de outro banco/conta. Todos os dígitos batem e o boleto parece perfeito. A única forma de pegar é comparar com o que você **esperava** pagar.

O Sentinel cobre os dois casos.

## Exemplo

```text
$ sentinel 23791790011043510047391020150008216560000025000 --banco 341 --valor 250,00

============================================================
  SENTINEL - análise de boleto antes do pagamento
============================================================

Linha digitável : 23791.79001 10435.100473 91020.150008 2 16560000025000
Banco           : 237 (Bradesco)
Valor           : R$ 250,00
Vencimento      : 10/12/2026 (daqui a 25 dia(s))

Resultado       : ALTO RISCO  (pontuação 60/100)
O que fazer     : NÃO pague antes de confirmar o boleto direto com quem
                  cobrou (site, app ou telefone oficial).

Alertas:
  [ALTO]   Banco diferente do esperado
           O pagamento vai para o banco 237 (Bradesco), mas você esperava 341
           (Itaú Unibanco). É o golpe mais comum: trocar o código de barras
           para desviar o dinheiro.
```

## O que ele verifica

| Verificação | Nível | Por quê |
|---|---|---|
| Dígitos dos campos 1, 2 e 3 (módulo 10) | 🔴 Alto | Número digitado errado ou alterado |
| Dígito verificador geral (módulo 11) | 🔴 Alto | Protege o código inteiro: banco, valor, vencimento |
| Código de moeda diferente de 9 | 🔴 Alto | Boleto em real sempre usa 9 |
| Valor diferente do esperado | 🔴 Alto | Cobrança a mais ou código trocado |
| Banco diferente do esperado | 🔴 Alto | Golpe do boleto adulterado "bem feito" |
| Banco fora da lista conhecida | 🟡 Médio | Pode ser banco pequeno… ou código inventado |
| Boleto vencido | 🟡 Médio | Boletos antigos são reaproveitados em golpes |
| Vencimento a mais de 1 ano | 🟡 Médio | Incomum, pode indicar alteração |
| Vencimento diferente do esperado | 🟡 Médio | Fatura e boleto deveriam bater |
| Boleto sem valor no código | 🔵 Baixo | O valor é digitado na hora, fácil cobrar a mais |

O resultado final é decidido pelo alerta **mais grave**: `SEM ALERTAS`, `BAIXO RISCO`, `ATENÇÃO` ou `ALTO RISCO`.

## Como rodar

Precisa só do **Java 17 ou mais novo**. O Gradle vem junto (`gradlew`), não precisa instalar.

### Versão web

```bash
./gradlew run --args="--web"
```

Abra **http://localhost:8080** no navegador. Para usar outra porta: `--args="--web --porta 9090"`.

> Dica: com o computador e o celular no mesmo Wi-Fi, dá para abrir no celular usando o IP do computador (ex.: `http://192.168.0.10:8080`).

### Terminal

```bash
# rodar os testes
./gradlew test

# analisar um boleto
./gradlew run --args="34191.79001 10435.100473 91020.150008 7 16560000025000"

# informando o que você esperava pagar
./gradlew run --args="CODIGO --valor 250,00 --banco 341 --vencimento 10/12/2026"

# modo interativo (ele pergunta tudo)
./gradlew run -q --console=plain
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`.

### API

O front-end usa a mesma API que qualquer outro sistema pode usar:

```bash
curl -X POST http://localhost:8080/api/analisar \
     -d "codigo=23791790011043510047391020150008216560000025000" \
     -d "banco=341" -d "valor=250,00"
```

```json
{
  "classificacao": "ALTO_RISCO",
  "classificacaoTexto": "ALTO RISCO",
  "recomendacao": "NÃO pague antes de confirmar o boleto direto com quem cobrou (site, app ou telefone oficial).",
  "pontuacao": 60,
  "boleto": {
    "linhaFormatada": "23791.79001 10435.100473 91020.150008 2 16560000025000",
    "banco": "237",
    "bancoNome": "Bradesco",
    "valor": "250.00",
    "vencimento": "2026-12-10",
    "diasParaVencer": 25
  },
  "alertas": [
    { "nivel": "ALTO", "titulo": "Banco diferente do esperado", "detalhe": "O pagamento vai para o banco 237 (Bradesco)..." }
  ],
  "aviso": "O Sentinel confere só os números do boleto..."
}
```

*(resposta resumida)*

| Rota | O que faz |
|---|---|
| `POST /api/analisar` | Analisa o boleto. Campos: `codigo` (obrigatório), `valor`, `banco`, `vencimento` |
| `GET /api/saude` | Responde `{"status":"ok"}` (para monitoramento/hospedagem) |
| `GET /` | Página web |

Erros voltam com o status certo e uma mensagem: `400` (código inválido), `405` (método errado), `413` (requisição grande demais).

### Programa que roda sozinho

```bash
./gradlew installDist
build/install/sentinel/bin/sentinel --ajuda
```

### Boletos de exemplo (fictícios)

| Caso | Linha digitável |
|---|---|
| ✅ Válido (Itaú, R$ 250,00) | `34191790011043510047391020150008716560000025000` |
| 🔴 Um dígito adulterado | `34191790011073510047391020150008716560000025000` |
| 🔴 Banco trocado (use `--banco 341`) | `23791790011043510047391020150008216560000025000` |
| 🟡 Vencido | `23790928161234567890312345678903215860000189990` |
| 🟡 Banco desconhecido | `99991790061043510047391020150008516560000050000` |
| 🔵 Sem valor | `00190000090123456700400001234178916660000000000` |

> Os exemplos usam datas de 2026. Para ver sempre o mesmo resultado, acrescente `--hoje 2026-11-15`.

## Como o código está organizado

```text
src/main/java/br/com/sentinel/
├── Main.java                  ← ponto de entrada (linha de comando)
├── boleto/                    ← ENTENDER o boleto
│   ├── LeitorDeBoleto.java    ← texto → Boleto (linha digitável ⇄ código de barras)
│   ├── Boleto.java            ← os dados separados (banco, valor, vencimento…)
│   ├── DigitoVerificador.java ← módulo 10 e módulo 11
│   ├── FatorVencimento.java   ← 4 dígitos → data (inclui a virada de 2025)
│   └── Bancos.java            ← código → nome do banco
├── analise/                   ← PROCURAR fraude
│   ├── AnalisadorDeRisco.java ← roda todas as regras e junta o resultado
│   ├── Regra.java             ← interface que toda regra implementa
│   └── regras/                ← uma classe por verificação
├── relatorio/                 ← MOSTRAR o resultado (terminal)
├── cli/                       ← ler os argumentos do terminal
└── web/                       ← servidor HTTP + API JSON
    ├── ServidorWeb.java       ← rotas, segurança e respostas
    ├── RespostaJson.java      ← Resultado → JSON
    ├── Json.java              ← escapar texto em JSON
    └── Formulario.java        ← ler o formulário enviado pelo navegador

src/main/resources/web/        ← FRONT-END
├── index.html
├── estilo.css
└── app.js                     ← chama a API com fetch() e monta o resultado
```

O fluxo é sempre o mesmo. Só muda a "porta de entrada" e a forma de mostrar:

```text
terminal ─┐                                                                  ┌─► RelatorioTexto (terminal)
          ├─► LeitorDeBoleto ─► Boleto ─► AnalisadorDeRisco ─► Resultado ─┤
navegador ┘   (via POST /api/analisar)     (roda cada Regra)              └─► RespostaJson ─► app.js (tela)
```

A regra de negócio fica **só no Java**: o front-end não sabe calcular nada, ele só mostra o que a API devolve.

**Para adicionar uma verificação nova**, crie uma classe em `analise/regras/` que implementa `Regra` e coloque-a na lista do `AnalisadorDeRisco`. Nada mais precisa mudar (padrão de projeto *Strategy*).

## Conceitos usados (para estudar)

- **Linha digitável × código de barras:** a linha (47 dígitos) é o código de barras (44) reorganizado em 5 campos, com 3 dígitos verificadores extras. Veja `LeitorDeBoleto.linhaParaCodigoDeBarras`.
- **Módulo 10 e módulo 11:** algoritmos de dígito verificador da FEBRABAN. Estão comentados passo a passo em `DigitoVerificador`.
- **Fator de vencimento:** dias desde 07/10/1997. Chegou em 9999 em 21/02/2025 e reiniciou em 1000 no dia 22/02/2025. O mesmo fator pode significar duas datas, então o Sentinel escolhe a mais próxima de hoje.
- **`record`** (Java 16+): classes só de dados (`Boleto`, `Alerta`, `Resultado`).
- **`BigDecimal`:** dinheiro nunca é guardado em `double`, porque `0.1 + 0.2` não dá `0.3` exato.
- **Testes com JUnit 5:** 62 testes em `src/test/`, incluindo testes que sobem o servidor de verdade e fazem requisições HTTP. Os boletos de exemplo foram gerados por um programa separado, para os testes não conferirem o código contra ele mesmo.
- **API HTTP sem framework:** o servidor usa o `HttpServer` que já vem no Java, para dar para enxergar o que frameworks como o Spring fazem por baixo.

## Segurança da versão web

- **Sem XSS:** o `app.js` monta a tela com `textContent`, nunca com `innerHTML`, então nenhum texto vira código.
- **Content-Security-Policy:** a página só carrega scripts e estilos do próprio servidor.
- **Sem path traversal:** só uma lista fechada de arquivos pode ser baixada; `/../build.gradle` dá 404.
- **Limite de tamanho:** requisições acima de 4 KB são recusadas (`413`).
- **Sem cache e sem armazenamento:** os dados do boleto não são salvos em lugar nenhum.

## Limitações (importante)

- O Sentinel confere **só os números**. Ele **não** sabe quem é o beneficiário (nome/CNPJ): essa informação fica no registro do banco. **Antes de pagar, confira o beneficiário no app do seu banco.**
- Sem os dados esperados (`--banco`, `--valor`), um boleto falso **bem feito** passa sem alertas. Ele é matematicamente válido.
- Ainda não analisa contas de consumo e guias de imposto (códigos que começam com 8).
- A lista de bancos é resumida.

Nenhuma ferramenta detecta 100% das fraudes. O Sentinel reduz o risco e explica o que conferir.

## Próximos passos

- [ ] Contas de consumo e tributos (padrão de arrecadação, 48 dígitos)
- [ ] Cadastro de fornecedores (banco e faixa de valor que cada um costuma usar)
- [x] Interface web + API HTTP
- [ ] Hospedar a versão web online
- [ ] Migrar a API para Spring Boot
- [ ] Leitura do boleto em PDF/imagem
- [ ] Lista completa de bancos a partir da tabela oficial do Banco Central

---

Feito por **João Guilherme Pereira Aquino**: [portfólio](https://joaohaze4.github.io/potifolio/) · [GitHub](https://github.com/joaohaze4)
