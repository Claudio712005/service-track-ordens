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
| `StatusOrdemServico.de` recebe o nome do status, não um índice | persistir o nome sobrevive a reordenar o enum; um índice não |

O domínio é Kotlin puro: nenhum arquivo importa Spring.

---

## Cobertura

Portão no build: `./gradlew check` falha abaixo do mínimo. Medido em 06/10/2026:

| Métrica | Atual | Mínimo |
|---|---|---|
| Linha | **93,9%** | 80% |
| Instrução | **85,7%** | 80% |
| Ramo | **82,3%** | 60% |

Está alto porque hoje só existe domínio, que é a camada mais fácil de cobrir. O número cai
quando entrarem aplicação, persistência e mensageria — o portão é o que impede que caia abaixo
do exigido.

```bash
./gradlew test      # 71 testes
./gradlew check     # testes mais o portao de cobertura
```

---

## O que ainda não existe

- **Aplicação e persistência**: portas, handlers, Postgres próprio, INBOX/OUTBOX, REST de
  abertura e consulta de status.
- **Saga e compensação**: orquestrador, produtor e consumidor Kafka, e o gatilho de falha
  embutido para a demonstração.
- **BDD**: um fluxo completo em Cucumber cobrindo a saga e a compensação.
- **Dockerfile, `k8s/`, `infra/terraform` e as esteiras.**
- **Contratos de evento em JSON Schema**, adotando verbatim o esquema que o `catalogo` já
  consome — o consumidor dele existe, testado, e divergir custa retrabalho em código que
  funciona.

---

## Fronteiras

**É dono de:** estado da ordem de serviço, orçamento, itens de serviço, a orquestração da saga,
o contrato dos comandos de estoque, e o próprio banco.

**Não é dono e não altera:** insumo, saldo de estoque, usuário, veículo, pagamento, plataforma.

Precisou de dado alheio: chama a API do dono ou consome um evento dele. **Nunca o banco.**
