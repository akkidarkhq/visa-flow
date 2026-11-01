package com.akkidarkhq.visaflow.event;

import com.akkidarkhq.visaflow.model.enums.DocumentStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class DocumentReviewedEvent extends ApplicationEvent {
    private final UUID applicationId;
    private final UUID documentId;
    private final DocumentStatus status;

    public DocumentReviewedEvent(Object source, UUID applicationId, UUID documentId, DocumentStatus status) {
        super(source);
        this.applicationId = applicationId;
        this.documentId = documentId;
        this.status = status;
    }
}
