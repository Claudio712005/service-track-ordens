# service-track-ordens

Microsserviço de **ordem de serviço** da plataforma ServiceTrack, e **orquestrador da saga**
(`GLOBAL-ADR-007`).

É dono do estado da OS e de quem sabe em que etapa ela está. Não conhece insumo, não conhece
usuário e não lê o banco de nenhum outro serviço: identificador alheio é referência opaca.

---

## O domínio não foi escrito, foi portado

O domínio já existia no monólito da Fase 2 (`service-track-api`, módulo `_domain`), com máquina
de estado explícita e 838 linhas de teste já avaliadas. Portar preserva modelagem entregue;
reescrever jogaria fora.

```
RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → FINALIZADA → ENTREGUE
   └──────────┴──────────────────┴─────────────────┘ → CANCELADA
```

As transições são declaradas em `StatusOrdemServico.podeTransitarPara`, e cada operação do
agregado confere o estado antes de agir.

### Três desvios deliberados do original

| Desvio | Por quê |
|---|---|
| `UsuarioId`, `VeiculoId`, `InsumoId` e `ServicoId` em `domain/referencia` | são dado de outros serviços; este não importa tipo de ninguém e não faz join com ninguém |
| `StatusOrdemServicoEnum` perdeu `descricao` e `corStatus` | cor de status em enum de domínio é apresentação vazando para dentro — quem pinta é o cliente |
| insumo passou a ter quantidade, em `ItemInsumo` | o original guardava `List<InsumoId>` e `ReservarEstoque` exige quantidade: a saga reservaria "óleo", não 3 litros de óleo |
| `aprovarOrcamento()` não transiciona mais | a saga de reserva abre na aprovação e o estado só avança na confirmação, em `iniciarExecucao()` |
| `StatusOrdemServico.de` recebe o nome do status, não um índice | persistir o nome sobrevive a reordenar o enum; um índice não |

O domínio é Kotlin puro: nenhum arquivo importa Spring.

---

## Arquitetura do serviço

Hexagonal, com os mesmos nomes de camada do catálogo (`GLOBAL-ADR-008`).

```
domain/            agregado, VOs, máquina de estado — portado, sem dependência de framework
application/
  port/in/api      contrato HTTP: OrdemServicoApiPort e os DTOs
  port/in/useCase  um comando por caso de uso, e a porta que o executa
  port/out         repositório, histórico e correlação
  handler/         OrdemServicoCommandHandler e OrdemServicoQueryHandler
infrastructure/
  adapter/in       controller e mapeador web
  adapter/out      Postgres, mapeador do agregado, correlação por MDC
  adapter/web      filtro de correlação e tratamento global de erro
  entity/postgres  entidades JPA
```

### Três decisões que não se leem no código

**Um comando por caso de uso.** Um comando compartilhado entre portas colidiria na assinatura
de `executar` e forçaria stub ou nome de método ad hoc. Com um tipo por caso de uso, o despacho
é por tipo, e campo obrigatório é obrigatório no próprio tipo — não numa validação no meio do
handler.

**O histórico está fora do agregado.** Ele cresce sem limite e não participa de nenhuma
invariante da OS. Carregar junto pagaria a leitura do histórico inteiro em toda operação de
escrita. Toda escrita passa por `mutar()`, que compara o estado antes e depois e grava uma linha
**só quando houve transição** — adicionar insumo não suja a trilha.

**A correlação é porta, não `MDC.get` no handler.** Ler MDC na camada de aplicação seria
infraestrutura vazando para dentro; e o histórico precisa dela para ligar cada transição ao log
e ao trace da requisição que a causou.

## A saga

Este serviço é o **orquestrador** (`GLOBAL-ADR-007`). O contrato está em
[`docs/saga/saga-da-ordem-de-servico.md`](docs/saga/saga-da-ordem-de-servico.md); o que segue é
como ele foi implementado.

```
aprovar orçamento ──> saga RESERVA ──┬─ todos confirmam ──> EM_EXECUCAO
                                     └─ um recusa ────────> libera o reservado ──> CANCELADA

pedir finalização ──> saga CONSUMO ──┬─ todos confirmam ──> FINALIZADA
                                     └─ um recusa ────────> FALHA, ordem bloqueada
```

