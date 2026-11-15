// =====================================================
// SENTINEL - front-end
// Envia o formulário para a API Java (POST /api/analisar)
// e mostra o resultado na tela.
// =====================================================

const formulario = document.getElementById('formulario');
const botao = document.getElementById('botao');
const secaoResultado = document.getElementById('resultado');
const conteudo = document.getElementById('conteudo-resultado');
const esperado = document.getElementById('esperado');

const CORES = { SEM_ALERTAS: '#22c55e', BAIXO_RISCO: '#60a5fa', ATENCAO: '#f59e0b', ALTO_RISCO: '#ef4444' };
const NOMES_NIVEL = { ALTO: 'ALTO', MEDIO: 'MÉDIO', BAIXO: 'BAIXO' };

// Boletos e contas FICTÍCIOS (os mesmos do README e dos testes)
const EXEMPLOS = {
    valido:       { codigo: '34191.79001 10435.100473 91020.150008 7 16560000025000' },
    adulterado:   { codigo: '34191.79001 10735.100473 91020.150008 7 16560000025000' },
    trocado:      { codigo: '23791.79001 10435.100473 91020.150008 2 16560000025000', banco: '341', valor: '250,00' },
    desconhecido: { codigo: '99991.79006 10435.100473 91020.150008 5 16560000050000' },
    luz:          { codigo: '83650000001-0 87520048000-7 00000001234-4 56789012345-6', tipo: 'ENERGIA_E_GAS' },
    // código real de TELEFONE, mas a pessoa acha que é conta de luz
    contaFalsa:   { codigo: '84630000001-1 87520221000-6 00000001234-4 56789012345-6', tipo: 'ENERGIA_E_GAS' }
};


// ---------------- enviar ----------------

formulario.addEventListener('submit', async function (evento) {
    evento.preventDefault(); // não recarrega a página

    carregando(true);

    try {
        const resposta = await fetch('/api/analisar', {
            method: 'POST',
            body: new URLSearchParams(new FormData(formulario))
        });

        const dados = await resposta.json();

        if (!resposta.ok) {
            mostrarErro(dados.erro || 'Não foi possível analisar o código.');
            return;
        }

        mostrarResultado(dados);

    } catch (erro) {
        mostrarErro('Não consegui falar com o servidor. Ele está rodando?');
    } finally {
        carregando(false);
    }
});

function carregando(sim) {
    botao.disabled = sim;
    botao.textContent = sim ? 'Analisando...' : 'Analisar';
}


// ---------------- exemplos ----------------

document.querySelectorAll('.exemplo').forEach(function (botaoExemplo) {
    botaoExemplo.addEventListener('click', function () {
        const exemplo = EXEMPLOS[botaoExemplo.dataset.exemplo];

        formulario.reset();
        formulario.codigo.value = exemplo.codigo;
        formulario.banco.value = exemplo.banco || '';
        formulario.valor.value = exemplo.valor || '';
        formulario.tipo.value = exemplo.tipo || '';
        esperado.open = Boolean(exemplo.banco || exemplo.valor || exemplo.tipo);

        formulario.requestSubmit(); // já analisa
    });
});


// ---------------- mostrar na tela ----------------
// Tudo é montado com createElement + textContent (nunca innerHTML com dados).
// Assim, mesmo que viesse um texto malicioso, ele aparece como texto e não vira código (XSS).

function criar(tag, classe, texto) {
    const elemento = document.createElement(tag);
    if (classe) elemento.className = classe;
    if (texto !== undefined) elemento.textContent = texto;
    return elemento;
}

function mostrarResultado(r) {
    conteudo.replaceChildren();

    // tipo + selo + recomendação
    conteudo.append(criar('span', 'tipo-documento',
        r.tipo === 'CONTA_DE_CONSUMO' ? 'conta de consumo / tributo' : 'boleto bancário'));
    conteudo.append(criar('span', 'selo selo--' + r.classificacao, r.classificacaoTexto));
    conteudo.append(criar('p', 'recomendacao', r.recomendacao));

    // medidor de risco
    const medidor = criar('div', 'medidor');
    const preenchido = criar('div');
    preenchido.style.background = CORES[r.classificacao];
    medidor.append(preenchido);
    conteudo.append(medidor);
    conteudo.append(criar('span', 'pontuacao', 'pontuação de risco: ' + r.pontuacao + '/100'));
    requestAnimationFrame(function () {
        preenchido.style.width = Math.max(r.pontuacao, 3) + '%';
    });

    // dados do documento (cada tipo mostra campos diferentes)
    const d = r.documento;
    const lista = criar('dl', 'dados');
    adicionarDado(lista, 'Linha digitável', d.linhaFormatada, true);

    if (r.tipo === 'CONTA_DE_CONSUMO') {
        adicionarDado(lista, 'Tipo de conta', d.segmentoCodigo + ' (' + (d.segmentoNome || 'segmento inexistente') + ')');
        adicionarDado(lista, 'Empresa', d.empresaFormatada);
        adicionarDado(lista, 'Valor', d.valorFormatado || 'não definido no código');
        adicionarDado(lista, 'Vencimento', 'não vem no código (confira na conta)');
    } else {
        adicionarDado(lista, 'Banco', d.banco + ' (' + (d.bancoNome || 'banco não identificado') + ')');
        adicionarDado(lista, 'Valor', d.valorFormatado || 'não definido no código');
        adicionarDado(lista, 'Vencimento', descreverVencimento(d));
    }
    conteudo.append(lista);

    // alertas
    conteudo.append(criar('h3', 'subtitulo', 'Alertas'));

    if (r.alertas.length === 0) {
        conteudo.append(criar('p', 'nenhum', '✔ Nenhum alerta encontrado.'));
    } else {
        const ul = criar('ul', 'alertas');

        r.alertas.forEach(function (alerta) {
            const li = criar('li', 'alerta alerta--' + alerta.nivel);
            li.append(criar('span', 'nivel', NOMES_NIVEL[alerta.nivel] || alerta.nivel));

            const texto = criar('div');
            texto.append(criar('strong', null, alerta.titulo));
            texto.append(criar('p', null, alerta.detalhe));
            li.append(texto);

            ul.append(li);
        });

        conteudo.append(ul);
    }

    // observações: coisas que o usuário preencheu e que não se aplicam (não são risco)
    (r.observacoes || []).forEach(function (observacao) {
        conteudo.append(criar('p', 'observacao', 'ℹ ' + observacao));
    });

    conteudo.append(criar('p', 'aviso', '⚠ ' + r.aviso));

    exibirSecao();
}

function adicionarDado(lista, rotulo, valor, monoespacado) {
    lista.append(criar('dt', null, rotulo));
    lista.append(criar('dd', monoespacado ? 'mono' : null, valor));
}

function descreverVencimento(b) {
    if (!b.vencimento) return 'sem vencimento';

    const dias = b.diasParaVencer;
    let quando;

    if (dias === 0) quando = 'vence hoje';
    else if (dias > 0) quando = 'daqui a ' + dias + ' dia(s)';
    else quando = 'venceu há ' + (-dias) + ' dia(s)';

    return b.vencimentoFormatado + ' (' + quando + ')';
}

function mostrarErro(mensagem) {
    conteudo.replaceChildren(criar('p', 'erro', mensagem));
    exibirSecao();
}

function exibirSecao() {
    secaoResultado.classList.remove('escondido');
    secaoResultado.scrollIntoView({ behavior: 'smooth', block: 'start' });
}
