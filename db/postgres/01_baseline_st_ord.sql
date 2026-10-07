CREATE SCHEMA IF NOT EXISTS ORDENS;

SET SEARCH_PATH TO ORDENS;

CREATE TABLE IF NOT EXISTS ORCAMENTOS (
    ID UUID NOT NULL,
    CUSTO_MAO_DE_OBRA NUMERIC(12,2) NOT NULL,
    CUSTO_INSUMOS NUMERIC(12,2) NOT NULL,
    APROVADO BOOLEAN NOT NULL DEFAULT FALSE,
    OBSERVACAO TEXT NOT NULL DEFAULT '',
    DATA_CRIACAO TIMESTAMPTZ(6) NOT NULL,
    DATA_ATUALIZACAO TIMESTAMPTZ(6) NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT CK_ORCAMENTOS_CUSTO_MAO_DE_OBRA CHECK (CUSTO_MAO_DE_OBRA >= 0),
    CONSTRAINT CK_ORCAMENTOS_CUSTO_INSUMOS CHECK (CUSTO_INSUMOS >= 0)
);

COMMENT ON TABLE ORCAMENTOS IS 'Orcamento de uma ordem de servico. Separado da OS porque tem ciclo proprio de aprovacao e reprovacao, e porque o valor total nao e armazenado: e derivado.';
COMMENT ON COLUMN ORCAMENTOS.CUSTO_INSUMOS IS 'Custo dos insumos no momento do orcamento, congelado. O preco vive no catalogo e pode mudar depois sem reabrir orcamento aprovado.';
COMMENT ON COLUMN ORCAMENTOS.APROVADO IS 'Verdadeiro apos aprovacao do cliente. Aprovacao nao avanca o estado da OS: quem avanca e a confirmacao da saga de reserva.';
COMMENT ON COLUMN ORCAMENTOS.OBSERVACAO IS 'Trilha legivel de aprovacao e reprovacao, com motivo. Nao e campo de negocio, e registro de decisao.';

CREATE TABLE IF NOT EXISTS ORDENS_SERVICO (
    ID UUID NOT NULL,
    MOTIVO VARCHAR(500) NOT NULL,
    OBSERVACAO TEXT NOT NULL DEFAULT '',
    CLIENTE_ID UUID NOT NULL,
    MECANICO_ID UUID NOT NULL,
    VEICULO_ID UUID NOT NULL,
    STATUS VARCHAR(30) NOT NULL,
    PRAZO_CONCLUSAO TIMESTAMPTZ(6),
    ORCAMENTO_ID UUID,
    VERSAO INTEGER NOT NULL DEFAULT 0,
    DATA_CRIACAO TIMESTAMPTZ(6) NOT NULL,
    DATA_ATUALIZACAO TIMESTAMPTZ(6) NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT FK_ORDENS_SERVICO_ORCAMENTO FOREIGN KEY (ORCAMENTO_ID) REFERENCES ORCAMENTOS (ID),
    CONSTRAINT UQ_ORDENS_SERVICO_ORCAMENTO UNIQUE (ORCAMENTO_ID)
);

COMMENT ON TABLE ORDENS_SERVICO IS 'Agregado raiz deste servico. CLIENTE_ID, MECANICO_ID e VEICULO_ID sao referencias opacas a dados de outros servicos: nao ha chave estrangeira nem join possivel, de proposito.';
COMMENT ON COLUMN ORDENS_SERVICO.CLIENTE_ID IS 'Referencia ao usuario no service-track-usuarios-veiculos. Sem integridade referencial: banco de outro servico.';
COMMENT ON COLUMN ORDENS_SERVICO.VEICULO_ID IS 'Referencia ao veiculo no service-track-usuarios-veiculos. Idem.';
COMMENT ON COLUMN ORDENS_SERVICO.STATUS IS 'Nome do valor do enum, nao o ordinal. Ordinal quebra silenciosamente quando alguem reordena o enum.';
COMMENT ON COLUMN ORDENS_SERVICO.VERSAO IS 'Trava otimista. Duas confirmacoes de saga concorrentes na mesma OS tem de colidir aqui, nao sobrescrever uma a outra.';
COMMENT ON COLUMN ORDENS_SERVICO.PRAZO_CONCLUSAO IS 'Prazo prometido ao cliente. Nao e o prazo de etapa da saga, que e de minutos e vive na configuracao.';

CREATE INDEX IF NOT EXISTS IX_ORDENS_SERVICO_CLIENTE ON ORDENS_SERVICO (CLIENTE_ID, DATA_CRIACAO DESC);
CREATE INDEX IF NOT EXISTS IX_ORDENS_SERVICO_VEICULO ON ORDENS_SERVICO (VEICULO_ID, DATA_CRIACAO DESC);
CREATE INDEX IF NOT EXISTS IX_ORDENS_SERVICO_STATUS ON ORDENS_SERVICO (STATUS, DATA_CRIACAO DESC);

