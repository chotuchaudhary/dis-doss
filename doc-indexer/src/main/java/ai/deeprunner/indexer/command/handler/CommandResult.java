package ai.deeprunner.indexer.command.handler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result returned after executing a command
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandResult {
    private boolean success;
    private String message;
}

