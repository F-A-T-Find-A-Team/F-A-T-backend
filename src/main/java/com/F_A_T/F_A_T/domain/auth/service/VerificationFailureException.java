package com.F_A_T.F_A_T.domain.auth.service;
import com.F_A_T.F_A_T.global.exception.*;
// Failed guesses must commit their attempt count even though the HTTP request fails.
public class VerificationFailureException extends ApiException {
    public VerificationFailureException(ErrorCode code) { super(code); }
}
