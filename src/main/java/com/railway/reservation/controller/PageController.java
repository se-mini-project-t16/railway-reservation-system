package com.railway.reservation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the operations console's index for /admin and /admin/. */
@Controller
public class PageController {

    @GetMapping({"/admin", "/admin/"})
    public String adminHome() {
        return "forward:/admin/index.html";
    }
}
