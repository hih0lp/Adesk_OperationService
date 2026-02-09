package Adesk_OperationService.Model.OperationModel;

public record RequestContext (
    Long companyId,
    String userEmail
) {}