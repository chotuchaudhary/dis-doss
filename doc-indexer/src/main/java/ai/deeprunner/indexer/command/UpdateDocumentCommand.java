package ai.deeprunner.indexer.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Command for updating a document
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDocumentCommand implements Command<Void> {
    private String tenantId;
    private String documentId;
    private String title;
    private String content;
    private String category;
    private Map<String, String> metadata;
    
    @Override
    public Void execute() {
        // Command execution is handled by the handler
        return null;
    }
    
    @Override
    public CommandType getType() {
        return CommandType.UPDATE_DOCUMENT;
    }
}

