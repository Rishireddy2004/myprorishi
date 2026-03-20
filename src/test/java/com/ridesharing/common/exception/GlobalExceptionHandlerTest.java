package com.ridesharing.common.exception;

import com.ridesharing.common.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void validationError_returns400WithEnvelope() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getError().getMessage()).contains("must not be blank");
    }

    @Test
    void conflictException_returns409WithEnvelope() {
        ConflictException ex = new ConflictException("EMAIL_TAKEN", "Email is already in use");

        ResponseEntity<ErrorResponse> response = handler.handleConflict(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("EMAIL_TAKEN");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Email is already in use");
    }

    @Test
    void unprocessableEntityException_returns422WithEnvelope() {
        UnprocessableEntityException ex = new UnprocessableEntityException("CANCELLATION_WINDOW_CLOSED",
                "Cannot cancel within 2 hours of departure");

        ResponseEntity<ErrorResponse> response = handler.handleUnprocessable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("CANCELLATION_WINDOW_CLOSED");
    }

    @Test
    void resourceGoneException_returns410WithEnvelope() {
        ResourceGoneException ex = new ResourceGoneException("REVIEW_WINDOW_EXPIRED",
                "Review window has closed (7 days after trip completion)");

        ResponseEntity<ErrorResponse> response = handler.handleGone(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("REVIEW_WINDOW_EXPIRED");
    }

    @Test
    void externalServiceException_returns502WithEnvelope() {
        ExternalServiceException ex = new ExternalServiceException("GEOCODER_FAILURE",
                "Unable to geocode the provided address");

        ResponseEntity<ErrorResponse> response = handler.handleExternalService(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("GEOCODER_FAILURE");
    }

    @Test
    void genericException_returns500WithEnvelope() {
        Exception ex = new RuntimeException("Something went wrong");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
    }

    @Test
    void errorEnvelope_hasCorrectStructure() {
        ConflictException ex = new ConflictException("TEST_CODE", "Test message");

        ResponseEntity<ErrorResponse> response = handler.handleConflict(ex);

        // Verify the envelope wraps error under "error" key
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isNotNull();
        assertThat(response.getBody().getError().getMessage()).isNotNull();
    }
}
