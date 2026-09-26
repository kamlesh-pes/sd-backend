package com.sahastra.backend.event;

import com.sahastra.backend.domain.entity.Product;
import com.sahastra.backend.service.ProductIndexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener for product lifecycle events.
 * Triggers asynchronous indexing of product changes to OpenSearch.
 */
@Component
@Slf4j
public class ProductEventListener {

    private final ProductIndexService productIndexService;

    public ProductEventListener(ProductIndexService productIndexService) {
        this.productIndexService = productIndexService;
    }

    /**
     * Index product after successful save transaction.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductSave(ProductSavedEvent event) {
        log.info("Product saved event received for: {}", event.getProduct().getId());
        try {
            productIndexService.indexProduct(event.getProduct());
        } catch (Exception e) {
            log.error("Failed to index product after save: {}", event.getProduct().getId(), e);
            // Do not fail the transaction; log and continue
        }
    }

    /**
     * Remove product from index after successful delete transaction.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductDelete(ProductDeletedEvent event) {
        log.info("Product deleted event received for: {}", event.getProductId());
        try {
            productIndexService.deleteProduct(event.getProductId());
        } catch (Exception e) {
            log.error("Failed to delete product from index: {}", event.getProductId(), e);
            // Do not fail the transaction; log and continue
        }
    }
}
