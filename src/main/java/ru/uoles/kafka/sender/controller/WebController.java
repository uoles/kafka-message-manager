package ru.uoles.kafka.sender.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/web")
public class WebController {

    @GetMapping("/send-message")
    public String showForm() {
        return "index";
    }

    // Если нужно открывать без /web
    @GetMapping({"/", "/index"})
    public String home() {
        return "index";
    }
}

