package Adesk_OperationService.Repository;

import Adesk_OperationService.Model.FileModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileModel, Long> {
}
