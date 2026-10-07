package com.clau.service_track.ordens.infrastructure.adapter.web.error

enum class CodigoErro(val descricao: String) {
    REQUISICAO_INVALIDA("Corpo ou parâmetro da requisição viola uma restrição declarada"),
    CORPO_ILEGIVEL("Corpo da requisição não pôde ser interpretado como JSON válido"),
    PARAMETRO_INVALIDO("Parâmetro de caminho ou de consulta com tipo incompatível"),
    REGRA_DE_NEGOCIO("Operação recusada por uma invariante do domínio"),
    CONFLITO_DE_ESTADO("Recurso não está no estado exigido pela operação"),
    RECURSO_NAO_ENCONTRADO("Nenhum recurso corresponde ao identificador informado"),
    METODO_NAO_SUPORTADO("Método HTTP não previsto para este recurso"),
    ERRO_INTERNO("Falha não prevista. Consulte o traceId no rastreamento distribuído"),
}
