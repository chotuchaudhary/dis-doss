package ai.deeprunner.indexer.command.handler;

import ai.deeprunner.indexer.command.Command;
import ai.deeprunner.indexer.command.CommandType;
import ai.deeprunner.indexer.command.CreateDocumentCommand;
import ai.deeprunner.indexer.command.DeleteDocumentCommand;
import ai.deeprunner.indexer.command.UpdateDocumentCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * CommandInvoker executes commands using registered handlers
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommandInvoker {
    
    private final Map<CommandType, CommandHandler> handlers = new HashMap<>();
    
    /**
     * Register a handler for a command type
     */
    public void registerHandler(CommandType type, CommandHandler handler) {
        handlers.put(type, handler);
        log.debug("Registered handler for command type: {}", type);
    }
    
    /**
     * Execute a command using registered handlers
     * 
     * @param command command to execute
     * @return result of command execution
     */
    @SuppressWarnings("unchecked")
    public <T> T execute(Command<T> command) {
        CommandType type = command.getType();
        CommandHandler handler = handlers.get(type);
        
        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for command type: " + type);
        }
        
        log.info("Executing command: {} for tenant: {}", type, getTenantId(command));
        Object result = handler.handle(command);
        return (T) result;
    }
    
    /**
     * Execute a command and return CommandResult directly
     */
    public CommandResult executeCommand(Command<?> command) {
        CommandType type = command.getType();
        CommandHandler handler = handlers.get(type);
        
        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for command type: " + type);
        }
        
        log.info("Executing command: {} for tenant: {}", type, getTenantId(command));
        Object result = handler.handle(command);
        if (result instanceof CommandResult) {
            return (CommandResult) result;
        }
        throw new IllegalStateException("Handler returned unexpected result type: " + result.getClass());
    }
    
    private String getTenantId(Command<?> command) {
        if (command instanceof CreateDocumentCommand) {
            return ((CreateDocumentCommand) command).getTenantId();
        } else if (command instanceof UpdateDocumentCommand) {
            return ((UpdateDocumentCommand) command).getTenantId();
        } else if (command instanceof DeleteDocumentCommand) {
            return ((DeleteDocumentCommand) command).getTenantId();
        }
        return "unknown";
    }
}