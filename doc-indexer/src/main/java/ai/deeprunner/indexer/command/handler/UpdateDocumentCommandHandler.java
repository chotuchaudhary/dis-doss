package ai.deeprunner.indexer.command.handler;

import ai.deeprunner.indexer.command.UpdateDocumentCommand;
import ai.deeprunner.indexer.command.publisher.CommandPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateDocumentCommandHandler implements CommandHandler {
    
    private final CommandPublisher commandPublisher;
    
    @Override
    public Object handle(ai.deeprunner.indexer.command.Command<?> command) {
        if (command instanceof UpdateDocumentCommand) {
            UpdateDocumentCommand cmd = (UpdateDocumentCommand) command;
            
            // Publish to RabbitMQ for async processing
            commandPublisher.publishUpdateCommand(cmd);
            
            log.info("Published update command for document: {} for tenant: {}", 
                cmd.getDocumentId(), cmd.getTenantId());
            
            // Return success response
            return new CommandResult(true, "Document update command published successfully");
        }
        
        throw new IllegalArgumentException("Invalid command type for this handler");
    }
}

