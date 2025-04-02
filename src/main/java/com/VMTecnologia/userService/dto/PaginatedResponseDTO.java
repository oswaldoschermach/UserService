package com.VMTecnologia.userService.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
public class PaginatedResponseDTO<T> {
    private int pagina;
    private int tamanho;
    private long totalItens;
    private int totalPaginas;
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