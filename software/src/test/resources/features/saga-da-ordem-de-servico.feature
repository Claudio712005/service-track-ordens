# language: pt
Funcionalidade: Saga da ordem de serviço
  A ordem de serviço só avança de estado quando o catálogo confirma o que a saga pediu.
  Uma recusa libera o que já tinha sido reservado e cancela a ordem.

  Contexto:
    Dado uma ordem de serviço aberta em diagnóstico

  Cenário: fluxo completo, com o catálogo confirmando tudo
    Dado que a ordem precisa de 2 unidades do insumo "oleo"
    E que a ordem precisa de 1 unidade do insumo "filtro"
    Quando o orçamento de 300.00 de mão de obra e 180.00 de insumos é gerado
    E o orçamento é aprovado
    Então a ordem continua em "AGUARDANDO_APROVACAO"
    E a saga de "RESERVA" está "EM_CURSO" com 2 passos pendentes
    E um comando "ReservarEstoque" foi publicado para cada insumo

    Quando o catálogo confirma a reserva do insumo "oleo"
    Então a ordem continua em "AGUARDANDO_APROVACAO"

    Quando o catálogo confirma a reserva do insumo "filtro"
    Então a ordem está em "EM_EXECUCAO"
    E a saga de "RESERVA" está "CONCLUIDA" com 0 passos pendentes

    Quando a finalização é pedida
    E o catálogo confirma o consumo do insumo "oleo"
    E o catálogo confirma o consumo do insumo "filtro"
    Então a ordem está em "FINALIZADA"

  Cenário: reserva recusada compensa o que deu certo e cancela a ordem
    Dado que a ordem precisa de 2 unidades do insumo "oleo"
    E que a ordem precisa de 1 unidade do insumo "filtro"
    Quando o orçamento de 300.00 de mão de obra e 180.00 de insumos é gerado
    E o orçamento é aprovado
    E o catálogo confirma a reserva do insumo "oleo"
    E o catálogo recusa a reserva do insumo "filtro" por "solicitado 1 UNIDADE, disponivel 0 UNIDADE"

    Então a saga de "RESERVA" está "COMPENSANDO" com 1 passos pendentes
    E um comando "LiberarReserva" foi publicado para o insumo "oleo"
    E nenhum comando "LiberarReserva" foi publicado para o insumo "filtro"
    E a ordem continua em "AGUARDANDO_APROVACAO"

    Quando o catálogo confirma a liberação do insumo "oleo"
    Então a ordem está em "CANCELADA"
    E a saga de "RESERVA" está "COMPENSADA" com 0 passos pendentes
    E o histórico da ordem registra "disponivel 0 UNIDADE"

  Cenário: prazo da etapa vence sem resposta e a saga compensa sozinha
    Dado que a ordem precisa de 2 unidades do insumo "oleo"
    E que a ordem precisa de 1 unidade do insumo "filtro"
    Quando o orçamento de 300.00 de mão de obra e 180.00 de insumos é gerado
    E o orçamento é aprovado
    E o catálogo confirma a reserva do insumo "oleo"
    E o prazo da etapa vence
    E a varredura de prazo roda

    Então a saga de "RESERVA" está "COMPENSANDO" com 1 passos pendentes
    E um comando "LiberarReserva" foi publicado para o insumo "oleo"

  Cenário: consumo recusado bloqueia a ordem, e a reposição permite retentar
    Dado que a ordem precisa de 2 unidades do insumo "oleo"
    Quando o orçamento de 300.00 de mão de obra e 180.00 de insumos é gerado
    E o orçamento é aprovado
    E o catálogo confirma a reserva do insumo "oleo"
    E a finalização é pedida
    E o catálogo recusa o consumo do insumo "oleo" por "Nenhuma reserva ativa deste insumo"

    Então a ordem continua em "EM_EXECUCAO"
    E a saga de "CONSUMO" está "FALHA" com 0 passos pendentes
    E o histórico da ordem registra "ordem bloqueada na etapa CONSUMO_DE_INSUMOS"
    E o histórico da ordem tem 1 fato sem transição

    Quando a finalização é pedida
    Então a saga de "CONSUMO" está "EM_CURSO" com 1 passos pendentes
    E a saga de "CONSUMO" está na tentativa 2
    E o comando republicado para o insumo "oleo" tem chave de tentativa 2

    Quando o catálogo confirma o consumo do insumo "oleo"
    Então a ordem está em "FINALIZADA"

  Cenário: ordem sem insumo não precisa de saga
    Quando o orçamento de 300.00 de mão de obra e 0.00 de insumos é gerado
    E o orçamento é aprovado
    Então a ordem está em "EM_EXECUCAO"
    E não existe saga de "RESERVA"

    Quando a finalização é pedida
    Então a ordem está em "FINALIZADA"
