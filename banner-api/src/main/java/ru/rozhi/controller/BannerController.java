package ru.rozhi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rozhi.controller.dto.BannerResponse;
import ru.rozhi.service.BannerService;

@RestController
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping
    public BannerResponse getBanner() {
        return bannerService.getBanner();
    }
}
