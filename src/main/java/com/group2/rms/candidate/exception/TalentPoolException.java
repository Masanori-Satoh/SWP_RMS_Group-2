package com.group2.rms.candidate.exception;
import com.group2.rms.core.exception.BaseBusinessException;
public class TalentPoolException extends BaseBusinessException {
    public TalentPoolException(String message) { super(message, "TALENT_POOL_INVALID"); }
}

