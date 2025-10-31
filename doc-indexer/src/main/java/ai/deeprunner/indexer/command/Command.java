package ai.deeprunner.indexer.command;

/**
 * Base interface for all commands in the command pattern
 * 
 * @param <T> return type of command execution
 */
public interface Command<T> {
    /**
     * Execute the command
     * 
     * @return result of command execution
     */
    T execute();
    
    /**
     * Get command type
     * 
     * @return command type
     */
    CommandType getType();
}
