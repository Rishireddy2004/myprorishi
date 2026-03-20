package com.ridesharing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards all non-API, non-static requests to index.html so React Router works.
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
        "/", "/login", "/register", "/forgot-password", "/reset-password",
        "/driver/**", "/passenger/**", "/admin/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
