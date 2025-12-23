package Adesk_OperationService.Controllers;

import Adesk_OperationService.Constants.OperationStatuses;
import Adesk_OperationService.Repository.OperationRepository;
import Adesk_OperationService.Model.OperationModel.OperationModel;
import Adesk_OperationService.Model.OperationModel.OperationModelDTO;
import Adesk_OperationService.Model.OperationModel.OperationModelDeleteDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/operations")
@RequiredArgsConstructor
public class OperationController {
    private final Logger log = LoggerFactory.getLogger(OperationController.class);
    private final OperationRepository _operationRepository;

    @PostMapping("/create-operation")
    public ResponseEntity<?> createOperationAsync(@RequestBody OperationModelDTO dto, HttpServletRequest request){
        try {
            if(!dto.isValid())
                return ResponseEntity.badRequest().body("dto is invalid");
            if(request.getHeader("X-Authenticated") == null || request.getHeader("X-Authenticated").isEmpty())
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("request has been came not from gateway");

            if(!Arrays.stream(request.getHeader("X-User-Permissions").split(",")).anyMatch(s -> s.equals("CREATE_REQUEST_AND_DELETE_BEFORE_APPROVE")))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("no rights");

            var newOperation = new OperationModel();
            newOperation.setTypeOfOperation(dto.getTypeOfOperation());
            newOperation.setDescription(dto.getDescription());
//            newOperation.setCategoryName(dto.getCategoryName());
            newOperation.setProjectName(dto.getProjectName());
            newOperation.setCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            newOperation.setNameOfCounterparty(dto.getNameOfCounterparty());
            newOperation.setResponsibleEmail(request.getHeader("X-User-Email"));
            newOperation.setResponsibleLogin(dto.getResponsibleLogin());
            newOperation.setCreatedAt(ZonedDateTime.now());
            newOperation.setSum(dto.getSum());
            newOperation.setApprovedStatus(OperationStatuses.APPROVING);

            _operationRepository.save(newOperation);

            return ResponseEntity.ok().body("successfully creating");
        }
        catch (Exception ex){
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

    @DeleteMapping("/delete-operations")
    @Transactional
    public ResponseEntity<?> deleteOperationsAsync(@RequestBody List<OperationModelDeleteDTO> dtos, HttpServletRequest request){
        try{
            for (var dto : dtos)
                if(!dto.isValid())
                    return ResponseEntity.badRequest().body("dto is invalid");

            if(request.getHeader("X-Authenticated") == null || request.getHeader("X-Authenticated").isEmpty())
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("request has been came not from gateway");

            if(Arrays.stream(request.getHeader("X-User-Permissions") //если может удалять только проекты до аппрува
                    .split(",")).anyMatch(s -> s.equals("CREATE_REQUEST_AND_DELETE_BEFORE_APPROVE") || s.equals("REQUEST_WORK"))){ //для работы с запросами
                if(dtos.stream().anyMatch(x -> x.approvedStatus != OperationStatuses.APPROVING))
                    return ResponseEntity.badRequest().body("you can delete only project with approving status");
                if(!Arrays.stream(request.getHeader("X-User-Permissions").split(",")).anyMatch(s -> s.equals("REQUEST_WORK")))
                    if (dtos.stream().anyMatch(s -> s.responsibleEmail != request.getHeader("X-User-Email")))
                        return ResponseEntity.badRequest().body("you can delete only yours project");

                List<Long> ids = dtos.stream()
                                .map(dto -> dto.getId())
                                        .collect(Collectors.toList());

                _operationRepository.deleteAllById(ids);

                return ResponseEntity.ok().body("deleting successfully");
            }
            else if(Arrays.stream(request.getHeader("X-User-Permissions")
                    .split(",")).anyMatch(s -> s.equals("APPROVE_REQUEST_AND_DELETE_AFTER_APPROVE"))){

                if (dtos.stream().anyMatch(s -> s.approvedStatus == OperationStatuses.APPROVED || s.approvedStatus == OperationStatuses.DISAPPROVED))
                    return ResponseEntity.badRequest().body("you can only delete projects which is not approved");

                if(!Arrays.stream(request.getHeader("X-User-Permissions").split(",")).anyMatch(s -> s.equals("REQUEST_WORK")))
                    if(dtos.stream().anyMatch(s -> s.responsibleEmail != request.getHeader("X-User-Email")))
                        return ResponseEntity.badRequest().body("you can only delete your projects");

                List<Long> ids = dtos.stream().map(dto -> dto.getId()).collect(Collectors.toList());

                _operationRepository.deleteAllById(ids);

                return ResponseEntity.ok().body("deleting successfully");
            }
            else return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("no rights");
        } catch(Exception ex){
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }
}


///TODO : ПОИСК НУЖНО СДЕЛАТЬ ПО ТРИГРАММАМ