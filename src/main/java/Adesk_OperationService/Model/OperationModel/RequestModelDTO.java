package Adesk_OperationService.Model.OperationModel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class RequestModelDTO {

    @JsonProperty("Description")
    public String description;

    @JsonProperty("TypeOfOperation")
    public String typeOfOperation; //income - outcome - transfer

    @JsonProperty("ProjectName")
    public String projectName;

    @JsonProperty("NameOfCounterparty")
    public String nameOfCounterparty;

    @JsonProperty("Sum")
    public Long sum;

    @JsonProperty("Name")
    public String name;
//    @JsonProperty("CategoryName")
//    public String categoryName;

//    @JsonProperty("CompanyName")
//    public String companyName;

    @JsonProperty("ResponsibleLogin")
    public String responsibleLogin;

    @JsonIgnore
    public List<MultipartFile> files;
//    @JsonProperty("ResponsibleEmail")
//    public String responsibleEmail;

    @JsonIgnore
    public boolean isValid(){
        return description != null && !description.trim().isEmpty() &&
                typeOfOperation != null && !typeOfOperation.trim().isEmpty() &&
                projectName != null && !projectName.trim().isEmpty() &&
                nameOfCounterparty != null && !nameOfCounterparty.trim().isEmpty() &&
//                companyName != null && !companyName.trim().isEmpty() &&
//                responsibleLogin != null && !responsibleLogin.trim().isEmpty() &&
//                responsibleEmail != null && !responsibleEmail.trim().isEmpty() &&
                sum != 0;
//                categoryName != null && categoryName.trim().isEmpty();
    }
}
