package Adesk_OperationService.Repository;

import Adesk_OperationService.Model.OperationModel.RequestModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<RequestModel, Long> {
//    Optional<RequestModel> findByCompanyName(String companyName);
    Optional<RequestModel> findByNameAndProjectNameAndCompanyId(String name, String projectName, Long companyName);
//    List<RequestModel> findByProjectName(String projectName);
    List<RequestModel> findByProjectNameAndCompanyId(String projectName, Long companyId);
}
