package Adesk_OperationService.Services;

import Adesk_OperationService.Constants.RequestStatuses;
import Adesk_OperationService.Model.FileModel;
import Adesk_OperationService.Model.OperationModel.RequestContext;
import Adesk_OperationService.Model.OperationModel.RequestFormDTO;
import Adesk_OperationService.Model.OperationModel.RequestModel;
import Adesk_OperationService.Repository.RequestRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class RequestService {
    private final Logger log = LoggerFactory.getLogger(RequestService.class);
    private final RequestRepository requestRepository;


    @Async
    public CompletableFuture<Long> createRequestAsync(RequestFormDTO form, RequestContext requestContext){
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
            return CompletableFuture.completedFuture(newRequest.getId());
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
