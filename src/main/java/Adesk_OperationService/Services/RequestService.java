    package Adesk_OperationService.Services;

    import Adesk_OperationService.Constants.RequestStatuses;
    import Adesk_OperationService.Model.FileModel;
    import Adesk_OperationService.Model.OperationModel.Request.RequestModelDeleteDTO;
    import Adesk_OperationService.Model.OperationModel.RequestContext;
    import Adesk_OperationService.Model.OperationModel.Request.RequestFormDTO;
    import Adesk_OperationService.Model.OperationModel.RequestModel;
    import Adesk_OperationService.Repository.RequestRepository;
    import lombok.RequiredArgsConstructor;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.http.ResponseEntity;
    import org.springframework.scheduling.annotation.Async;
    import org.springframework.stereotype.Service;
    import org.springframework.web.multipart.MultipartFile;

    import java.io.IOException;
    import java.time.ZonedDateTime;
    import java.util.ArrayList;
    import java.util.Arrays;
    import java.util.List;
    import java.util.concurrent.CompletableFuture;
    import java.util.stream.Collectors;

    @Service
    @RequiredArgsConstructor
    public class RequestService {
        private final Logger log = LoggerFactory.getLogger(RequestService.class);
        private final RequestRepository requestRepository;



        public CompletableFuture<Long> createRequestAsync(RequestFormDTO form, RequestContext requestContext){
            return CompletableFuture.supplyAsync(() -> {


                if(!form.isValid()) throw new IllegalArgumentException("Form is invalid");

                var newRequest = new RequestModel();
                newRequest.setTypeOfOperation(form.getTypeOfOperation());
                newRequest.setDescription(form.getDescription());
                newRequest.setProjectId(form.getProjectId());
                newRequest.setCompanyId(requestContext.companyId());
                newRequest.setNameOfCounterparty(form.getNameOfCounterparty());
                newRequest.setCreatorEmail(requestContext.userEmail());
    //            newRequest.setName(form.getName());
                newRequest.setCreatorLogin(form.getResponsibleLogin());
                newRequest.setCreatedAt(ZonedDateTime.now());
                newRequest.setSum(form.getSum());
                newRequest.setApprovedStatus(RequestStatuses.APPROVING);    //В СЛУЧАЕ ЧЕГО МОЖНО УБРАТЬ КАКИЕ-ТО ПОЛЯ ИЗ ФОРМЫ И НЕ ДАВАТЬ ЕЮ ВСЮ ЗАПОЛНЯТЬ
                if (form.getFiles() != null && !form.getFiles().isEmpty()) {
                    List<FileModel> fileModels = new ArrayList<>();

                    for (MultipartFile multipartFile : form.getFiles()) {
                        if (!multipartFile.isEmpty()) {
                            try {
                                FileModel fileModel = createFileModel(multipartFile, newRequest,
                                        requestContext.userEmail());
                                fileModels.add(fileModel);
                            } catch (Exception e){
                                throw new RuntimeException("Failed to proccess file");
                            }
                        }
                    }
                    newRequest.setFiles(fileModels);
                }

                requestRepository.save(newRequest);
                return newRequest.getId();
            });
        }

        public CompletableFuture<Void> deleteRequests(List<RequestModelDeleteDTO> dtos){
            if (dtos.stream().anyMatch(x -> x.getId() == null)) throw new RuntimeException("dto is invalid");

            return CompletableFuture.runAsync(() -> {
                requestRepository.deleteAllById(
                        dtos.stream().map(RequestModelDeleteDTO::getId).collect(Collectors.toList())
                );
            });
        }

        public CompletableFuture<Void> editOperationAsync(Long id, RequestFormDTO dto, RequestContext requestContext) {
            return CompletableFuture.runAsync(() -> {
                var operationOpt = requestRepository.findById(id);
                if (operationOpt.isEmpty()) throw new RuntimeException("operation doesn't exist");

                var operation = operationOpt.get();

                operation.setDescription(dto.getDescription());
                operation.setTypeOfOperation(dto.getTypeOfOperation());
                operation.setProjectId(dto.getProjectId());
                operation.setNameOfCounterparty(dto.getNameOfCounterparty());
                operation.setSum(dto.getSum());
                operation.setCreatorLogin(dto.getResponsibleLogin());

                if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
                    List<String> newFileNames = dto.getFiles().stream()
                            .filter(f -> !f.isEmpty())
                            .map(MultipartFile::getOriginalFilename)
                            .collect(Collectors.toList());

                    List<FileModel> filesToRemove = operation.getFiles().stream()
                            .filter(file -> !newFileNames.contains(file.getOriginalFilename()))
                            .collect(Collectors.toList());

                    for (FileModel fileToRemove : filesToRemove) {
                        operation.removeFile(fileToRemove);
                    }

                    for (MultipartFile multipartFile : dto.getFiles()) {
                        if (!multipartFile.isEmpty()) {
                            try {
                                FileModel existingFile = operation.getFiles().stream()
                                        .filter(f -> f.getOriginalFilename().equals(multipartFile.getOriginalFilename()))
                                        .findFirst()
                                        .orElse(null);

                                if (existingFile != null) {
                                    existingFile.setFileSize(multipartFile.getSize());
                                    existingFile.setContent(multipartFile.getBytes());
                                    existingFile.setUserEmail(requestContext.userEmail());
//                                    existingFile.setCompressed(false);
                                    existingFile.setStoredFilename(null);
                                    existingFile.setHref(null);
                                } else {
                                    FileModel newFile = FileModel.builder()
                                            .originalFilename(multipartFile.getOriginalFilename())
                                            .fileSize(multipartFile.getSize())
                                            .content(multipartFile.getBytes())
                                            .userEmail(requestContext.userEmail())
                                            .companyId(operation.getCompanyId())
                                            .request(operation)
                                            .isCompressed(false)
                                            .build();
                                    operation.addFile(newFile);
                                }
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to process file: " +
                                        multipartFile.getOriginalFilename(), e);
                            }
                        }
                    }
                } else {
                    operation.getFiles().clear();
                }

                requestRepository.save(operation);
            });
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
    }
