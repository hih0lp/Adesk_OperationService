package Adesk_OperationService.Model.OperationModel;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class RequestFormDTO {
    private String description;
    private String typeOfOperation;
    private String projectId;
    private String nameOfCounterparty;
    private Long sum;
    private String name;
    private String responsibleLogin;
    private List<MultipartFile> files;

    public boolean isValid() {
        return description != null && !description.trim().isEmpty() &&
                typeOfOperation != null && !typeOfOperation.trim().isEmpty() &&
                projectId != null && !projectId.trim().isEmpty() &&
                nameOfCounterparty != null && !nameOfCounterparty.trim().isEmpty() &&
                sum != null && sum != 0;
    }
}