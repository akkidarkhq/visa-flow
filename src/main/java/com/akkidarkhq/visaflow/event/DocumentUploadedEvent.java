package com.akkidarkhq.visaflow.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class DocumentUploadedEvent extends ApplicationEvent {
    private final UUID applicationId;
    private final UUID documentId;
    private final Long requirementId;

    public DocumentUploadedEvent(Object source, UUID applicationId, UUID documentId, Long requirementId) {
        super(source);
        this.applicationId = applicationId;
        this.documentId = documentId;
        this.requirementId = requirementId;
    }
}
