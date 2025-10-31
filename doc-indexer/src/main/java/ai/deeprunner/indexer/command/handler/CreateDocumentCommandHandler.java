package ai.deeprunner.indexer.command.handler;

import ai.deeprunner.indexer.command.CreateDocumentCommand;
import ai.deeprunner.indexer.command.publisher.CommandPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateDocumentCommandHandler implements CommandHandler {
    
    private final CommandPublisher commandPublisher;
    
    @Override
    public Object handle(ai.deeprunner.indexer.command.Command<?> command) {
        if (command instanceof CreateDocumentCommand) {
            CreateDocumentCommand cmd = (CreateDocumentCommand) command;
            
            // Publish to RabbitMQ for async processing
            commandPublisher.publishCreateCommand(cmd);
            
            log.info("Published create command for document: {} for tenant: {}", 
                cmd.getDocumentId(), cmd.getTenantId());
            
            // Return success response
            return new CommandResult(true, "Document creation command published successfully");
        }
        
        throw new IllegalArgumentException("Invalid command type for this handler");
    }
}