**A OS não ganha estado intermediário.** O progresso vive em `SAGAS` e `SAGA_PASSOS`; o cliente
vê só o estado da ordem. `GET /ordens/{id}/saga` mostra o progresso a quem opera.

### Quatro coisas que não se adivinham lendo o código

**A tentativa está na chave de idempotência.** `<ordemServicoId>:<ETAPA>:<insumoId>:<tentativa>`.
O catálogo considera processada qualquer reentrega com a mesma chave — **inclusive a recusa**,
que é resposta de negócio bem-sucedida e fica registrada no INBOX dele. Retentar o consumo com a
chave da primeira tentativa receberia silêncio, e a saga esperaria para sempre.

**Consumo não compensa, mas retenta.** Baixar o reservado é irreversível pelo contrato do
catálogo, então o consumo é o último passo com efeito externo. `ConsumoRecusado` leva a saga a
`FALHA` e a OS **fica em `EM_EXECUCAO`**, com uma linha no histórico dizendo que está bloqueada —
sem isso a falha viveria só num `ERROR` de log, onde nem o atendente nem o cliente olham. Daí
`POST /ordens/{id}/saga/consumo` retenta depois da reposição, ou a OS é cancelada.

**A compensação aceita uma janela de atraso.** Libera só os passos que confirmaram antes da
recusa; se uma confirmação chegar depois, aquela reserva fica presa até o `expiraEm` vencer no
catálogo. É escolha registrada: a rede de segurança de lá já existe, e liberar preventivamente
custaria mensagem inútil no caminho normal.

**O `traceparent` completo vai para a OUTBOX.** O publicador roda em rotina agendada, fora do
span de origem, e restaura o contexto antes de enviar. Sem isso o consumidor entraria num trace
vizinho — e o teste que garante isso foi verificado com controle negativo: desligando a
restauração, ele falha apontando que o trace publicado é outro.

### O prazo é deste serviço

| Relógio | Padrão | Papel |
|---|---|---|
| prazo do passo | 2 min | reprova a etapa e dispara a compensação |
| `expiraEm` da reserva | 10 min | rede de segurança do catálogo para saga abandonada |
| varredura de prazo | 30 s | `FOR UPDATE SKIP LOCKED`, uma saga por transação |

A varredura trava a linha porque, com mais de uma réplica, todas varreriam as mesmas sagas.

### O cenário é executável

`src/test/resources/features/saga-da-ordem-de-servico.feature`, em português, cinco cenários:
fluxo completo, compensação por recusa, compensação por prazo, bloqueio e retentativa do
consumo, e ordem sem insumo. É o roteiro da demonstração, e roda em `./gradlew check`.

```bash
cd software && ./gradlew test --tests '*ExecutorDeCenarios*'
```

**O gatilho de falha está no seed do catálogo**, não aqui: o insumo `OL-20W50-MIN-1L` abre com
saldo zero e recusa toda reserva. Abrir uma OS com ele é a demonstração da compensação, sem
mexer em infraestrutura durante a gravação.

## O banco

Schema `ORDENS` no `st_ord`, sete tabelas em [`db/postgres/01_baseline_st_ord.sql`](db/postgres/01_baseline_st_ord.sql).
`ddl-auto: validate` — o baseline **é** o schema, o Hibernate só confere.

| Tabela | Por que existe |
|---|---|
| `ORDENS_SERVICO` | agregado raiz, com trava otimista |
| `ORCAMENTOS` | ciclo próprio de aprovação; o valor total é derivado, não armazenado |
| `ITENS_SERVICO` | mão de obra, com o valor congelado no orçamento |
| `ITENS_INSUMO` | material **com quantidade** — é a entrada de cada `ReservarEstoque` |
| `HISTORICO_STATUS` | por onde a OS passou, que o enum não responde |
| `SAGAS` / `SAGA_PASSOS` | progresso da transação distribuída, com trava otimista |
| `OUTBOX` / `INBOX` | mensagem gravada na mesma transação do dado, e idempotência do consumidor |

`OUTBOX.TRACEPARENT` guarda o cabeçalho W3C **completo**, não só o `traceId`: o publicador roda
em rotina agendada, fora do span de origem, e sem o cabeçalho inteiro não há como declarar
paternidade de span — o consumidor entraria num trace vizinho (`GLOBAL-ADR-010`).

