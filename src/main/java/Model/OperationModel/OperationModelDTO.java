package Model.OperationModel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OperationModelDTO {

    @JsonProperty("Description")
    public String description;

    @JsonProperty("TypeOfOperation")
    public String typeOfOperation; //income - outcome - transfer

    @JsonProperty("ProjectName")
    public String projectName;

    @JsonProperty("NameOfCounterparty")
    public String nameOfCounterparty;

    @JsonProperty("CategoryName")
    public String categoryName;

//    @JsonProperty("CompanyName")
//    public String companyName;

    @JsonProperty("ResponsibleLogin")
    public String responsibleLogin;

    @JsonProperty("ResponsibleEmail")
    public String responsibleEmail;

    @JsonIgnore
    public boolean isValid(){
        return description != null && !description.trim().isEmpty() &&
                typeOfOperation != null && !typeOfOperation.trim().isEmpty() &&
                projectName != null && !projectName.trim().isEmpty() &&
                nameOfCounterparty != null && !nameOfCounterparty.trim().isEmpty() &&
                companyName != null && !companyName.trim().isEmpty() &&
                responsibleLogin != null && responsibleLogin.trim().isEmpty() &&
                responsibleEmail != null && responsibleEmail.trim().isEmpty();
    }
}