COMMENT ON INDEX IX_ORDENS_SERVICO_CLIENTE IS 'A consulta do cliente e sempre "minhas ordens, da mais recente para a mais antiga".';
COMMENT ON INDEX IX_ORDENS_SERVICO_STATUS IS 'O painel da oficina filtra por estado. Sem este indice, filtrar por EM_EXECUCAO le a tabela inteira.';

CREATE TABLE IF NOT EXISTS ITENS_SERVICO (
    ID UUID NOT NULL,
    ORDEM_SERVICO_ID UUID NOT NULL,
    SERVICO_ID UUID NOT NULL,
    VALOR NUMERIC(12,2) NOT NULL,
    FEITO BOOLEAN NOT NULL DEFAULT FALSE,
    MECANICO_RESPONSAVEL_ID UUID,
    OBSERVACAO TEXT,
    DATA_REALIZACAO TIMESTAMPTZ(6),
    DATA_CRIACAO TIMESTAMPTZ(6) NOT NULL,
    DATA_ATUALIZACAO TIMESTAMPTZ(6) NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT FK_ITENS_SERVICO_ORDEM FOREIGN KEY (ORDEM_SERVICO_ID) REFERENCES ORDENS_SERVICO (ID) ON DELETE CASCADE,
    CONSTRAINT UQ_ITENS_SERVICO_ORDEM_SERVICO UNIQUE (ORDEM_SERVICO_ID, SERVICO_ID),
    CONSTRAINT CK_ITENS_SERVICO_VALOR CHECK (VALOR >= 0)
);

COMMENT ON TABLE ITENS_SERVICO IS 'Mao de obra contratada nesta OS, com o valor congelado no momento do orcamento. SERVICO_ID referencia o catalogo, sem chave estrangeira.';
COMMENT ON COLUMN ITENS_SERVICO.VALOR IS 'Valor cobrado nesta OS, congelado. O preco de referencia do catalogo pode mudar; o que foi orcado nao muda.';
COMMENT ON CONSTRAINT UQ_ITENS_SERVICO_ORDEM_SERVICO ON ITENS_SERVICO IS 'O mesmo servico duas vezes na mesma OS e erro de lancamento, nao quantidade.';

CREATE INDEX IF NOT EXISTS IX_ITENS_SERVICO_ORDEM ON ITENS_SERVICO (ORDEM_SERVICO_ID);

CREATE TABLE IF NOT EXISTS ITENS_INSUMO (
    ID UUID NOT NULL,
    ORDEM_SERVICO_ID UUID NOT NULL,
    INSUMO_ID UUID NOT NULL,
    QUANTIDADE NUMERIC(14,4) NOT NULL,
    DATA_CRIACAO TIMESTAMPTZ(6) NOT NULL,
    DATA_ATUALIZACAO TIMESTAMPTZ(6) NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT FK_ITENS_INSUMO_ORDEM FOREIGN KEY (ORDEM_SERVICO_ID) REFERENCES ORDENS_SERVICO (ID) ON DELETE CASCADE,
    CONSTRAINT UQ_ITENS_INSUMO_ORDEM_INSUMO UNIQUE (ORDEM_SERVICO_ID, INSUMO_ID),
    CONSTRAINT CK_ITENS_INSUMO_QUANTIDADE CHECK (QUANTIDADE > 0)
);

COMMENT ON TABLE ITENS_INSUMO IS 'Material que esta OS vai consumir, com quantidade. E a entrada de cada ReservarEstoque da saga: sem quantidade aqui, nao ha o que reservar.';
COMMENT ON COLUMN ITENS_INSUMO.QUANTIDADE IS 'Na unidade do insumo, que vive no catalogo. Quatro decimais porque ha insumo fracionavel, como litro de oleo.';
COMMENT ON CONSTRAINT UQ_ITENS_INSUMO_ORDEM_INSUMO ON ITENS_INSUMO IS 'Uma linha por insumo. O catalogo recusa reserva ativa repetida para a mesma ordem, entao duas linhas do mesmo insumo gerariam uma reserva recusada por construcao.';

CREATE INDEX IF NOT EXISTS IX_ITENS_INSUMO_ORDEM ON ITENS_INSUMO (ORDEM_SERVICO_ID);

CREATE TABLE IF NOT EXISTS HISTORICO_STATUS (
    ID UUID NOT NULL,
    ORDEM_SERVICO_ID UUID NOT NULL,
    STATUS_ANTERIOR VARCHAR(30),
    STATUS_NOVO VARCHAR(30) NOT NULL,
    MOTIVO VARCHAR(500),
    CORRELATION_ID VARCHAR(64),
    DATA_CRIACAO TIMESTAMPTZ(6) NOT NULL,
    PRIMARY KEY (ID),
    CONSTRAINT FK_HISTORICO_STATUS_ORDEM FOREIGN KEY (ORDEM_SERVICO_ID) REFERENCES ORDENS_SERVICO (ID) ON DELETE CASCADE
);

