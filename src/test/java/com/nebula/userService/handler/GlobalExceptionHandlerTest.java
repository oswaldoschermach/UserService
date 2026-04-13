package com.nebula.userService.handler;

import com.nebula.userService.dto.ErrorResponseDTO;
import com.nebula.userService.exception.DatabaseIntegrityException;
import com.nebula.userService.exception.DuplicateEmailException;
import com.nebula.userService.exception.DuplicateUsernameException;
import com.nebula.userService.exception.UserNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    @DisplayName("BusinessException UserNotFoundException retorna 404")
    void handleBusinessException_UserNotFound_Returns404() {
        UserNotFoundException ex = new UserNotFoundException(1L);
        ResponseEntity<ErrorResponseDTO> response = handler.handleBusinessException(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).contains("1");
    }

    @Test
    @DisplayName("BusinessException DuplicateEmailException retorna 409")
    void handleBusinessException_DuplicateEmail_Returns409() {
        DuplicateEmailException ex = new DuplicateEmailException("joao@empresa.com");
        ResponseEntity<ErrorResponseDTO> response = handler.handleBusinessException(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).contains("joao@empresa.com");
    }

    @Test
    @DisplayName("BusinessException DuplicateUsernameException retorna 409")
    void handleBusinessException_DuplicateUsername_Returns409() {
        DuplicateUsernameException ex = new DuplicateUsernameException("joao.silva");
        ResponseEntity<ErrorResponseDTO> response = handler.handleBusinessException(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).contains("joao.silva");
    }


    @Test
    @DisplayName("BusinessException DatabaseIntegrityException retorna 409")
    void handleBusinessException_DatabaseIntegrity_Returns409() {
        DatabaseIntegrityException ex = new DatabaseIntegrityException("excluir");
        ResponseEntity<ErrorResponseDTO> response = handler.handleBusinessException(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("EntityNotFoundException retorna 404")
    void handleEntityNotFoundException_Returns404() {
        EntityNotFoundException ex = new EntityNotFoundException("Entidade nao encontrada");
        ResponseEntity<ErrorResponseDTO> response = handler.handleEntityNotFoundException(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Entidade nao encontrada");
    }

    @Test
    @DisplayName("DataIntegrityViolationException retorna 409")
    void handleDataIntegrityViolationException_Returns409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("constraint violation");
        ResponseEntity<ErrorResponseDTO> response = handler.handleDataIntegrityViolationException(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("IllegalArgumentException retorna 400")
    void handleIllegalArgumentException_Returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("ID invalido");
        ResponseEntity<ErrorResponseDTO> response = handler.handleIllegalArgumentException(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("ID invalido");
    }

    @Test
    @DisplayName("BadCredentialsException retorna 401")
    void handleBadCredentialsException_Returns401() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<ErrorResponseDTO> response = handler.handleAuthenticationException(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).contains("Credenciais");
    }

    @Test
    @DisplayName("AuthenticationServiceException retorna 401")
    void handleAuthenticationServiceException_Returns401() {
        AuthenticationServiceException ex = new AuthenticationServiceException("Authentication failed");
        ResponseEntity<ErrorResponseDTO> response = handler.handleAuthenticationException(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("ResponseStatusException retorna status correto")
    void handleResponseStatusException_Returns503() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service down");
        ResponseEntity<ErrorResponseDTO> response = handler.handleResponseStatusException(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Excecao generica retorna 500")
    void handleGenericException_Returns500() {
        Exception ex = new RuntimeException("Erro inesperado");
        ResponseEntity<ErrorResponseDTO> response = handler.handleGenericException(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).contains("inesperado");
    }

    @Test
    @DisplayName("Response body contem path e timestamp")
    void handleBusinessException_ResponseBodyContainsPathAndTimestamp() {
        UserNotFoundException ex = new UserNotFoundException(1L);
        ResponseEntity<ErrorResponseDTO> response = handler.handleBusinessException(ex, request);
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
    }
}