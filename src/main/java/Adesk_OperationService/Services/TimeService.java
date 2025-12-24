package Adesk_OperationService.Services;


import Adesk_OperationService.Model.OperationModel.RequestModel;
import Adesk_OperationService.Repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeService {
    private final RequestRepository _requestRepository;

    public List<RequestModel> sortByLocalDateSystemZone(List<RequestModel> requests) {
//        List<RequestModel> requests = _requestRepository.findAll();

        requests.sort((r1, r2) -> {
            LocalDate date1 = r1.getCreatedAt().toLocalDate();
            LocalDate date2 = r2.getCreatedAt().toLocalDate();

            int dateComparison = date2.compareTo(date1);

            if (dateComparison == 0) {
                return r2.getCreatedAt().compareTo(r1.getCreatedAt());
            }

            return dateComparison;
        });

        return requests;
    }

    public List<RequestModel> sortByLocalDateInTimeZone(ZoneId timeZone) {
        List<RequestModel> requests = _requestRepository.findAll();

        requests.sort((r1, r2) -> {
            LocalDate date1 = r1.getCreatedAt()
                    .withZoneSameInstant(timeZone)
                    .toLocalDate();

            LocalDate date2 = r2.getCreatedAt()
                    .withZoneSameInstant(timeZone)
                    .toLocalDate();

            int dateComparison = date2.compareTo(date1);

            if (dateComparison == 0) {
                ZonedDateTime time1 = r1.getCreatedAt().withZoneSameInstant(timeZone);
                ZonedDateTime time2 = r2.getCreatedAt().withZoneSameInstant(timeZone);
                return time2.compareTo(time1);
            }

            return dateComparison;
        });

        return requests;
    }

    public List<RequestModel> getTodaySortedInTimeZone(ZoneId timeZone) {
        LocalDate today = LocalDate.now(timeZone);

        return _requestRepository.findAll().stream()
                .filter(request ->
                        request.getCreatedAt()
                                .withZoneSameInstant(timeZone)
                                .toLocalDate()
                                .equals(today)
                )
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(timeZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    public List<RequestModel> sortByDateSimple(ZoneId timeZone) {
        return _requestRepository.findAll().stream()
                .sorted(Comparator
                        .comparing((RequestModel r) ->
                                r.getCreatedAt().withZoneSameInstant(timeZone).toLocalDate()
                        )
                        .reversed()
                        .thenComparing(r ->
                                        r.getCreatedAt().withZoneSameInstant(timeZone),
                                Comparator.reverseOrder()
                        )
                )
                .collect(Collectors.toList());
    }
}
