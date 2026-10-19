package org.isaas.vendorcatalog.api;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns errors into RFC 9457 problem details with a {@code code} property.
 *
 * Business rules enforced by the database (the publish gate, the join request
 * gate, unique emails and so on) are not re-checked in Java. Their errors are
 * passed to the caller as 409 with the database's own message and, when there
 * is one, the constraint name.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ApiException.class)
	ProblemDetail apiException(ApiException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
		problem.setProperty("code", ex.getCode());
		return problem;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail databaseRule(DataIntegrityViolationException ex) {
		if (ex.getMostSpecificCause() instanceof PSQLException psql && psql.getServerErrorMessage() != null) {
			ServerErrorMessage error = psql.getServerErrorMessage();
			ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage());
			problem.setProperty("code", codeFor(error.getSQLState()));
			if (error.getConstraint() != null) {
				problem.setProperty("constraint", error.getConstraint());
			}
			return problem;
		}
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The change breaks a data rule.");
		problem.setProperty("code", "DATA_INTEGRITY");
		return problem;
	}

	private static String codeFor(String sqlState) {
		return switch (sqlState) {
			case "23514" -> "RULE_VIOLATION";
			case "23505" -> "ALREADY_EXISTS";
			case "23503" -> "REFERENCE_VIOLATION";
			default -> "DATA_INTEGRITY";
		};
	}

}
