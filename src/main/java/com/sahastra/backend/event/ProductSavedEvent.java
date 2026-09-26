package com.sahastra.backend.event;

import com.sahastra.backend.domain.entity.Product;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ProductSavedEvent extends ApplicationEvent {
    private final Product product;

    public ProductSavedEvent(Object source, Product product) {
        super(source);
        this.product = product;
    }
}
