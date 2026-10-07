# Contratos de mensagem

Este serviço é o orquestrador da saga, então é ele quem **publica comando** e **consome
evento** — o inverso dos serviços de domínio. Os esquemas aqui são JSON Schema 2020-12.

| Tópico | Papel deste serviço | Dono do contrato |
|---|---|---|
| `servicetrack.estoque.comandos.v1` | produtor | **este serviço** |
| `servicetrack.estoque.eventos.v1` | consumidor | `service-track-catalogo` |

- [`estoque/comandos-de-estoque-v1.json`](estoque/comandos-de-estoque-v1.json)
- [`estoque/eventos-de-estoque-v1.json`](estoque/eventos-de-estoque-v1.json)

Como as etapas usam esses contratos: [`../saga/saga-da-ordem-de-servico.md`](../saga/saga-da-ordem-de-servico.md).

## Dono do contrato é quem decide, não quem publica primeiro

O par é assimétrico de propósito:

- **evento** pertence a quem o emite. O catálogo decide o que sai do estoque, e este serviço
  lê. Discordar não é opção: quem consome se adapta.
- **comando** pertence a quem o pede, não a quem o executa. A forma do pedido é decisão da
  saga; o catálogo declara apenas o que aceita.

Na prática isso significa que `eventos-de-estoque-v1.json` é **cópia** e
`comandos-de-estoque-v1.json` é **original** — e o arquivo espelho de cada um vive no outro
repositório. É a mesma assimetria que o contrato OpenAPI já tem, com o mesmo risco: cópia
divergindo sem ninguém notar.

## O que impede a cópia de divergir

Nada automático, e isso está escrito de propósito. O que existe:

1. **Teste de contrato no catálogo**, que valida cada evento que ele realmente produz contra o
   esquema, e cada comando do esquema contra os DTOs que ele aceita. Falha de divergência
   aparece em CI, do lado de quem pode consertar.
2. **Teste equivalente aqui**, na etapa 4, quando o produtor de comandos existir.
3. **Evolução aditiva obrigatória**: campo novo e tipo novo não mudam a versão; remoção ou
   troca de tipo mudam. Quebra de evento não dá erro HTTP — dá silêncio e dado errado.

Alterar um destes arquivos sem alterar o espelho é a falha que os dois testes existem para
pegar. Procedimento de mudança de contrato de mensagem: `GLOBAL-ADR-010`.

## Onde o contrato é apertado

| Campo | Restrição que não é óbvia |
|---|---|
| `idMensagem` do comando | 120 caracteres, e o catálogo acrescenta o tipo ao gravar no INBOX — orçamento detalhado no documento da saga |
| `expiraEm` de `ReservarEstoque` | obrigatório; o prazo do passo da saga é sempre menor que ele |
| `traceId` do envelope | redundância de depuração; o trace canônico é o cabeçalho `traceparent` |
| chave da mensagem no broker | `ordemServicoId`, sempre, em comando e em evento — é o que preserva a ordem dos passos |
