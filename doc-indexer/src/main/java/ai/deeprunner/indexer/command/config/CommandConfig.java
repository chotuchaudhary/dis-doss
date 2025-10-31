package ai.deeprunner.indexer.command.config;

import ai.deeprunner.indexer.command.CommandType;
import ai.deeprunner.indexer.command.handler.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration class that registers command handlers
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CommandConfig {
    
    private final CommandInvoker commandInvoker;
    private final CreateDocumentCommandHandler createHandler;
    private final UpdateDocumentCommandHandler updateHandler;
    private final DeleteDocumentCommandHandler deleteHandler;
    
    /**
     * Register all command handlers after bean creation
     */
    @PostConstruct
    public void registerHandlers() {
        commandInvoker.registerHandler(CommandType.CREATE_DOCUMENT, createHandler);
        commandInvoker.registerHandler(CommandType.UPDATE_DOCUMENT, updateHandler);
        commandInvoker.registerHandler(CommandType.DELETE_DOCUMENT, deleteHandler);
        
        log.info("Successfully registered command handlers");
    }
}
