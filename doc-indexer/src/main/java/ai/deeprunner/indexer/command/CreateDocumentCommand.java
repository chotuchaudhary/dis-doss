package ai.deeprunner.indexer.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Command for creating a document
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDocumentCommand implements Command<Void> {
    private String tenantId;
    private String documentId;
    private Map<String, Object> document;
    private String documentType;

    @Override
    public Void execute() {
        // Command execution is handled by the handler
        return null;
    }
    
    @Override
    public CommandType getType() {
        return CommandType.CREATE_DOCUMENT;
    }
}

