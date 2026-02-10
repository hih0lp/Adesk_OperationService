package Adesk_OperationService.Model.OperationModel.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RequestModelDeleteDTO {

    @JsonProperty("Id")
    public Long id;


}