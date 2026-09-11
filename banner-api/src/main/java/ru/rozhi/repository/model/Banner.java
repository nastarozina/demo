package ru.rozhi.repository.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "banners")
@Data
@Builder
@AllArgsConstructor
public class Banner{

    @Id
    private String id;

    private String name;
    private String description;
}
