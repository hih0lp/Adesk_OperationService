package Controllers;

import Interfaces.OperationRepository;
import Model.OperationModel.OperationModel;
import Model.OperationModel.OperationModelDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/operation")
@RequiredArgsConstructor
public class OperationController {
    private final Logger log = LoggerFactory.getLogger(OperationController.class);
    private final OperationRepository _operationService;

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
            newOperation.setTypeOfOperation(dto.typeOfOperation);
            newOperation.setDescription(dto.getDescription());
            newOperation.setCategoryName(dto.getCategoryName());
            newOperation.setProjectName(dto.getProjectName());
            newOperation.setNameOfCounterparty(dto.getNameOfCounterparty());
            newOperation.setResponsibleEmail(request.getHeader("X-User-Email"));
            newOperation.setResponsibleLogin(dto.responsibleLogin);
            /// TODO

            
        }
        catch (Exception ex){
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }
}
