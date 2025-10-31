package ai.deeprunner.indexer.command.handler;

import ai.deeprunner.indexer.command.Command;

public interface CommandHandler {
    /**
     * Handle a command
     * 
     * @param command command to handle
     * @return result of command handling
     */
    Object handle(Command<?> command);
}

