package Model.OperationModel;


import Constants.OperationStatuses;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Data
@Table(name = "ADESK_OPERATIONS")
@NoArgsConstructor
@Getter
@Setter
public class OperationModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(name = "description")
    private String description;

    @Column(name = "type_of_operation")
    private String typeOfOperation; //income - outcome - transfer

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "name_of_counterparty")
    private String nameOfCounterparty;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "responsible_login")
    private String responsibleLogin;

    @Column(name = "responsible_email")
    private String responsibleEmail;

    @Column(name = "approvedStatus")
    private OperationStatuses approvedStatus;
}
