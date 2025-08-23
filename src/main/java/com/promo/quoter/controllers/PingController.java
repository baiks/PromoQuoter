package com.promo.quoter.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PingController {

    @GetMapping(value = "ping")
    public String ping() {
        return "ping";
    }
}
