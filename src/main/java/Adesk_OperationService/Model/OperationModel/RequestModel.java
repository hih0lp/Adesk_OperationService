package Adesk_OperationService.Model.OperationModel;


import Adesk_OperationService.Constants.RequestStatuses;
import jakarta.persistence.*;
import lombok.Data;

import java.time.ZonedDateTime;


@Entity
@Data
public class RequestModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description")
    private String description;

    @Column(name = "type_of_operation")
    private String typeOfOperation; //income - outcome - transfer - ФРОНТ САМ ВЫСТАВИТ, ЧТО НАДО И ПУСТЬ САМ ФИЛЬТРУЕТ ПО ЗНАКУ

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "name_of_counterparty")
    private String nameOfCounterparty;

    @Column(name = "sum")
    private Long sum;

    @Column(name = "name")
    private String name;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;
//    @Column(name = "category_name")
//    private String categoryName;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "creator_login")
    private String creatorLogin; //тот, кто создал заявки

    @Column(name = "creator_email")
    private String creatorEmail;

    @Column(name = "responsible_manager")
    private String responsibleManager; // ответственный менеджер (тот, кто сделал контрагента)

    @Column(name = "approvedStatus")
    private RequestStatuses approvedStatus;

    /// TODO : СДЕЛАТЬ ФАЙЛЫ
}
