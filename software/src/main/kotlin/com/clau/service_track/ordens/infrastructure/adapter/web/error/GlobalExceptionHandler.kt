package com.clau.service_track.ordens.infrastructure.adapter.web.error

import com.clau.service_track.ordens.application.exception.RecursoNaoEncontradoException
import com.clau.service_track.ordens.domain.DomainException
import com.clau.service_track.ordens.infrastructure.adapter.web.filter.CorrelacaoFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler(
    private val fabrica: FabricaDeErro,
) {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(RecursoNaoEncontradoException::class)
    fun recursoNaoEncontrado(e: RecursoNaoEncontradoException, requisicao: HttpServletRequest) =
        fabrica.montar(HttpStatus.NOT_FOUND, CodigoErro.RECURSO_NAO_ENCONTRADO, e.message.orEmpty(), requisicao)

    @ExceptionHandler(DomainException::class)
    fun regraDeNegocio(e: DomainException, requisicao: HttpServletRequest) =
        fabrica.montar(HttpStatus.UNPROCESSABLE_ENTITY, CodigoErro.REGRA_DE_NEGOCIO, e.message.orEmpty(), requisicao)

    @ExceptionHandler(IllegalStateException::class)
    fun conflitoDeEstado(e: IllegalStateException, requisicao: HttpServletRequest) =
        fabrica.montar(HttpStatus.CONFLICT, CodigoErro.CONFLITO_DE_ESTADO, e.message.orEmpty(), requisicao)

    @ExceptionHandler(IllegalArgumentException::class)
    fun argumentoInvalido(e: IllegalArgumentException, requisicao: HttpServletRequest) =
        fabrica.montar(HttpStatus.BAD_REQUEST, CodigoErro.REQUISICAO_INVALIDA, e.message.orEmpty(), requisicao)

    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun escritaConcorrente(e: OptimisticLockingFailureException, requisicao: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.warn("escrita concorrente na mesma ordem rota={}", CorrelacaoFilter.rotaDe(requisicao))
        return fabrica.montar(
            HttpStatus.CONFLICT,
            CodigoErro.CONFLITO_DE_ESTADO,
            "A ordem de serviço foi alterada por outra operação. Releia o recurso e tente de novo",
            requisicao,
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun corpoInvalido(e: MethodArgumentNotValidException, requisicao: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val violacoes = e.bindingResult.fieldErrors.map {
            Violacao(campo = it.field, mensagem = it.defaultMessage.orEmpty())
        }
        return fabrica.montar(
            HttpStatus.BAD_REQUEST,
            CodigoErro.REQUISICAO_INVALIDA,
            "Corpo da requisição contém ${violacoes.size} campo(s) inválido(s)",
            requisicao,
            violacoes,
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun restricaoViolada(e: ConstraintViolationException, requisicao: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val violacoes = e.constraintViolations.map {
            Violacao(campo = it.propertyPath.toString(), mensagem = it.message)
        }
        return fabrica.montar(
            HttpStatus.BAD_REQUEST,
            CodigoErro.REQUISICAO_INVALIDA,
            "Parâmetro da requisição contém ${violacoes.size} restrição(ões) violada(s)",
            requisicao,
            violacoes,
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun corpoIlegivel(e: HttpMessageNotReadableException, requisicao: HttpServletRequest) =
        fabrica.montar(
            HttpStatus.BAD_REQUEST,
            CodigoErro.CORPO_ILEGIVEL,
            "Corpo da requisição não é um JSON válido para esta operação",
            requisicao,
        )

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun parametroInvalido(e: MethodArgumentTypeMismatchException, requisicao: HttpServletRequest) =
        fabrica.montar(
            HttpStatus.BAD_REQUEST,
            CodigoErro.PARAMETRO_INVALIDO,
            "Parâmetro '${e.name}' não aceita o valor informado",
            requisicao,
        )

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun metodoNaoSuportado(e: HttpRequestMethodNotSupportedException, requisicao: HttpServletRequest) =
        fabrica.montar(
            HttpStatus.METHOD_NOT_ALLOWED,
            CodigoErro.METODO_NAO_SUPORTADO,
            "Método ${e.method} não é previsto para este recurso",
            requisicao,
        )

    @ExceptionHandler(Exception::class)
    fun naoPrevista(e: Exception, requisicao: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.error("falha nao prevista rota={}", CorrelacaoFilter.rotaDe(requisicao), e)
        return fabrica.montar(
            HttpStatus.INTERNAL_SERVER_ERROR,
            CodigoErro.ERRO_INTERNO,
            "Falha não prevista ao processar a requisição",
            requisicao,
        )
    }
}
