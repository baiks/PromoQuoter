package com.promo.quoter.services;

import com.promo.quoter.dtos.CartConfirmResponse;
import com.promo.quoter.dtos.CartQuoteRequest;
import com.promo.quoter.dtos.CartQuoteResponse;

public interface CartService {
    CartQuoteResponse calculateQuote(CartQuoteRequest request);
    CartConfirmResponse confirmCart(CartQuoteRequest request, String idempotencyKey);
}