Identificador de cliente, veículo, insumo e serviço é coluna `UUID` solta, **sem chave
estrangeira**: é dado de outro serviço e não existe join possível.

## Cobertura

Portão em linha 80%, instrução 80%, ramo 60%. Medido em 07/10/2026, com 187 testes e 5 cenários BDD:

| Contador | Atual | Portão |
|---|---|---|
| linha | 93,8% | 80% |
| instrução | 87,0% | 80% |
| ramo | 77,7% | 60% |

```bash
cd software && ./gradlew check
```

Os adaptadores de repositório são exercitados contra **H2 em modo PostgreSQL** com o schema
gerado pelo Hibernate. Isso cobre o mapeamento e a JPQL, e **não** cobre o acordo entre as
entidades e o baseline escrito à mão — quem confere isso é o `ddl-auto: validate` no start, e um
teste com Postgres de verdade entra junto com a esteira.

## Ambiente local

[`docs/ambiente-local.md`](docs/ambiente-local.md). Resumo: `docker compose up -d postgres` e
`./gradlew bootRun --args='--spring.profiles.active=dev'`. O Postgres sobe em **5433**, para não
colidir com o do catálogo.

---

## O broker é deste serviço

Um broker por ambiente, hospedado aqui, por `GLOBAL-ADR-011`. Quem orquestra não pode ficar sem fila: se o broker vivesse no participante, o orquestrador
passaria a depender da infraestrutura de quem ele comanda.

O `service-track-catalogo` consome deste broker e **não** hospeda um próprio nos ambientes
compartilhados.

### O endereço é FQDN nos dois lugares, e isso não é redundância

```
service-track-ordens-kafka.service-track-ordens.svc.cluster.local:9092
```

| Onde | Usado quando |
|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | primeira conexão do cliente |
| `KAFKA_ADVERTISED_LISTENERS` do broker | **segunda** conexão, no endereço que o broker anuncia |

Cliente Kafka conecta no bootstrap e reconecta no endereço anunciado. Com nome curto, cliente de
outro namespace conecta e **trava no metadata** — erro que não diz o que é. Trocar só o primeiro
dos dois é o defeito, e ele aparece depois de parecer ter funcionado.

### Hospedar o broker não dá voz no contrato alheio

`servicetrack.estoque.eventos.v1` é do catálogo, que emite. Este serviço só hospeda. A
propriedade segue o assunto, não o processo — `GLOBAL-ADR-010`.

### Broker ausente derruba a subida

`KAFKA_ADMIN_FAIL_FAST=true` nos três overlays e no smoke da esteira. Sem isso, mensageria
ligada sem broker não dá erro de subida: dá retentativa infinita e OUTBOX crescendo em silêncio.
Um pod em `CrashLoopBackOff` com causa no log é mais barato.

### `local` tem broker próprio, e isso é divergência declarada

O overlay `local` existe para subir **um** repositório sozinho. O endereço tem o mesmo formato
que em `hml`, mas o broker é o deste repositório. O que `local` não serve para testar é a saga
entre serviços; isso é `@EmbeddedKafka` em teste, e `hml` ao vivo.

---

## Contrato da saga

Escrito antes do código, de propósito: a saga atravessa três serviços e um contrato descoberto
durante a implementação já nasce com um consumidor dependendo dele.

- [`docs/saga/saga-da-ordem-de-servico.md`](docs/saga/saga-da-ordem-de-servico.md) — etapas,
  compensação, hierarquia dos prazos e chave de idempotência.
- [`docs/contratos/`](docs/contratos/) — JSON Schema dos comandos que este serviço publica e
  dos eventos que consome.

Os esquemas do estoque foram adotados **verbatim** do `service-track-catalogo`: o consumidor
dele já existe e está testado, e divergir custaria retrabalho em código que funciona. O que
mudou do original, e mudou porque a saga exigiu, está registrado na `GLOBAL-ADR-010`.

---

## Fronteiras

**É dono de:** estado da ordem de serviço, orçamento, itens de serviço, itens de insumo com
quantidade, a orquestração da saga, o contrato dos comandos de estoque, e o próprio banco.

**Não é dono e não altera:** insumo, saldo de estoque, usuário, veículo, pagamento, plataforma.

Precisou de dado alheio: chama a API do dono ou consome um evento dele. **Nunca o banco.**
