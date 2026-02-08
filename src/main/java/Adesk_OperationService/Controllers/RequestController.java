package Adesk_OperationService.Controllers;

import Adesk_OperationService.Constants.RequestStatuses;
import Adesk_OperationService.Model.FileModel;
import Adesk_OperationService.Model.OperationModel.*;
import Adesk_OperationService.Model.StatDTO;
import Adesk_OperationService.Repository.RequestRepository;
import Adesk_OperationService.Services.TimeService;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
@Tag(name = "Управление запросами", description = "API для работы с запросами на операции")
@SecurityRequirement(name = "bearerAuth")
public class RequestController {
    private final Logger log = LoggerFactory.getLogger(RequestController.class);
    private final RequestRepository _requestRepository;
    private final TimeService _timeService;

    @PostMapping(value = "/create-request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    @Operation(
            summary = "Создание нового запроса с файлами",
            description = "Создает новый запрос на операцию с прикрепленными файлами. Требуется право CREATE_REQUEST_AND_DELETE_BEFORE_APPROVE"
    )
    public ResponseEntity<?> createOperationAsync(
            @ModelAttribute RequestFormDTO form,
            HttpServletRequest request) {

        try {
            if(!form.isValid())
                return ResponseEntity.badRequest().body("form data is invalid");

            if(!Arrays.stream(request.getHeader("X-User-Permissions").split(","))
                    .anyMatch(s -> s.equals("CREATE_REQUEST_AND_DELETE_BEFORE_APPROVE")))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("no rights");

            var newRequest = new RequestModel();
            newRequest.setTypeOfOperation(form.getTypeOfOperation());
            newRequest.setDescription(form.getDescription());
            newRequest.setProjectId(form.getProjectId());
            newRequest.setCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            newRequest.setNameOfCounterparty(form.getNameOfCounterparty());
            newRequest.setCreatorEmail(request.getHeader("X-User-Email"));
//            newRequest.setName(form.getName());
            newRequest.setCreatorLogin(form.getResponsibleLogin());
            newRequest.setCreatedAt(ZonedDateTime.now());
            newRequest.setSum(form.getSum());
            newRequest.setApprovedStatus(RequestStatuses.APPROVING);    //В СЛУЧАЕ ЧЕГО МОЖНО УБРАТЬ КАКИЕ-ТО ПОЛЯ ИЗ ФОРМЫ И НЕ ДАВАТЬ ЕЮ ВСЮ ЗАПОЛНЯТЬ
            if (form.getFiles() != null && !form.getFiles().isEmpty()) {
                List<FileModel> fileModels = new ArrayList<>();

                for (MultipartFile multipartFile : form.getFiles()) {
                    if (!multipartFile.isEmpty()) {
                        FileModel fileModel = createFileModel(multipartFile, newRequest,
                                request.getHeader("X-User-Email"));
                        fileModels.add(fileModel);
                    }
                }
                newRequest.setFiles(fileModels);
            }

            _requestRepository.save(newRequest);
            return ResponseEntity.ok().body("successfully creating");

        } catch (Exception ex) {
            log.error(ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Logic error");
        }
    }

    private FileModel createFileModel(MultipartFile multipartFile,
                                      RequestModel request,
                                      String userEmail) throws IOException {

        return FileModel.builder()
                .originalFilename(multipartFile.getOriginalFilename())
                .fileSize(multipartFile.getSize())
                .content(multipartFile.getBytes())
                .userEmail(userEmail)
                .companyId(request.getCompanyId())
                .request(request) // Устанавливаем связь с Request
                .isCompressed(false)
                .build();
    }

    @DeleteMapping("/delete-requests")
    @Transactional
    @Operation(
            summary = "Удаление запросов",
            description = "Удаляет несколько запросов по ID. Доступ зависит от прав пользователя"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запросы успешно удалены"),
            @ApiResponse(responseCode = "400", description = "Невалидные данные или некорректный статус запросов"),
            @ApiResponse(responseCode = "401", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> deleteRequestsAsync(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Список ID запросов для удаления",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RequestModelDeleteDTO.class))
            )
            @RequestBody List<RequestModelDeleteDTO> dtos,
            HttpServletRequest request){
        try{
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
    @Operation(
            summary = "Получение запросов по проекту",
            description = "Возвращает список запросов для текущей компании"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запросы успешно получены"),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> getRequestsByProjectName(HttpServletRequest request){
        try{

//            var requests = _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
//            if(requests.isEmpty())
//                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

//            var json =
            return ResponseEntity.ok().body(Map.of("message", "hello world"));
        } catch(Exception ex){
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

    @PostMapping("/approve-request/{requestId}")
    @Operation(
            summary = "Утверждение запроса",
            description = "Утверждает запрос с указанным ID. Требуются права REQUEST_WORK или APPROVE_REQUEST_AND_DELETE_AFTER_APPROVE"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запрос успешно утвержден"),
            @ApiResponse(responseCode = "400", description = "Невалидный ID или запрос уже утвержден"),
            @ApiResponse(responseCode = "401", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> approveRequest(
            @Parameter(description = "ID запроса для утверждения", required = true)
            @PathVariable Long requestId,
            HttpServletRequest request){
        try{
            if(requestId == null)
                return ResponseEntity.badRequest().body("id cannot be null");

            if(!Arrays.stream(request.getHeader("X-User-Permissions")
                    .split(",")).anyMatch(s -> s.equals("REQUEST_WORK") || s.equals("APPROVE_REQUEST_AND_DELETE_AFTER_APPROVE")))
                return ResponseEntity.badRequest().body("no rights");

            var requestOpt = _requestRepository.findById(requestId);
            if(requestOpt.isEmpty())
                return ResponseEntity.badRequest().body("request doesn't exist");


            var req = requestOpt.get();
            if(req.getApprovedStatus() == RequestStatuses.APPROVING)
                return ResponseEntity.badRequest().body("request has been already approved");
            req.setApprovedStatus(RequestStatuses.APPROVED);
            _requestRepository.save(req);

            return ResponseEntity.ok().body("successfully approving");
        } catch(Exception ex) {
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

    @PostMapping("/disapprove-request/{requestId}")
    @Operation(
            summary = "Отклонение запроса",
            description = "Отклоняет запрос с указанным ID. Требуются права REQUEST_WORK или APPROVE_REQUEST_AND_DELETE_AFTER_APPROVE"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запрос успешно отклонен"),
            @ApiResponse(responseCode = "400", description = "Невалидный ID"),
            @ApiResponse(responseCode = "401", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> disapproveRequest(
            @Parameter(description = "ID запроса для отклонения", required = true)
            @PathVariable Long requestId,
            HttpServletRequest request){
        try{
            if(requestId == null)
                return ResponseEntity.badRequest().body("id cannot be null");

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
    @Operation(
            summary = "Получение запросов за сегодня",
            description = "Возвращает запросы текущей компании за сегодняшний день"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запросы успешно получены"),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> getRequestsOrderByDateToday(HttpServletRequest request){
        try {

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
    @Operation(
            summary = "Получение запросов за неделю",
            description = "Возвращает запросы текущей компании за текущую неделю"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запросы успешно получены"),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> getRequestsOrderByDateWeek(HttpServletRequest request){
        try{

            var requests = _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            if(requests.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            return ResponseEntity.ok().body(_timeService.filterByCurrentWeek(requests)); //фильтрация по текущей неделе
        } catch(Exception ex) {
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

    @GetMapping("/get-requests-order-by-month")
    @Operation(
            summary = "Получение запросов за месяц",
            description = "Возвращает запросы текущей компании за текущий месяц"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запросы успешно получены"),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> getRequestsOrderByMonth(HttpServletRequest request){
        try {
            var requests = _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            if(requests.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            return ResponseEntity.ok().body(_timeService.filterByCurrentMonth(requests));
        } catch (Exception ex){
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

    @PostMapping("/get-requests-order-by-dates")
    @Operation(
            summary = "Получение запросов по диапазону дат",
            description = "Возвращает запросы текущей компании в указанном диапазоне дат"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запросы успешно получены"),
            @ApiResponse(responseCode = "400", description = "Невалидные даты"),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> getRequestsOrderByDates(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Диапазон дат для фильтрации",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SortByDateDTO.class))
            )
            @RequestBody SortByDateDTO dto,
            HttpServletRequest request){
        try {
            if(!dto.isValid())
                return ResponseEntity.badRequest().body("dto is invalid");

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
    @Operation(
            summary = "Получение запросов за квартал",
            description = "Возвращает запросы текущей компании за указанный квартал"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запросы успешно получены"),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> getRequestsOrderByDateQuarter(
            @Parameter(description = "Номер квартала (1-4)", required = true)
            @PathVariable int numberOfQuarter,
            HttpServletRequest request){
        try{

            var requests = _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            if(requests.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            return ResponseEntity.ok().body(_timeService.filterByQuarter(requests, numberOfQuarter));
        } catch(Exception ex) {
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

    @GetMapping("/get-operations-by-project/{projectName}")
    @Operation(
            summary = "Получение операций по проекту",
            description = "Возвращает утвержденные операции (запросы) для указанного проекта"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Операции успешно получены"),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> getProjectOperations(
            @Parameter(description = "Название проекта", required = true)
            @PathVariable Long projectId,
            HttpServletRequest request){
        try {
            var requests = _requestRepository.findByProjectIdAndCompanyId(projectId, Long.parseLong(request.getHeader("X-Company-Id")));
            if(requests.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            var operations = requests.stream().filter(x -> x.getApprovedStatus() == RequestStatuses.APPROVED).toList();
            if(operations.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            return ResponseEntity.ok().body(operations);
        } catch (Exception ex){
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

    @GetMapping("/get-requests-order-by-date-year")
    @Operation(
            summary = "Получение запросов за год",
            description = "Возвращает запросы текущей компании за текущий год"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запросы успешно получены"),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> getRequestsOrderByYear(HttpServletRequest request){
        try{

            var requests = _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            if(requests.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            return ResponseEntity.ok().body(_timeService.filterByCurrentYear(requests));
        } catch(Exception ex) {
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

    @GetMapping("/get-company-requests")
    @Operation(
            summary = "Получение всех запросов компании",
            description = "Возвращает все запросы текущей компании"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запросы успешно получены"),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> getCompanyRequests(HttpServletRequest request){
        try {

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
    @Operation(
            summary = "Получение всех операций компании",
            description = "Возвращает все утвержденные операции (запросы) текущей компании"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Операции успешно получены"),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> getCompanyOperations(HttpServletRequest request){
        try{

            var operations = _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            if(operations.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            return ResponseEntity.ok().body(operations.stream().filter(x -> x.getApprovedStatus() == RequestStatuses.APPROVED));
        } catch(Exception ex){
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

    @GetMapping("/get-project-statistic/{projectName}")
    @Operation(
            summary = "Получение статистики по проекту",
            description = "Возвращает статистику по операциям указанного проекта"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статистика успешно получена",
                    content = @Content(schema = @Schema(implementation = StatDTO.class))),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<?> getProjectStatistic(
            @Parameter(description = "Название проекта", required = true)
            @PathVariable Long projectId,
            HttpServletRequest request){
        try {

            var projectOperations = _requestRepository.findByProjectIdAndCompanyId(projectId, Long.parseLong(request.getHeader("X-Company-Id")));
            StatDTO stat = new StatDTO();
            stat.setRevenue(projectOperations.stream().filter(x -> x.getSum() > 0).mapToDouble(x -> x.getSum()).sum());
            stat.setProfit(projectOperations.stream().mapToDouble(x -> x.getSum()).sum());
            stat.setCountOfOperations(projectOperations.stream().count());

            return ResponseEntity.ok().body(stat);
        } catch (Exception ex){
            log.error(ex.getMessage());
            return ResponseEntity.status(500).body("Logic error");
        }
    }

}


///TODO : ПОИСК НУЖНО СДЕЛАТЬ ПО ТРИГРАММАМ
///TODO : ОПЕРАЦИИ ТОЛЬКО АПРУВНУТЫЕ МОГУТ БЫТЬ //есть
///TODO : ЗАЯВКИ ВСЕ МОГУТ БЫТЬ (ЛЮБОЙ СТАТУС МОЖЕТ БЫТЬ) //есть
///TODO : ПОИСК ЗАЯВОК НЕ ДОЛЖЕН БЫ ПО ПРОЕКТУ (ДОЛЖЕН БЫТЬ ПРОСТО ЗАПРОС НА ВСЕ МАТЬ ТВОЮ ЗАПРОСЫ БЛЯ) //есть
/// TODO : СДЕЛАТЬ ВАЛИДАЦИЮ НА СТАТУС ЗАЯВКИ  (ЕСЛИ ПОПЫТАТЬСЯ ПОВТОРНО АПРУВНУТЬ АПРУВНУТУЮ ЗАЯВКУ) //есть
/// TODO : СДЕЛАТЬ ФИЛЬТРАЦИЮ ПО МЕСЯЦУ //есть

///TODO : ДОБАВИТЬ БЛОКИ СТАТИСТИКИ
