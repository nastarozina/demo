package ru.rozhi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.rozhi.controller.dto.BannerRequest;
import ru.rozhi.controller.dto.BannerResponse;
import ru.rozhi.service.BannerService;

import java.net.URI;
import java.util.List;

@RestController
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping("/{id}")
    public ResponseEntity<BannerResponse> getBannerById(@PathVariable String id) {
        BannerResponse banner = bannerService.getBannerById(id);
        if (banner == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(banner);
    }

    @GetMapping
    public List<BannerResponse> getAllBanners() {
        return bannerService.getAllBanners();
    }

    @PostMapping
    public ResponseEntity<BannerResponse> createBanner(@RequestBody BannerRequest banner) {
        BannerResponse createdBanner = bannerService.createBanner(banner);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdBanner.id())
                .toUri();

        return ResponseEntity.created(location).body(createdBanner);
    }
}
