package ai.deeprunner.indexer.command.publisher;

import ai.deeprunner.indexer.command.CreateDocumentCommand;
import ai.deeprunner.indexer.command.DeleteDocumentCommand;
import ai.deeprunner.indexer.command.UpdateDocumentCommand;
import ai.deeprunner.indexer.rabbit.CommandProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Publisher for document commands using Spring Cloud Stream
 * This abstraction allows easy switching between RabbitMQ and Kafka
 * Compatible with Java 21 and Spring Cloud Stream 4.x functional model
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommandPublisher {
    
    private final CommandProducer documentSource;
    
    /**
     * Publish create command using Cloud Stream
     */
    public void publishCreateCommand(CreateDocumentCommand command) {
        try {
            documentSource.sendCreateMessage(command);
            log.info("Published create command via Cloud Stream for document: {}", 
                command.getDocumentId());
        } catch (Exception e) {
            log.error("Error publishing create command via Cloud Stream", e);
            throw new RuntimeException("Failed to publish create command", e);
        }
    }
    
    /**
     * Publish update command using Cloud Stream
     */
    public void publishUpdateCommand(UpdateDocumentCommand command) {
        try {
            documentSource.sendUpdateMessage(command);
            log.info("Published update command via Cloud Stream for document: {}", 
                command.getDocumentId());
        } catch (Exception e) {
            log.error("Error publishing update command via Cloud Stream", e);
            throw new RuntimeException("Failed to publish update command", e);
        }
    }
    
    /**
     * Publish delete command using Cloud Stream
     */
    public void publishDeleteCommand(DeleteDocumentCommand command) {
        try {
            documentSource.sendDeleteMessage(command);
            log.info("Published delete command via Cloud Stream for document: {}", 
                command.getDocumentId());
        } catch (Exception e) {
            log.error("Error publishing delete command via Cloud Stream", e);
            throw new RuntimeException("Failed to publish delete command", e);
        }
    }
}


