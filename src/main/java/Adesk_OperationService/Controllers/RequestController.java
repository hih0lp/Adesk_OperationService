package Adesk_OperationService.Controllers;

import Adesk_OperationService.Constants.RequestStatuses;
import Adesk_OperationService.Model.OperationModel.*;
import Adesk_OperationService.Model.OperationModel.Request.RequestFormDTO;
import Adesk_OperationService.Model.OperationModel.Request.RequestModelDeleteDTO;
import Adesk_OperationService.Model.OperationModel.Request.SortByDateDTO;
import Adesk_OperationService.Model.StatDTO;
import Adesk_OperationService.Repository.FileRepository;
import Adesk_OperationService.Repository.RequestRepository;
import Adesk_OperationService.Services.RequestService;
import Adesk_OperationService.Services.TimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
    private final RequestService requestService;
    private final FileRepository fileRepository;

    @PostMapping(value = "/create-request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    @Operation(
            summary = "Создание нового запроса с файлами",
            description = "Создает новый запрос на операцию с прикрепленными файлами. Требуется право CREATE_REQUEST_AND_DELETE_BEFORE_APPROVE"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запрос успешно создан"),
            @ApiResponse(responseCode = "401", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<?>> createRequestAsync(
            @ModelAttribute RequestFormDTO form,
            HttpServletRequest request) {

        if (!Arrays.stream(request.getHeader("X-User-Permissions").split(","))
                .anyMatch(s -> s.equals("CREATE_REQUEST_AND_DELETE_BEFORE_APPROVE")))
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("no rights"));

        RequestContext rContext = new RequestContext(
                Long.parseLong(request.getHeader("X-Company-Id")),
                request.getHeader("X-User-Email")
        );

        return requestService.createRequestAsync(form, rContext)
                .thenApply(ResponseEntity::ok);
    }

    @GetMapping("/download-file/{id}")
    @Operation(
            summary = "Скачивание файла",
            description = "Скачивает файл по его ID. Требуется право REQUEST_WORK"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Файл успешно скачан"),
            @ApiResponse(responseCode = "401", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Файл не найден"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<byte[]>> downloadFile(
            @Parameter(description = "ID файла", required = true)
            @PathVariable Long id,
            HttpServletRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            String permissions = request.getHeader("X-User-Permissions");
            if (permissions == null || Arrays.stream(permissions.split(","))
                    .noneMatch("REQUEST_WORK"::equals)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            var fileOpt = fileRepository.findById(id);
            if (fileOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            var file = fileOpt.get();
            byte[] content = file.getContent();

            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            String storedName = file.getStoredFilename();

            if (storedName != null) {
                if (storedName.endsWith(".docx")) {
                    mediaType = MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                } else if (storedName.endsWith(".pdf")) {
                    mediaType = MediaType.APPLICATION_PDF;
                } else if (storedName.endsWith(".webp")) {
                    mediaType = MediaType.parseMediaType("image/webp");
                } else if (storedName.endsWith(".jpg") || storedName.endsWith(".jpeg")) {
                    mediaType = MediaType.IMAGE_JPEG;
                } else if (storedName.endsWith(".png")) {
                    mediaType = MediaType.IMAGE_PNG;
                }
            }

            String extension = "";
            if (storedName != null && storedName.contains(".")) {
                extension = storedName.substring(storedName.lastIndexOf('.'));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentLength(content.length);
            headers.set(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"file" + extension + "\""
            );

            return new ResponseEntity<>(content, headers, HttpStatus.OK);
        });
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
    public CompletableFuture<ResponseEntity<?>> deleteRequestsAsync(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Список ID запросов для удаления",
                    required = true,
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestModelDeleteDTO.class)))
            )
            @RequestBody List<RequestModelDeleteDTO> dtos,
            HttpServletRequest request) {
            var requests = _requestRepository.findAllById(dtos.stream().map(x -> x.getId()).collect(Collectors.toList()));

            if (Arrays.stream(request.getHeader("X-User-Permissions").split(","))
                    .anyMatch(s -> s.equals("REQUEST_WORK"))) {
                requestService.deleteRequests(dtos);
                return CompletableFuture.completedFuture(ResponseEntity.ok().body("deleting successfully"));

            } else if (Arrays.stream(request.getHeader("X-User-Permissions").split(","))
                    .anyMatch(s -> s.equals("CREATE_REQUEST_AND_DELETE_BEFORE_APPROVE"))) {

                if (requests.stream().anyMatch(x -> x.getApprovedStatus() != RequestStatuses.APPROVING))
                    return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("you can delete only request with approving status"));

                if (!Arrays.stream(request.getHeader("X-User-Permissions").split(","))
                        .anyMatch(s -> s.equals("REQUEST_WORK")))
                    if (requests.stream().anyMatch(s -> !s.getCreatorEmail().equals(request.getHeader("X-User-Email"))))
                        return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("you can delete only yours request"));

                requestService.deleteRequests(dtos);
                return CompletableFuture.completedFuture(ResponseEntity.ok().body("deleting successfully"));

            } else if (Arrays.stream(request.getHeader("X-User-Permissions").split(","))
                    .anyMatch(s -> s.equals("APPROVE_REQUEST_AND_DELETE_AFTER_APPROVE"))) {

                if (requests.stream().anyMatch(s -> s.getApprovedStatus() == RequestStatuses.APPROVING))
                    return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("you can only delete projects which approved"));

                if (!Arrays.stream(request.getHeader("X-User-Permissions").split(","))
                        .anyMatch(s -> s.equals("REQUEST_WORK")))
                    if (requests.stream().anyMatch(s -> s.getResponsibleManager() != request.getHeader("X-User-Email")))
                        return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("you can only delete your projects"));

                requestService.deleteRequests(dtos);
                return CompletableFuture.completedFuture(ResponseEntity.ok().body("deleting successfully"));

            } else {
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("no rights"));
            }
    }

    @GetMapping("/get-requests")
    @Operation(
            summary = "Получение запросов компании",
            description = "Возвращает список запросов для текущей компании (статус APPROVING)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запросы успешно получены",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestModel.class)))),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<?>> getRequestsByProjectName(HttpServletRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            var requests = _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            if (requests.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            requests = requests.stream()
                    .filter(x -> x.getApprovedStatus() == RequestStatuses.APPROVING)
                    .collect(Collectors.toList());

            return ResponseEntity.ok().body(requests);
        });
    }

    @PostMapping("/approve-request/{requestId}")
    @Transactional
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
    public CompletableFuture<ResponseEntity<?>> approveRequest(
            @Parameter(description = "ID запроса для утверждения", required = true)
            @PathVariable Long requestId,
            HttpServletRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            if (requestId == null)
                return ResponseEntity.badRequest().body("id cannot be null");

            if (!Arrays.stream(request.getHeader("X-User-Permissions").split(","))
                    .anyMatch(s -> s.equals("REQUEST_WORK") || s.equals("APPROVE_REQUEST_AND_DELETE_AFTER_APPROVE")))
                return ResponseEntity.badRequest().body("no rights");

            var requestOpt = _requestRepository.findById(requestId);
            if (requestOpt.isEmpty())
                return ResponseEntity.badRequest().body("request doesn't exist");

            var req = requestOpt.get();
            if (req.getApprovedStatus() == RequestStatuses.APPROVED)
                return ResponseEntity.badRequest().body("request has been already approved");

            req.setApprovedStatus(RequestStatuses.APPROVED);
            _requestRepository.save(req);

            return ResponseEntity.ok().body("successfully approved");
        });
    }

    @PostMapping("/disapprove-request/{requestId}")
    @Transactional
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
    public CompletableFuture<ResponseEntity<?>> disapproveRequest(
            @Parameter(description = "ID запроса для отклонения", required = true)
            @PathVariable Long requestId,
            HttpServletRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            if (requestId == null)
                return ResponseEntity.badRequest().body("id cannot be null");

            if (!Arrays.stream(request.getHeader("X-User-Permissions").split(","))
                    .anyMatch(s -> s.equals("REQUEST_WORK") || s.equals("APPROVE_REQUEST_AND_DELETE_AFTER_APPROVE")))
                return ResponseEntity.badRequest().body("no rights");

            var requestOpt = _requestRepository.findById(requestId);
            if (requestOpt.isEmpty())
                return ResponseEntity.badRequest().body("request doesn't exist");

            var req = requestOpt.get();
            _requestRepository.delete(req);

            return ResponseEntity.ok().body("successfully disapproved");
        });
    }

    @GetMapping("/get-requests-order-by-date-today")
    @Operation(
            summary = "Получение запросов за сегодня",
            description = "Возвращает запросы текущей компании за сегодняшний день"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запросы успешно получены",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestModel.class)))),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<?>> getRequestsOrderByDateToday(HttpServletRequest request) {
        return CompletableFuture.supplyAsync(() ->
                _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")))
        ).thenCompose(requests -> {
            if (requests.isEmpty())
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
            return _timeService.filterByTodayAsync(requests)
                    .thenApply(x -> ResponseEntity.ok().body(x));
        });
    }

    @GetMapping("/get-requests-order-by-date-week")
    @Operation(
            summary = "Получение запросов за неделю",
            description = "Возвращает запросы текущей компании за текущую неделю"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запросы успешно получены",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestModel.class)))),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<?>> getRequestsOrderByDateWeek(HttpServletRequest request) {
        return CompletableFuture.supplyAsync(() ->
                _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")))
        ).thenCompose(requests -> {
            if (requests.isEmpty())
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NO_CONTENT).build());

            var approvingRequests = requests.stream()
                    .filter(x -> x.getApprovedStatus() == RequestStatuses.APPROVING)
                    .collect(Collectors.toList());

            return _timeService.filterByCurrentWeekAsync(approvingRequests)
                    .thenApply(x -> ResponseEntity.ok().body(x));
        });
    }

    @GetMapping("/get-requests-order-by-month")
    @Operation(
            summary = "Получение запросов за месяц",
            description = "Возвращает запросы текущей компании за текущий месяц"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запросы успешно получены",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestModel.class)))),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<?>> getRequestsOrderByMonth(HttpServletRequest request) {
        return CompletableFuture.supplyAsync(() ->
                _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")))
        ).thenCompose(requests -> {
            if (requests.isEmpty())
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NO_CONTENT).build());

            var approvingRequests = requests.stream()
                    .filter(x -> x.getApprovedStatus() == RequestStatuses.APPROVING)
                    .collect(Collectors.toList());

            return _timeService.filterByCurrentMonthAsync(approvingRequests)
                    .thenApply(x -> ResponseEntity.ok().body(x));
        });
    }

    @PostMapping("/get-requests-order-by-dates")
    @Operation(
            summary = "Получение запросов по диапазону дат",
            description = "Возвращает запросы текущей компании в указанном диапазоне дат"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запросы успешно получены",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestModel.class)))),
            @ApiResponse(responseCode = "400", description = "Невалидные даты"),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<?>> getRequestsOrderByDates(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Диапазон дат для фильтрации",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SortByDateDTO.class))
            )
            @RequestBody SortByDateDTO dto,
            HttpServletRequest request) {

        if (!dto.isValid())
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body("dto is invalid"));

        return CompletableFuture.supplyAsync(() ->
                _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")))
        ).thenCompose(requests -> {
            if (requests.isEmpty())
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NO_CONTENT).build());

            var approvingRequests = requests.stream()
                    .filter(x -> x.getApprovedStatus() == RequestStatuses.APPROVING)
                    .collect(Collectors.toList());

            return _timeService.filterByDateTimeRangeAsync(approvingRequests, dto.date1, dto.date2)
                    .thenApply(x -> ResponseEntity.ok().body(x));
        });
    }

    @GetMapping("/get-requests-order-by-date-quarter/{numberOfQuarter}")
    @Operation(
            summary = "Получение запросов за квартал",
            description = "Возвращает запросы текущей компании за указанный квартал"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запросы успешно получены",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestModel.class)))),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "400", description = "Неверный номер квартала"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<?>> getRequestsOrderByDateQuarter(
            @Parameter(description = "Номер квартала (1-4)", required = true, example = "1")
            @PathVariable int numberOfQuarter,
            HttpServletRequest request) {

        if (numberOfQuarter < 1 || numberOfQuarter > 4) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body("Quarter must be between 1 and 4"));
        }

        return CompletableFuture.supplyAsync(() ->
                _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")))
        ).thenCompose(requests -> {
            if (requests.isEmpty())
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NO_CONTENT).build());

            var approvingRequests = requests.stream()
                    .filter(x -> x.getApprovedStatus() == RequestStatuses.APPROVING)
                    .collect(Collectors.toList());

            return _timeService.filterByQuarterAsync(approvingRequests, numberOfQuarter)
                    .thenApply(x -> ResponseEntity.ok().body(x));
        });
    }

    @GetMapping("/get-operations-by-project/{projectId}")
    @Operation(
            summary = "Получение операций по проекту",
            description = "Возвращает утвержденные операции (запросы) для указанного проекта"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Операции успешно получены",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestModel.class)))),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<?>> getProjectOperations(
            @Parameter(description = "ID проекта", required = true)
            @PathVariable Long projectId,
            HttpServletRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var requests = _requestRepository.findByProjectIdAndCompanyId(
                    projectId,
                    Long.parseLong(request.getHeader("X-Company-Id"))
            );

            if (requests.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            var operations = requests.stream()
                    .filter(x -> x.getApprovedStatus() == RequestStatuses.APPROVED)
                    .collect(Collectors.toList());

            if (operations.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            return ResponseEntity.ok().body(operations);
        });
    }

    @GetMapping("/get-requests-order-by-date-year")
    @Operation(
            summary = "Получение запросов за год",
            description = "Возвращает запросы текущей компании за текущий год"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Запросы успешно получены",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestModel.class)))),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<?>> getRequestsOrderByYear(HttpServletRequest request) {
        return CompletableFuture.supplyAsync(() ->
                _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")))
        ).thenCompose(requests -> {
            if (requests.isEmpty())
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NO_CONTENT).build());

            var approvingRequests = requests.stream()
                    .filter(x -> x.getApprovedStatus() == RequestStatuses.APPROVING)
                    .collect(Collectors.toList());

            return _timeService.filterByCurrentYearAsync(approvingRequests)
                    .thenApply(x -> ResponseEntity.ok().body(x));
        });
    }

    @GetMapping("/get-company-operations")
    @Operation(
            summary = "Получение всех операций компании",
            description = "Возвращает все утвержденные операции (запросы) текущей компании"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Операции успешно получены",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestModel.class)))),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<?>> getCompanyOperations(HttpServletRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            var operations = _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")));
            if (operations.isEmpty())
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

            var approvedOperations = operations.stream()
                    .filter(x -> x.getApprovedStatus() == RequestStatuses.APPROVED)
                    .collect(Collectors.toList());

            return ResponseEntity.ok().body(approvedOperations);
        });
    }

    @GetMapping("/get-company-operations-order-by-date-today")
    @Operation(
            summary = "Получение операций компании за сегодня",
            description = "Возвращает утвержденные операции текущей компании за сегодняшний день"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Операции успешно получены",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestModel.class)))),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<?>> getCompanyOperationsOrderByDateToday(HttpServletRequest request) {
        return CompletableFuture.supplyAsync(() ->
                _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")))
        ).thenCompose(operations -> {
            if (operations.isEmpty())
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NO_CONTENT).build());

            var approvedOperations = operations.stream()
                    .filter(x -> x.getApprovedStatus() == RequestStatuses.APPROVED)
                    .collect(Collectors.toList());

            return _timeService.filterByTodayAsync(approvedOperations)
                    .thenApply(x -> ResponseEntity.ok().body(x));
        });
    }

    @GetMapping("/get-company-operations-order-by-week")
    @Operation(
            summary = "Получение операций компании за неделю",
            description = "Возвращает утвержденные операции текущей компании за текущую неделю"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Операции успешно получены",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestModel.class)))),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<?>> getCompanyOperationsOrderByDateWeek(HttpServletRequest request) {
        return CompletableFuture.supplyAsync(() ->
                _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")))
        ).thenCompose(operations -> {
            if (operations.isEmpty())
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NO_CONTENT).build());

            var approvedOperations = operations.stream()
                    .filter(x -> x.getApprovedStatus() == RequestStatuses.APPROVED)
                    .collect(Collectors.toList());

            return _timeService.filterByCurrentWeekAsync(approvedOperations)
                    .thenApply(x -> ResponseEntity.ok().body(x));
        });
    }

    @GetMapping("/get-company-operations-order-by-month")
    @Operation(
            summary = "Получение операций компании за месяц",
            description = "Возвращает утвержденные операции текущей компании за текущий месяц"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Операции успешно получены",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestModel.class)))),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<?>> getCompanyOperationsOrderByMonth(HttpServletRequest request) {
        return CompletableFuture.supplyAsync(() ->
                _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")))
        ).thenCompose(operations -> {
            if (operations.isEmpty())
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NO_CONTENT).build());

            var approvedOperations = operations.stream()
                    .filter(x -> x.getApprovedStatus() == RequestStatuses.APPROVED)
                    .collect(Collectors.toList());

            return _timeService.filterByCurrentMonthAsync(approvedOperations)
                    .thenApply(x -> ResponseEntity.ok().body(x));
        });
    }

    @GetMapping("/get-company-operations-order-by-dates-quarter/{numberOfQuarter}")
    @Operation(
            summary = "Получение операций компании за квартал",
            description = "Возвращает утвержденные операции текущей компании за указанный квартал"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Операции успешно получены",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestModel.class)))),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "400", description = "Неверный номер квартала"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<?>> getCompanyOperationsOrderByDatesQuarter(
            @Parameter(description = "Номер квартала (1-4)", required = true, example = "1")
            @PathVariable int numberOfQuarter,
            HttpServletRequest request) {

        if (numberOfQuarter < 1 || numberOfQuarter > 4) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body("Quarter must be between 1 and 4"));
        }

        return CompletableFuture.supplyAsync(() ->
                _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")))
        ).thenCompose(operations -> {
            if (operations.isEmpty())
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NO_CONTENT).build());

            var approvedOperations = operations.stream()
                    .filter(x -> x.getApprovedStatus() == RequestStatuses.APPROVED)
                    .collect(Collectors.toList());

            return _timeService.filterByQuarterAsync(approvedOperations, numberOfQuarter)
                    .thenApply(x -> ResponseEntity.ok().body(x));
        });
    }

    @PostMapping("/get-company-operations-order-by-dates")
    @Operation(
            summary = "Получение операций компании по диапазону дат",
            description = "Возвращает утвержденные операции текущей компании в указанном диапазоне дат"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Операции успешно получены",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestModel.class)))),
            @ApiResponse(responseCode = "400", description = "Невалидные даты"),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<?>> getCompanyOperationsOrderByDates(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Диапазон дат для фильтрации",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SortByDateDTO.class))
            )
            @RequestBody SortByDateDTO dto,
            HttpServletRequest request) {

        if (!dto.isValid()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body("dto is invalid"));
        }

        return CompletableFuture.supplyAsync(() ->
                _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")))
        ).thenCompose(operations -> {
            if (operations.isEmpty())
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NO_CONTENT).build());

            var approvedOperations = operations.stream()
                    .filter(x -> x.getApprovedStatus() == RequestStatuses.APPROVED)
                    .collect(Collectors.toList());

            return _timeService.filterByDateTimeRangeAsync(approvedOperations, dto.date1, dto.date2)
                    .thenApply(x -> ResponseEntity.ok().body(x));
        });
    }

    @GetMapping("/get-company-operations-order-by-date-year")
    @Operation(
            summary = "Получение операций компании за год",
            description = "Возвращает утвержденные операции текущей компании за текущий год"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Операции успешно получены",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RequestModel.class)))),
            @ApiResponse(responseCode = "204", description = "Нет данных"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<?>> getCompanyOperationsOrderByYear(HttpServletRequest request) {
        return CompletableFuture.supplyAsync(() ->
                _requestRepository.findByCompanyId(Long.parseLong(request.getHeader("X-Company-Id")))
        ).thenCompose(operations -> {
            if (operations.isEmpty())
                return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.NO_CONTENT).build());

            var approvedOperations = operations.stream()
                    .filter(x -> x.getApprovedStatus() == RequestStatuses.APPROVED)
                    .collect(Collectors.toList());

            return _timeService.filterByCurrentYearAsync(approvedOperations)
                    .thenApply(x -> ResponseEntity.ok().body(x));
        });
    }

    @GetMapping("/get-project-statistic/{projectId}")
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
    public CompletableFuture<ResponseEntity<?>> getProjectStatistic(
            @Parameter(description = "ID проекта", required = true)
            @PathVariable Long projectId,
            HttpServletRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var projectOperations = _requestRepository.findByProjectIdAndCompanyId(
                    projectId,
                    Long.parseLong(request.getHeader("X-Company-Id"))
            );

            if (projectOperations.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }

            StatDTO stat = new StatDTO();
            stat.setRevenue(projectOperations.stream()
                    .filter(x -> x.getSum() > 0)
                    .mapToDouble(x -> x.getSum())
                    .sum());
            stat.setProfit(projectOperations.stream()
                    .mapToDouble(x -> x.getSum())
                    .sum());
            stat.setCountOfOperations((long) projectOperations.size());

            return ResponseEntity.ok().body(stat);
        });
    }


    @PutMapping("/edit-operation/{id}")
    @Operation(summary = "Редактирование операции",
            description = "Обновляет существующую операцию по ID с новыми данными из формы")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Операция успешно отредактирована",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "404", description = "Операция не найдена"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public CompletableFuture<ResponseEntity<?>> editOperationAsync(
            @Parameter(description = "ID операции", required = true, example = "123")
            @PathVariable Long id,

            @Parameter(description = "Данные для обновления операции", required = true)
            @ModelAttribute RequestFormDTO dto,

            @Parameter(hidden = true)
            HttpServletRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            var requestContext = new RequestContext(
                    Long.parseLong(request.getHeader("X-Company-Id")),
                    request.getHeader("X-User-Email")
            );
            return requestService.editOperationAsync(id, dto, requestContext);
        }).thenApply(x -> ResponseEntity.ok().body("successfully editing"));
    }
}