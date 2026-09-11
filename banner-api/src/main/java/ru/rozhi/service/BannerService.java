package ru.rozhi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.rozhi.controller.dto.BannerResponse;
import ru.rozhi.repository.BannerRepository;
import ru.rozhi.repository.model.Banner;

@Service
public class BannerService {

    @Autowired
    private BannerRepository repository;

    public BannerResponse getBanner() {
        Banner banner = repository.findAll().getFirst();

        return new BannerResponse(
               banner.getName(),
               banner.getDescription()
        );
    }
}
