# Ambiente local

Este serviço precisa de um Postgres e de nada mais. Kafka entra na etapa da saga, quando
houver produtor e consumidor para justificá-lo.

## Subir

```bash
docker compose up -d postgres
cd software && ./gradlew bootRun --args='--spring.profiles.active=dev'
```

O `compose` sobe **só o banco**. A aplicação roda pelo Gradle porque o `Dockerfile` ainda não
existe — ele entra junto com `k8s/` e as esteiras. Compose referenciando imagem que não se
constrói é pior que compose sem a aplicação.

| Endereço | O que é |
|---|---|
| `http://localhost:8080/swagger-ui.html` | contrato navegável |
| `http://localhost:8080/actuator/health` | saúde, com o banco incluído |
| `http://localhost:8080/actuator/prometheus` | métricas |
| `localhost:5433` | Postgres — **5433**, não 5432, para não colidir com o do catálogo |

O perfil `dev` traz as credenciais locais como padrão e sobe o log da aplicação para `DEBUG`.
Fora dele, `ST_ORD_DB_URL`, `ST_ORD_DB_USER` e `ST_ORD_DB_PASSWORD` são obrigatórios e a
aplicação não sobe sem eles, de propósito: endpoint e senha de banco são lidos em tempo de uso,
nunca embutidos.

## O schema é o baseline, não o Hibernate

`ddl-auto: validate`. O schema vem de [`../db/postgres/01_baseline_st_ord.sql`](../db/postgres/01_baseline_st_ord.sql),
que o Postgres do compose executa no primeiro start. Se a aplicação não sobe reclamando de
coluna, **a entidade divergiu do baseline** — o remédio é corrigir um dos dois, nunca ligar
`ddl-auto: update`.

Recriar o schema do zero:

```bash
docker compose down -v && docker compose up -d postgres
```

## Exercitar o ciclo completo

```bash
API=http://localhost:8080
CLIENTE=$(uuidgen | tr 'A-Z' 'a-z'); MECANICO=$(uuidgen | tr 'A-Z' 'a-z')
VEICULO=$(uuidgen | tr 'A-Z' 'a-z'); INSUMO=$(uuidgen | tr 'A-Z' 'a-z')
SERVICO=$(uuidgen | tr 'A-Z' 'a-z')

OS=$(curl -s -X POST $API/ordens -H 'Content-Type: application/json' \
  -d "{\"motivo\":\"barulho na suspensao\",\"clienteId\":\"$CLIENTE\",\"mecanicoId\":\"$MECANICO\",\"veiculoId\":\"$VEICULO\"}" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')

curl -s -X POST $API/ordens/$OS/diagnostico > /dev/null
curl -s -X POST $API/ordens/$OS/insumos -H 'Content-Type: application/json' \
  -d "{\"insumoId\":\"$INSUMO\",\"quantidade\":4.5}" > /dev/null
curl -s -X POST $API/ordens/$OS/servicos -H 'Content-Type: application/json' \
  -d "{\"servicoId\":\"$SERVICO\",\"valor\":180.00}" > /dev/null
curl -s -X POST $API/ordens/$OS/orcamento -H 'Content-Type: application/json' \
  -d '{"custoMaoDeObra":180.00,"custoInsumos":220.50}' > /dev/null
curl -s -X POST $API/ordens/$OS/orcamento/aprovacao > /dev/null
curl -s -X POST $API/ordens/$OS/execucao > /dev/null
curl -s -X POST $API/ordens/$OS/finalizacao > /dev/null
curl -s -X POST $API/ordens/$OS/entrega > /dev/null

curl -s $API/ordens/$OS/historico | python3 -m json.tool
```

Dois pontos do roteiro acima merecem atenção, porque não são óbvios:

1. **Aprovar o orçamento não muda o estado.** A OS fica em `AGUARDANDO_APROVACAO` e só avança
   com `POST /execucao`, que hoje é chamado à mão e na etapa da saga passa a ser a confirmação
   da reserva de insumos no catálogo.
2. **Os identificadores de insumo, serviço, cliente e veículo são inventados aqui.** Este
   serviço não valida existência de dado alheio: ele não lê o banco de ninguém. Quem valida é
   quem é dono, no momento em que a saga pede a reserva.
