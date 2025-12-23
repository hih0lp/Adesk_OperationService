package Adesk_OperationService.Repository;

import Adesk_OperationService.Model.OperationModel.OperationModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperationRepository extends JpaRepository<OperationModel, Long> {
    Optional<OperationModel> findByProjectName(String companyName);
}
