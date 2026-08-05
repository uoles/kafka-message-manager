package ru.uoles.kafka.sender.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Контроллер HTML-страниц веб-интерфейса отправки сообщений.
 */
@Controller
@RequestMapping("/web")
public class WebController {

    /**
     * Открывает страницу входа.
     *
     * @return имя Thymeleaf-шаблона страницы входа
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Открывает страницу регистрации.
     *
     * @return имя Thymeleaf-шаблона страницы регистрации
     */
    @GetMapping("/register")
    public String register() {
        return "register";
    }

    /**
     * Открывает форму отправки сообщения.
     *
     * @return имя Thymeleaf-шаблона страницы
     */
    @GetMapping("/send-message")
    public String showForm() {
        return "index";
    }

    /**
     * Открывает главную страницу веб-интерфейса.
     *
     * @return имя Thymeleaf-шаблона страницы
     */
    @GetMapping({"/", "/index"})
    public String home() {
        return "index";
    }
}
