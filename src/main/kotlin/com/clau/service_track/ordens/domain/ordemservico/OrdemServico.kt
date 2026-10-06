package com.clau.service_track.ordens.domain.ordemservico

import com.clau.service_track.ordens.domain.referencia.InsumoId
import com.clau.service_track.ordens.domain.orcamento.Orcamento
import com.clau.service_track.ordens.domain.ordemservico.vo.ItemOrdemServicoId
import com.clau.service_track.ordens.domain.ordemservico.vo.OrdemServicoId
import com.clau.service_track.ordens.domain.ordemservico.vo.PrazoConclusao
import com.clau.service_track.ordens.domain.ordemservico.vo.StatusOrdemServico
import com.clau.service_track.ordens.domain.referencia.ServicoId
import com.clau.service_track.ordens.domain.DomainException
import com.clau.service_track.ordens.domain.vo.ValorMonetario
import com.clau.service_track.ordens.domain.referencia.UsuarioId
import com.clau.service_track.ordens.domain.referencia.VeiculoId
import java.time.LocalDateTime

class OrdemServico private constructor(
    val id: OrdemServicoId,
    val motivo: String,
    var observacao: String,
    val clienteId: UsuarioId,
    private var mecanicoId: UsuarioId,
    val veiculoId: VeiculoId,
    val dataCriacao: LocalDateTime,
    var dataAtualizacao: LocalDateTime,
    private var status: StatusOrdemServico,
    private var prazoConclusao: PrazoConclusao?,
    private var orcamento: Orcamento?,
    private val insumos: MutableList<InsumoId>,
    private val itensServico: MutableList<ItemOrdemServico>,
) {

    companion object {

        fun abrir(
            motivo: String,
            clienteId: UsuarioId,
            mecanicoId: UsuarioId,
            veiculoId: VeiculoId,
            observacao: String = ""
        ): OrdemServico {
            require(motivo.isNotBlank()) { "Motivo da OS não pode ser vazio" }

            val agora = LocalDateTime.now()

            return OrdemServico(
                id = OrdemServicoId.gerar(),
                motivo = motivo,
                observacao = observacao,
                clienteId = clienteId,
                mecanicoId = mecanicoId,
                veiculoId = veiculoId,
                dataCriacao = agora,
                dataAtualizacao = agora,
                status = StatusOrdemServico.deEnum(StatusOrdemServicoEnum.RECEBIDA),
                prazoConclusao = null,
                orcamento = null,
                insumos = mutableListOf(),
                itensServico = mutableListOf(),
            )
        }

        fun reconstituir(
            id: OrdemServicoId,
            motivo: String,
            observacao: String,
            clienteId: UsuarioId,
            mecanicoId: UsuarioId,
            veiculoId: VeiculoId,
            dataCriacao: LocalDateTime,
            dataAtualizacao: LocalDateTime,
            status: StatusOrdemServico,
            prazoConclusao: PrazoConclusao?,
            orcamento: Orcamento?,
            insumos: MutableList<InsumoId>,
            itensServico: MutableList<ItemOrdemServico>,
        ): OrdemServico = OrdemServico(
            id = id,
            motivo = motivo,
            observacao = observacao,
            clienteId = clienteId,
            mecanicoId = mecanicoId,
            veiculoId = veiculoId,
            dataCriacao = dataCriacao,
            dataAtualizacao = dataAtualizacao,
            status = status,
            prazoConclusao = prazoConclusao,
            orcamento = orcamento,
            insumos = insumos,
            itensServico = itensServico,
        )
    }


    fun obterStatus(): StatusOrdemServicoEnum = status.valor

    fun obterOrcamento(): Orcamento? = orcamento

    fun listarInsumos(): List<InsumoId> = insumos.toList()

    fun obterMecanicoId(): UsuarioId = mecanicoId

    fun obterPrazoConclusao(): LocalDateTime? = prazoConclusao?.valor

    fun listarServicos(): List<ItemOrdemServico> = itensServico.toList()

    fun iniciarDiagnostico() {
        alterarStatus(StatusOrdemServicoEnum.EM_DIAGNOSTICO)
    }

    fun finalizar() {
        alterarStatus(StatusOrdemServicoEnum.FINALIZADA)
    }

    fun entregar() {
        alterarStatus(StatusOrdemServicoEnum.ENTREGUE)
    }

    fun cancelar(motivo: String = "") {
        if (motivo.isNotBlank()) {
            observacao += "\nCancelada: $motivo"
        }
        alterarStatus(StatusOrdemServicoEnum.CANCELADA)
    }

    fun adicionarInsumo(insumoId: InsumoId) {
        check(status.valor == StatusOrdemServicoEnum.EM_DIAGNOSTICO) {
            "Insumos só podem ser adicionados durante o diagnóstico"
        }
        insumos.add(insumoId)
        dataAtualizacao = LocalDateTime.now()
    }

    fun removerInsumo(insumoId: InsumoId) {
        check(status.valor == StatusOrdemServicoEnum.EM_DIAGNOSTICO) {
            "Insumos só podem ser removidos durante o diagnóstico"
        }
        check(insumos.remove(insumoId)) { "Insumo não encontrado na OS" }
        dataAtualizacao = LocalDateTime.now()
    }

    fun gerarOrcamento(custoMaoDeObra: ValorMonetario, custoInsumos: ValorMonetario) {
        check(status.valor == StatusOrdemServicoEnum.EM_DIAGNOSTICO) {
            "Orçamento só pode ser gerado durante o diagnóstico"
        }
        orcamento = Orcamento.gerar(custoMaoDeObra, custoInsumos)
        alterarStatus(StatusOrdemServicoEnum.AGUARDANDO_APROVACAO)
    }

    fun aprovarOrcamento() {
        val orc = orcamento ?: throw DomainException("OS não possui orçamento gerado")
        check(status.valor == StatusOrdemServicoEnum.AGUARDANDO_APROVACAO) {
            "Aprovação só é possível quando a OS está aguardando aprovação"
        }
        orc.aprovar()
        alterarStatus(StatusOrdemServicoEnum.EM_EXECUCAO)
    }

    fun reprovarOrcamento(motivo: String) {
        val orc = orcamento ?: throw DomainException("OS não possui orçamento gerado")
        check(status.valor == StatusOrdemServicoEnum.AGUARDANDO_APROVACAO) {
            "Reprovação só é possível quando a OS está aguardando aprovação"
        }
        orc.reprovar(motivo)
        alterarStatus(StatusOrdemServicoEnum.CANCELADA)
    }

    fun definirPrazoConclusao(prazo: LocalDateTime) {
        check(prazoConclusao == null) { "Prazo de conclusão já definido" }
        if (prazo.isBefore(LocalDateTime.now())) {
            throw DomainException("Prazo de conclusão não pode estar no passado")
        }
        prazoConclusao = PrazoConclusao(prazo)
        dataAtualizacao = LocalDateTime.now()
    }

    fun reassinarMecanico(novoMecanicoId: UsuarioId) {
        check(mecanicoId != novoMecanicoId) { "O mecânico já está atribuído a esta OS" }
        mecanicoId = novoMecanicoId
        dataAtualizacao = LocalDateTime.now()
    }

    fun adicionarServico(servicoId: ServicoId, valor: ValorMonetario): ItemOrdemServico {
        check(status.valor == StatusOrdemServicoEnum.EM_DIAGNOSTICO) {
            "Serviços só podem ser adicionados durante o diagnóstico"
        }
        check(itensServico.none { it.servicoId == servicoId }) {
            "Serviço já adicionado à OS"
        }

        val item = ItemOrdemServico.criar(
            servicoId = servicoId,
            ordemServicoId = id,
            valor = valor,
        )
        itensServico.add(item)
        dataAtualizacao = LocalDateTime.now()
        return item
    }

    fun removerServico(servicoId: ServicoId) {
        check(status.valor == StatusOrdemServicoEnum.EM_DIAGNOSTICO) {
            "Serviços só podem ser removidos durante o diagnóstico"
        }
        val item = itensServico.find { it.servicoId == servicoId }
            ?: throw DomainException("Serviço não encontrado nesta OS")
        check(!item.feito) { "Não é possível remover um serviço já concluído da OS" }
        itensServico.remove(item)
        dataAtualizacao = LocalDateTime.now()
    }

    fun concluirItemServico(itemId: ItemOrdemServicoId, mecanicoId: UsuarioId, observacao: String) {
        check(status.valor == StatusOrdemServicoEnum.EM_EXECUCAO) {
            "Itens de serviço só podem ser concluídos quando a OS está em execução"
        }
        val item = itensServico.find { it.id == itemId }
            ?: throw DomainException("Item de serviço não encontrado nesta OS")
        if (item.mecanicoResponsavelId == null) {
            item.vincularMecanico(mecanicoId)
        }
        item.concluir(observacao)
        dataAtualizacao = LocalDateTime.now()
    }

    private fun alterarStatus(novoStatus: StatusOrdemServicoEnum) {
        status = status.transitarPara(novoStatus)
        dataAtualizacao = LocalDateTime.now()
    }
}
