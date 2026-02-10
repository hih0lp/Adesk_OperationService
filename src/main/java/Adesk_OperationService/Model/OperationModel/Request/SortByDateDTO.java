package Adesk_OperationService.Model.OperationModel.Request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SortByDateDTO {
    @JsonProperty("Date1")
    public String date1; //начало

    @JsonProperty("Date2")
    public String date2; //конец

    @JsonIgnore
    public boolean isValid(){
        return date1 != null && !date1.trim().isEmpty() &&
                date2 != null && !date2.trim().isEmpty();
    }
}