COMMENT ON TABLE HISTORICO_STATUS IS 'Uma linha por transicao de estado da OS. O enum guarda onde a OS esta; esta tabela guarda por onde passou, que e o que o cliente e a auditoria perguntam.';
COMMENT ON COLUMN HISTORICO_STATUS.STATUS_ANTERIOR IS 'Nulo na abertura da OS, que nao vem de transicao.';
COMMENT ON COLUMN HISTORICO_STATUS.MOTIVO IS 'Por que a transicao aconteceu. Em cancelamento por compensacao da saga, e aqui que fica a recusa do estoque.';
COMMENT ON COLUMN HISTORICO_STATUS.CORRELATION_ID IS 'Correlacao da operacao que causou a transicao. Liga a linha do historico ao log e ao trace daquela requisicao.';

CREATE INDEX IF NOT EXISTS IX_HISTORICO_STATUS_ORDEM ON HISTORICO_STATUS (ORDEM_SERVICO_ID, DATA_CRIACAO);

COMMENT ON INDEX IX_HISTORICO_STATUS_ORDEM IS 'O historico e sempre lido por OS, em ordem cronologica crescente.';

CREATE TABLE IF NOT EXISTS OUTBOX (
    ID UUID NOT NULL,
    AGREGADO_TIPO VARCHAR(40) NOT NULL,
    AGREGADO_ID UUID NOT NULL,
    CHAVE_PARTICAO VARCHAR(60) NOT NULL,
    TIPO_MENSAGEM VARCHAR(60) NOT NULL,
    VERSAO_MENSAGEM SMALLINT NOT NULL DEFAULT 1,
    TOPICO VARCHAR(120) NOT NULL,
    PAYLOAD JSONB NOT NULL,
    TRACEPARENT VARCHAR(64),
    DATA_CRIACAO TIMESTAMPTZ(6) NOT NULL,
    DATA_PUBLICACAO TIMESTAMPTZ(6),
    PRIMARY KEY (ID)
);

COMMENT ON TABLE OUTBOX IS 'Mensagem gravada na MESMA transacao que muda o dado, publicada depois por um leitor. Sem isso, gravar no banco e publicar na fila sao dois passos que falham em separado.';
COMMENT ON COLUMN OUTBOX.TIPO_MENSAGEM IS 'Nome do comando ou evento. Este servico publica comando, diferente dos servicos de dominio, que publicam evento.';
COMMENT ON COLUMN OUTBOX.TOPICO IS 'Destino da mensagem. Existe porque o orquestrador fala com mais de um servico, cada um com seu topico de comandos.';
COMMENT ON COLUMN OUTBOX.CHAVE_PARTICAO IS 'Chave da mensagem no broker: sempre o ORDEM_SERVICO_ID. Mesma ordem, mesma particao, ordem de entrega preservada entre os passos da saga.';
COMMENT ON COLUMN OUTBOX.TRACEPARENT IS 'Cabecalho W3C completo, nao so o traceId. O publicador roda em rotina agendada, fora do span de origem; sem o traceparent inteiro nao da para declarar paternidade e o consumidor entra num trace vizinho. Ver GLOBAL-ADR-010.';
COMMENT ON COLUMN OUTBOX.DATA_PUBLICACAO IS 'Nulo enquanto pendente. Preenchido pelo publicador depois do envio confirmado.';

CREATE INDEX IF NOT EXISTS IX_OUTBOX_PENDENTES ON OUTBOX (DATA_CRIACAO) WHERE DATA_PUBLICACAO IS NULL;

COMMENT ON INDEX IX_OUTBOX_PENDENTES IS 'O publicador le so o que falta enviar. Indice sobre a tabela inteira cresceria para sempre.';

CREATE TABLE IF NOT EXISTS INBOX (
    ID VARCHAR(120) NOT NULL,
    TIPO_MENSAGEM VARCHAR(60) NOT NULL,
    DATA_PROCESSAMENTO TIMESTAMPTZ(6) NOT NULL,
    PRIMARY KEY (ID)
);

COMMENT ON TABLE INBOX IS 'Chave de cada mensagem ja processada. Toda entrega e ao menos uma vez; a segunda colide na chave primaria e vira operacao nula.';
COMMENT ON COLUMN INBOX.ID IS 'Chave de idempotencia, escopada por tipo: <tipo>:<idMensagem>. Chave global descartaria operacao legitima de outro tipo com o mesmo identificador.';
