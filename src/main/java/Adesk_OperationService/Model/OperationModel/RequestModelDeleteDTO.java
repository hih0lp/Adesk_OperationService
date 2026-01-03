package Adesk_OperationService.Model.OperationModel;

import Adesk_OperationService.Constants.RequestStatuses;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RequestModelDeleteDTO {

    @JsonProperty("Id")
    public Long id;


}