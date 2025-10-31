package ai.deeprunner.indexer.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Command for deleting a document
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteDocumentCommand implements Command<Void> {
    private String tenantId;
    private String documentId;
    
    @Override
    public Void execute() {
        // Command execution handled by the handler
        return null;
    }
    
    @Override
    public CommandType getType() {
        return CommandType.DELETE_DOCUMENT;
    }
}

