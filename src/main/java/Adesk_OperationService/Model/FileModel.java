package Adesk_OperationService.Model;

import Adesk_OperationService.Model.OperationModel.RequestModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "stored_filename", unique = true, length = 255)
    private String storedFilename;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

//    @Lob
    @JsonIgnore
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "content", columnDefinition = "bytea")
    private byte[] content;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private RequestModel request;

    @Column(name = "is_compressed")
    private Boolean isCompressed = false;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "href")
    private String href;

    @PostPersist
    protected void generateHref(){
        if(this.href == null && this.id != null){
            this.href = "https://gateway.marinafin.ru/api/gateway/requests/download-file/" + this.id; //создаю ссылку уже после закачивания в БД
        }
    }

    @PrePersist
    protected void onCreate() {
        if (storedFilename == null) {
            storedFilename = generateStoredFilename();
        }
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }

    private String generateStoredFilename() {
        String extension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalFilename.substring(lastDot);
        }
        return UUID.randomUUID().toString() + extension;
    }

    public String getFileSizeFormatted() {
        if (fileSize < 1024) return fileSize + " B";
        int exp = (int) (Math.log(fileSize) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", fileSize / Math.pow(1024, exp), pre);
    }
}