package md.dpscs.cch.iis.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api") // This prefixes all methods in this class with /api
public class HelloController {

    @GetMapping("/hello") // This completes the path: /api/hello
    public String sayHello() {
        // This is the text that will appear in your React App
        return "Hello from Ident-Index Backend (Port 8081)!";
    }
}