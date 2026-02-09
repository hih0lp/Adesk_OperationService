package Adesk_OperationService.Model.OperationModel;

import Adesk_OperationService.Constants.RequestStatuses;
import Adesk_OperationService.Model.FileModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "requests")
public class RequestModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description")
    private String description;

    @Column(name = "type_of_operation")
    private String typeOfOperation; //income - outcome - transfer

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "name_of_counterparty")
    private String nameOfCounterparty;

    @Column(name = "sum")
    private Long sum;

//    @Column(name = "name")
//    private String name;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "creator_login")
    private String creatorLogin;

    @Column(name = "creator_email")
    private String creatorEmail;

    @Column(name = "responsible_manager")
    private String responsibleManager;

    @Column(name = "approved_status")
    @Enumerated(EnumType.STRING)
    private RequestStatuses approvedStatus;

    @JsonIgnore
    @OneToMany(
            mappedBy = "request",
            cascade = CascadeType.ALL, // При удалении request удаляются все связанные файлы
            orphanRemoval = true,      // При отсоединении файла от request - удаляем файл
            fetch = FetchType.LAZY     // Ленивая загрузка файлов
    )
    private List<FileModel> files = new ArrayList<>();

    // метод для добавления файла
    public void addFile(FileModel file) {
        files.add(file);
        file.setRequest(this);
    }

    // метод для удаления файла
    public void removeFile(FileModel file) {
        files.remove(file);
        file.setRequest(null);
    }

    // Получение количества файлов
    public int getFilesCount() {
        return files.size();
    }

    // Получение общего размера всех файлов
    public long getTotalFilesSize() {
        return files.stream()
                .mapToLong(FileModel::getFileSize)
                .sum();
    }
}