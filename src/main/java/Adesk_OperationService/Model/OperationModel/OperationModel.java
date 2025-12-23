package Adesk_OperationService.Model.OperationModel;


import Adesk_OperationService.Constants.OperationStatuses;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;


@Entity
@Data
public class OperationModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description")
    private String description;

    @Column(name = "type_of_operation")
    private String typeOfOperation; //income - outcome - transfer

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "name_of_counterparty")
    private String nameOfCounterparty;

    @Column(name = "sum")
    private Long sum;

    /// TODO : СДЕЛАТЬ ОТВЕТСТВЕННОГО МЕНЕДЖЕРА - ЭТО ТОТ, КТО СОЗДАЛ КОНТРАГЕНТА (ПОИСК ПО ИМЕНИ)

    @Column(name = "created_at")
    private ZonedDateTime createdAt;
//    @Column(name = "category_name")
//    private String categoryName;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "responsible_login")
    private String responsibleLogin;

    @Column(name = "responsible_email")
    private String responsibleEmail;

    @Column(name = "approvedStatus")
    private OperationStatuses approvedStatus;

    /// TODO : СДЕЛАТЬ ФАЙЛЫ
}
