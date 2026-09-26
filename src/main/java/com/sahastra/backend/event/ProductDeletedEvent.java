package com.sahastra.backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ProductDeletedEvent extends ApplicationEvent {
    private final String productId;

    public ProductDeletedEvent(Object source, String productId) {
        super(source);
        this.productId = productId;
    }
}
