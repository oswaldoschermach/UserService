package com.nebula.userService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@Schema(description = "Envelope de resposta paginada usado nos endpoints de listagem e busca")
public class PaginatedResponseDTO<T> {
    @Schema(description = "Indice da pagina atual, iniciado em 0", example = "0")
    private int pagina;

    @Schema(description = "Quantidade de itens solicitada por pagina", example = "10")
    private int tamanho;

    @Schema(description = "Quantidade total de registros encontrados", example = "42")
    private long totalItens;

    @Schema(description = "Quantidade total de paginas disponiveis", example = "5")
    private int totalPaginas;

    @Schema(description = "Lista de itens da pagina atual")
    private List<T> items;

    public PaginatedResponseDTO(int pagina, int tamanho, long totalItens, int totalPaginas, List<T> items) {
        this.pagina = pagina;
        this.tamanho = tamanho;
        this.totalItens = totalItens;
        this.totalPaginas = totalPaginas;
        this.items = items;
    }

    public static <T> PaginatedResponseDTO<T> fromPage(Page<T> page) {
        return new PaginatedResponseDTO<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getContent()
        );
    }


}
