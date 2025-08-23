package com.promo.quoter.exception.advice;

import org.springframework.context.MessageSource;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Locale;

public abstract class MessageSourceAdviceCtrl extends ResponseEntityExceptionHandler {
    private static final Locale LOCALE = Locale.ENGLISH;
    private final MessageSource messageSource;

    protected MessageSourceAdviceCtrl(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    String extractMessageSource(final String sourceCode) {
        return messageSource.getMessage(sourceCode, null, LOCALE);
    }

}