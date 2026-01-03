package Adesk_OperationService.Controllers;

import Adesk_OperationService.Constants.RequestStatuses;
import Adesk_OperationService.Model.OperationModel.SortByDateDTO;
import Adesk_OperationService.Repository.RequestRepository;
import Adesk_OperationService.Model.OperationModel.RequestModel;
import Adesk_OperationService.Model.OperationModel.RequestModelDTO;
import Adesk_OperationService.Model.OperationModel.RequestModelDeleteDTO;
import Adesk_OperationService.Services.TimeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.lang.model.element.VariableElement;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class RequestController {
    private final Logger log = LoggerFactory.getLogger(RequestController.class);
    private final RequestRepository _requestRepository;
    private final TimeService _timeService;



    @PostMapping("/create-request")
    public ResponseEntity<?> createOperationAsync(@RequestBody RequestModelDTO dto, HttpServletRequest request){
        try {
            if(!dto.isValid())
                return ResponseEntity.badRequest().body("dto is invalid");
            if(request.getHeader("X-Authenticated") == null || request.getHeader("X-Authenticated").isEmpty())
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("request has been came not from gateway");

            if(!Arrays.stream(request.getHeader("X-User-Permissions").split(",")).anyMatch(s -> s.equals("CREATE_REQUEST_AND_DELETE_BEFORE_APPROVE")))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("no rights");

            var newRequest = new RequestModel();
            newRequest.setTypeOfOperation(dto.getTypeOfOperation());
            newRequest.setDescription(dto.getDescription());
//            newOperation.setCategoryName(dto.getCategoryName());
            newRequest.setProjectName(dto.getProjectName());
            newRequest.setCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            newRequest.setNameOfCounterparty(dto.getNameOfCounterparty());
            newRequest.setCreatorEmail(request.getHeader("X-User-Email"));
            newRequest.setName(dto.getName());
            newRequest.setCreatorLogin(dto.getResponsibleLogin());
//            newRequest
            newRequest.setCreatedAt(ZonedDateTime.now());
            newRequest.setSum(dto.getSum());
            newRequest.setApprovedStatus(RequestStatuses.APPROVING);

            _requestRepository.save(newRequest);

            return ResponseEntity.ok().body("successfully creating");
        }
        catch (Exception ex){
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

    @DeleteMapping("/delete-requests")
    @Transactional
    public ResponseEntity<?> deleteRequestsAsync(@RequestBody List<RequestModelDeleteDTO> dtos, HttpServletRequest request){
        try{
            if(request.getHeader("X-Authenticated") == null || request.getHeader("X-Authenticated").isEmpty())
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("request has been came not from gateway");
            /// TODO : СДЕЛАТЬ ПОЛУЧЕНИЕ ПО АЙДИ БЛЯТЬ НАХУЙ
            var requests = _requestRepository.findAllById(dtos.stream().map(x -> x.getId()).collect(Collectors.toList()));


            if(Arrays.stream(request.getHeader("X-User-Permissions") //если может удалять только проекты до аппрува
                    .split(",")).anyMatch(s -> s.equals("CREATE_REQUEST_AND_DELETE_BEFORE_APPROVE") || s.equals("REQUEST_WORK"))){ //для работы с запросами
                if(requests.stream().anyMatch(x -> x.getApprovedStatus() != RequestStatuses.APPROVING))
                    return ResponseEntity.badRequest().body("you can delete only request with approving status");
                if(!Arrays.stream(request.getHeader("X-User-Permissions").split(",")).anyMatch(s -> s.equals("REQUEST_WORK")))
                    if (requests.stream().anyMatch(s -> !s.getCreatorEmail().equals(request.getHeader("X-User-Email"))))
                        return ResponseEntity.badRequest().body("you can delete only yours request");

                List<Long> ids = dtos.stream()
                                .map(dto -> dto.getId())
                                        .collect(Collectors.toList());

                _requestRepository.deleteAllById(ids);

                return ResponseEntity.ok().body("deleting successfully");
            }
            else if(Arrays.stream(request.getHeader("X-User-Permissions")
                    .split(",")).anyMatch(s -> s.equals("APPROVE_REQUEST_AND_DELETE_AFTER_APPROVE"))){

                if (requests.stream().anyMatch(s -> s.getApprovedStatus() == RequestStatuses.APPROVED || s.getApprovedStatus() == RequestStatuses.DISAPPROVED))
                    return ResponseEntity.badRequest().body("you can only delete projects which is not approved");

                if(!Arrays.stream(request.getHeader("X-User-Permissions").split(",")).anyMatch(s -> s.equals("REQUEST_WORK")))
                    if(requests.stream().anyMatch(s -> s.getResponsibleManager() != request.getHeader("X-User-Email")))
                        return ResponseEntity.badRequest().body("you can only delete your projects");

                List<Long> ids = dtos.stream().map(dto -> dto.getId()).collect(Collectors.toList());

                _requestRepository.deleteAllById(ids);

                return ResponseEntity.ok().body("deleting successfully");
            }
            else return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("no rights");
        } catch(Exception ex){
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

    @GetMapping("/get-requests")
    public ResponseEntity<?> getRequestsByProjectName(HttpServletRequest request){
        try{
            if(request.getHeader("X-Authenticated") == null || request.getHeader("X-Authenticated").isEmpty())
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("request has been came not from gateway");

            var requests = _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            if(requests.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            return ResponseEntity.ok().body(requests);
        } catch(Exception ex){
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

    @PostMapping("/approve-request/{requestId}")
    public ResponseEntity<?> approveRequest(@PathVariable Long requestId, HttpServletRequest request){
        try{
            if(requestId == null)
                return ResponseEntity.badRequest().body("id cannot be null");

            if(request.getHeader("X-Authenticated") == null || request.getHeader("X-Authenticated").isEmpty())
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("request has been came not from gateway");

            if(!Arrays.stream(request.getHeader("X-User-Permissions")
                    .split(",")).anyMatch(s -> s.equals("REQUEST_WORK") || s.equals("APPROVE_REQUEST_AND_DELETE_AFTER_APPROVE")))
                return ResponseEntity.badRequest().body("no rights");

            var requestOpt = _requestRepository.findById(requestId);
            if(requestOpt.isEmpty())
                return ResponseEntity.badRequest().body("request doesn't exist");

            var req = requestOpt.get();
            req.setApprovedStatus(RequestStatuses.APPROVED);
            _requestRepository.save(req);

            return ResponseEntity.ok().body("successfully approving");
        } catch(Exception ex) {
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

    @PostMapping("/disapprove-request/{requestId}")
    public ResponseEntity<?> disapproveRequest(@PathVariable Long requestId, HttpServletRequest request){
        try{
            if(requestId == null)
                return ResponseEntity.badRequest().body("id cannot be null");

            if(request.getHeader("X-Authenticated") == null || request.getHeader("X-Authenticated").isEmpty())
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("request has been came not from gateway");

            if(!Arrays.stream(request.getHeader("X-User-Permissions")
                    .split(",")).anyMatch(s -> s.equals("REQUEST_WORK") || s.equals("APPROVE_REQUEST_AND_DELETE_AFTER_APPROVE")))
                return ResponseEntity.badRequest().body("no rights");

            var requestOpt = _requestRepository.findById(requestId);
            if(requestOpt.isEmpty())
                return ResponseEntity.badRequest().body("request doesn't exist");

            var req = requestOpt.get();

            req.setApprovedStatus(RequestStatuses.DISAPPROVED);
            _requestRepository.save(req);

            return ResponseEntity.ok().body("successfully disapproved");
        } catch(Exception ex){
            log.error(ex.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/get-requests-order-by-date-today")
    public ResponseEntity<?> getRequestsOrderByDateToday(HttpServletRequest request){
        try {
            if(request.getHeader("X-Authenticated") == null || request.getHeader("X-Authenticated").isEmpty())
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("request has been came not from gateway");

            var requests = _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            if(requests.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            return ResponseEntity.ok().body(_timeService.filterByToday(requests));
        } catch (Exception ex){
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

    @GetMapping("/get-requests-order-by-date-week")
    public ResponseEntity<?> getRequestsOrderByDateWeek(HttpServletRequest request){
        try{
            if(request.getHeader("X-Authenticated") == null || request.getHeader("X-Authenticated").isEmpty())
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("request has been came not from gateway");

            var requests = _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            if(requests.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            return ResponseEntity.ok().body(_timeService.filterByCurrentWeek(requests));
        } catch(Exception ex) {
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

    @PostMapping("/get-requests-order-by-dates")
    public ResponseEntity<?> getRequestsOrderByDates(@RequestBody SortByDateDTO dto, HttpServletRequest request){
        try {
            if(!dto.isValid())
                return ResponseEntity.badRequest().body("dto is invalid");

            if(request.getHeader("X-Authenticated") == null || request.getHeader("X-Authenticated").isEmpty())
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("request has been came not from gateway");

            var requests = _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            if(requests.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            return ResponseEntity.ok().body(_timeService.filterByDateTimeRange(requests, dto.date1, dto.date2));
        } catch (Exception ex){
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }


    @GetMapping("/get-requests-order-by-date-quarter/{numberOfQuarter}")
    public ResponseEntity<?> getRequestsOrderByDateQuarter(@PathVariable int numberOfQuarter, HttpServletRequest request){
        try{
            if(request.getHeader("X-Authenticated") == null || request.getHeader("X-Authenticated").isEmpty())
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("request has been came not from gateway");

            var requests = _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            if(requests.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            return ResponseEntity.ok().body(_timeService.filterByQuarter(requests, numberOfQuarter));
        } catch(Exception ex) {
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

    @GetMapping("/get-requests-order-by-date-year")
    public ResponseEntity<?> getRequestsOrderByYear(HttpServletRequest request){
        try{
            if(request.getHeader("X-Authenticated") == null || request.getHeader("X-Authenticated").isEmpty())
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("request has been came not from gateway");

            var requests = _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            if(requests.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            return ResponseEntity.ok().body(_timeService.filterByCurrentYear(requests));
        } catch(Exception ex) {
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }



    @GetMapping("/get-company-requests") //получение всех запросов по компании
    public ResponseEntity<?> getCompanyRequests(HttpServletRequest request){
        try {
            if(request.getHeader("X-Authenticated") == null || request.getHeader("X-Authenticated").isEmpty())
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("request has been came not from gateway");

            var requests = _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            if(requests.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            return ResponseEntity.ok().body(requests);
        } catch (Exception ex){
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

    @GetMapping("/get-company-operations")
    public ResponseEntity<?> getCompanyOperations(HttpServletRequest request){
        try{
            if(request.getHeader("X-Authenticated") == null || request.getHeader("X-Authenticated").isEmpty())
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("request has been came not from gateway");

            var operations = _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            if(operations.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            return ResponseEntity.ok().body(operations.stream().filter(x -> x.getApprovedStatus() == RequestStatuses.APPROVED));
        } catch(Exception ex){
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }
}


///TODO : ПОИСК НУЖНО СДЕЛАТЬ ПО ТРИГРАММАМ
///TODO : ОПЕРАЦИИ ТОЛЬКО АПРУВНУТЫЕ МОГУТ БЫТЬ //есть
///TODO : ЗАЯВКИ ВСЕ МОГУТ БЫТЬ (ЛЮБОЙ СТАТУС МОЖЕТ БЫТЬ) //есть
///TODO : ПОИСК ЗАЯВОК НЕ ДОЛЖЕН БЫ ПО ПРОЕКТУ (ДОЛЖЕН БЫТЬ ПРОСТО ЗАПРОС НА ВСЕ МАТЬ ТВОЮ ЗАПРОСЫ БЛЯ) //есть
/// TODO : СДЕЛАТЬ ВАЛИДАЦИЮ НА СТАТУС ЗАЯВКИ  (ЕСЛИ ПОПЫТАТЬСЯ ПОВТОРНО АПРУВНУТЬ АПРУВНУТУЮ ЗАЯВКУ)