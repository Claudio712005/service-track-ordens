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
| `OUTBOX` / `INBOX` | criadas agora, usadas na etapa da saga |

`OUTBOX.TRACEPARENT` guarda o cabeçalho W3C **completo**, não só o `traceId`: o publicador roda
em rotina agendada, fora do span de origem, e sem o cabeçalho inteiro não há como declarar
paternidade de span — o consumidor entraria num trace vizinho (`GLOBAL-ADR-010`).

Identificador de cliente, veículo, insumo e serviço é coluna `UUID` solta, **sem chave
estrangeira**: é dado de outro serviço e não existe join possível.

## Cobertura

Portão em linha 80%, instrução 80%, ramo 60%. Medido em 07/10/2026, com 139 testes:

| Contador | Atual | Portão |
|---|---|---|
| linha | 95,6% | 80% |
| instrução | 89,1% | 80% |
| ramo | 81,4% | 60% |

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

## O que ainda não existe

- **Saga e compensação**: orquestrador, produtor e consumidor Kafka, e o gatilho de falha
  embutido para a demonstração. `POST /ordens/{id}/execucao` e `POST /ordens/{id}/finalizacao`
  são **temporários**: hoje são chamados à mão, e passam a ser a confirmação da reserva e do
  consumo de insumos.
- **BDD**: um fluxo completo em Cucumber cobrindo a saga e a compensação.
- **Dockerfile, `k8s/`, `infra/terraform` e as esteiras.**
- **`oauth2-resource-server`**: ausente de propósito. Sem issuer configurado ele trancaria tudo;
  entra junto com a decisão de quais rotas protege.
- **Contratos de evento em JSON Schema**, adotando verbatim o esquema que o `catalogo` já
  consome — o consumidor dele existe, testado, e divergir custa retrabalho em código que
  funciona.

---

## Fronteiras

**É dono de:** estado da ordem de serviço, orçamento, itens de serviço, itens de insumo com
quantidade, a orquestração da saga, o contrato dos comandos de estoque, e o próprio banco.

**Não é dono e não altera:** insumo, saldo de estoque, usuário, veículo, pagamento, plataforma.

Precisou de dado alheio: chama a API do dono ou consome um evento dele. **Nunca o banco.**
