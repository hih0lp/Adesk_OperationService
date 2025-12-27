package Adesk_OperationService.Services;

import Adesk_OperationService.Model.OperationModel.RequestModel;
import Adesk_OperationService.Repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeService {
    private final RequestRepository _requestRepository;

    // ========== МЕТОДЫ ДЛЯ ФИЛЬТРАЦИИ ПО ДНЯМ И НЕДЕЛЯМ ==========

    /**
     * Фильтрует и сортирует запросы за сегодня (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за сегодня
     */
    public List<RequestModel> filterByToday(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);

        return requests.stream()
                .filter(request -> {
                    LocalDate requestDate = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();
                    return requestDate.equals(today);
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует и сортирует запросы за вчера (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за вчера
     */
    public List<RequestModel> filterByYesterday(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate yesterday = LocalDate.now(systemZone).minusDays(1);

        return requests.stream()
                .filter(request -> {
                    LocalDate requestDate = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();
                    return requestDate.equals(yesterday);
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует и сортирует запросы за текущую неделю (системная временная зона)
     * Неделя считается с понедельника по воскресенье
     *
     * @param requests список запросов
     * @return отсортированный список запросов за текущую неделю
     */
    public List<RequestModel> filterByCurrentWeek(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);

        // Начало недели (понедельник)
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        // Конец недели (воскресенье)
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        return requests.stream()
                .filter(request -> {
                    LocalDate requestDate = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();
                    return !requestDate.isBefore(startOfWeek) && !requestDate.isAfter(endOfWeek);
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует и сортирует запросы за предыдущую неделю (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за предыдущую неделю
     */
    public List<RequestModel> filterByPreviousWeek(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);
        LocalDate lastWeek = today.minusWeeks(1);

        // Начало предыдущей недели (понедельник)
        LocalDate startOfLastWeek = lastWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        // Конец предыдущей недели (воскресенье)
        LocalDate endOfLastWeek = lastWeek.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        return requests.stream()
                .filter(request -> {
                    LocalDate requestDate = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();
                    return !requestDate.isBefore(startOfLastWeek) && !requestDate.isAfter(endOfLastWeek);
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует и сортирует запросы за текущий месяц (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за текущий месяц
     */
    public List<RequestModel> filterByCurrentMonth(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        YearMonth currentMonth = YearMonth.now(systemZone);

        LocalDate startOfMonth = currentMonth.atDay(1);
        LocalDate endOfMonth = currentMonth.atEndOfMonth();

        return requests.stream()
                .filter(request -> {
                    LocalDate requestDate = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();
                    return !requestDate.isBefore(startOfMonth) && !requestDate.isAfter(endOfMonth);
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует и сортирует запросы за текущий год (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за текущий год
     */
    public List<RequestModel> filterByCurrentYear(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        int currentYear = Year.now(systemZone).getValue();

        LocalDate startOfYear = LocalDate.of(currentYear, 1, 1);
        LocalDate endOfYear = LocalDate.of(currentYear, 12, 31);

        return requests.stream()
                .filter(request -> {
                    LocalDate requestDate = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();
                    return !requestDate.isBefore(startOfYear) && !requestDate.isAfter(endOfYear);
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует и сортирует запросы за последние N дней (системная временная зона)
     *
     * @param requests список запросов
     * @param days количество последних дней
     * @return отсортированный список запросов за последние N дней
     */
    public List<RequestModel> filterByLastNDays(List<RequestModel> requests, int days) {
        if (days < 1) {
            throw new IllegalArgumentException("Days must be at least 1");
        }

        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);
        LocalDate startDate = today.minusDays(days - 1); // Включая сегодня

        return requests.stream()
                .filter(request -> {
                    LocalDate requestDate = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();
                    return !requestDate.isBefore(startDate) && !requestDate.isAfter(today);
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует и сортирует запросы за последние 7 дней (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за последние 7 дней
     */
    public List<RequestModel> filterByLast7Days(List<RequestModel> requests) {
        return filterByLastNDays(requests, 7);
    }

    /**
     * Фильтрует и сортирует запросы за последние 30 дней (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за последние 30 дней
     */
    public List<RequestModel> filterByLast30Days(List<RequestModel> requests) {
        return filterByLastNDays(requests, 30);
    }

    /**
     * Фильтрует и сортирует запросы за конкретную дату (системная временная зона)
     *
     * @param requests список запросов
     * @param date конкретная дата
     * @return отсортированный список запросов за указанную дату
     */
    public List<RequestModel> filterByDate(List<RequestModel> requests, LocalDate date) {
        ZoneId systemZone = ZoneId.systemDefault();

        return requests.stream()
                .filter(request -> {
                    LocalDate requestDate = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();
                    return requestDate.equals(date);
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует и сортирует запросы за диапазон дат (системная временная зона)
     *
     * @param requests список запросов
     * @param startDate начальная дата (включительно)
     * @param endDate конечная дата (включительно)
     * @return отсортированный список запросов за указанный диапазон
     */
    public List<RequestModel> filterByDateRange(List<RequestModel> requests, LocalDate startDate, LocalDate endDate) {
        ZoneId systemZone = ZoneId.systemDefault();

        return requests.stream()
                .filter(request -> {
                    LocalDate requestDate = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();
                    return !requestDate.isBefore(startDate) && !requestDate.isAfter(endDate);
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    // ========== МЕТОДЫ ДЛЯ ПОЛУЧЕНИЯ ЗАПИСЕЙ ПО КВАРТАЛАМ (оставляем) ==========

    /**
     * Получить номер финансового квартала для даты
     * Финансовый год считается стандартным (кварталы: янв-март, апр-июнь, июл-сен, окт-дек)
     *
     * @param date дата
     * @return номер квартала (1-4)
     */
    private int getFinancialQuarter(LocalDate date) {
        int month = date.getMonthValue();
        return (month - 1) / 3 + 1;
    }

    /**
     * Фильтрует и сортирует запросы по выбранному кварталу (системная временная зона)
     *
     * @param requests список запросов
     * @param quarter номер квартала (1, 2, 3, 4)
     * @return отсортированный список запросов за выбранный квартал
     */
    public List<RequestModel> filterByQuarter(List<RequestModel> requests, int quarter) {
        validateQuarter(quarter);
        ZoneId systemZone = ZoneId.systemDefault();

        return requests.stream()
                .filter(request -> {
                    LocalDate localDate = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();
                    return getFinancialQuarter(localDate) == quarter;
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует и сортирует запросы по выбранному кварталу текущего года (системная временная зона)
     *
     * @param requests список запросов
     * @param quarter номер квартала (1, 2, 3, 4)
     * @return отсортированный список запросов за выбранный квартал текущего года
     */
    public List<RequestModel> filterByQuarterThisYear(List<RequestModel> requests, int quarter) {
        validateQuarter(quarter);
        ZoneId systemZone = ZoneId.systemDefault();
        int currentYear = Year.now(systemZone).getValue();

        return requests.stream()
                .filter(request -> {
                    LocalDate localDate = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();
                    return localDate.getYear() == currentYear &&
                            getFinancialQuarter(localDate) == quarter;
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует запросы за текущий квартал (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за текущий квартал
     */
    public List<RequestModel> filterByCurrentQuarter(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);
        int currentQuarter = getFinancialQuarter(today);

        return filterByQuarter(requests, currentQuarter);
    }

    /**
     * Фильтрует запросы за предыдущий квартал (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за предыдущий квартал
     */
    public List<RequestModel> filterByPreviousQuarter(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);
        int currentQuarter = getFinancialQuarter(today);
        int currentYear = today.getYear();

        // Создаем final переменные для использования в лямбде
        final int previousQuarter;
        final int previousYear;

        if (currentQuarter - 1 < 1) {
            previousQuarter = 4;
            previousYear = currentYear - 1;
        } else {
            previousQuarter = currentQuarter - 1;
            previousYear = currentYear;
        }

        return requests.stream()
                .filter(request -> {
                    LocalDate localDate = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();
                    return localDate.getYear() == previousYear &&
                            getFinancialQuarter(localDate) == previousQuarter;
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует запросы за следующий квартал (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за следующий квартал
     */
    public List<RequestModel> filterByNextQuarter(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);
        int currentQuarter = getFinancialQuarter(today);
        int currentYear = today.getYear();

        // Создаем final переменные для использования в лямбде
        final int nextQuarter;
        final int nextYear;

        if (currentQuarter + 1 > 4) {
            nextQuarter = 1;
            nextYear = currentYear + 1;
        } else {
            nextQuarter = currentQuarter + 1;
            nextYear = currentYear;
        }

        return requests.stream()
                .filter(request -> {
                    LocalDate localDate = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();
                    return localDate.getYear() == nextYear &&
                            getFinancialQuarter(localDate) == nextQuarter;
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Получить запросы за несколько кварталов (системная временная зона)
     *
     * @param requests список запросов
     * @param quarters номера кварталов (1, 2, 3, 4)
     * @return отсортированный список запросов за выбранные кварталы
     */
    public List<RequestModel> filterByQuarters(List<RequestModel> requests, List<Integer> quarters) {
        if (quarters == null || quarters.isEmpty()) {
            throw new IllegalArgumentException("Quarters list cannot be null or empty");
        }

        quarters.forEach(this::validateQuarter);
        ZoneId systemZone = ZoneId.systemDefault();

        return requests.stream()
                .filter(request -> {
                    LocalDate localDate = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();
                    int quarter = getFinancialQuarter(localDate);
                    return quarters.contains(quarter);
                })
                .sorted((r1, r2) -> {
                    // Сначала по году (новые сначала), потом по кварталу, потом по дате
                    LocalDate date1 = r1.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();
                    LocalDate date2 = r2.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();

                    int yearComparison = Integer.compare(date2.getYear(), date1.getYear());
                    if (yearComparison != 0) {
                        return yearComparison;
                    }

                    int quarter1 = getFinancialQuarter(date1);
                    int quarter2 = getFinancialQuarter(date2);
                    int quarterComparison = Integer.compare(quarter2, quarter1);
                    if (quarterComparison != 0) {
                        return quarterComparison;
                    }

                    return r2.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .compareTo(r1.getCreatedAt().withZoneSameInstant(systemZone));
                })
                .collect(Collectors.toList());
    }

    /**
     * Сортирует запросы по кварталам (сначала новые кварталы, потом новые записи внутри квартала)
     *
     * @param requests список запросов
     * @return отсортированный список
     */
    public List<RequestModel> sortByQuarter(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();

        return requests.stream()
                .sorted((r1, r2) -> {
                    LocalDate date1 = r1.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();
                    LocalDate date2 = r2.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalDate();

                    // Сначала по году (новые сначала)
                    int yearComparison = Integer.compare(date2.getYear(), date1.getYear());
                    if (yearComparison != 0) {
                        return yearComparison;
                    }

                    // Потом по кварталу (новые сначала)
                    int quarter1 = getFinancialQuarter(date1);
                    int quarter2 = getFinancialQuarter(date2);
                    int quarterComparison = Integer.compare(quarter2, quarter1);
                    if (quarterComparison != 0) {
                        return quarterComparison;
                    }

                    // Потом по дате внутри квартала (новые сначала)
                    return r2.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .compareTo(r1.getCreatedAt().withZoneSameInstant(systemZone));
                })
                .collect(Collectors.toList());
    }

    /**
     * Получить статистику по кварталам для переданного списка запросов
     *
     * @param requests список запросов
     * @return Map где ключ - "Год-Квартал" (напр. "2024-Q1"), значение - количество запросов
     */
    public Map<String, Long> getQuarterStats(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();

        return requests.stream()
                .collect(Collectors.groupingBy(
                        request -> {
                            LocalDate localDate = request.getCreatedAt()
                                    .withZoneSameInstant(systemZone)
                                    .toLocalDate();
                            int year = localDate.getYear();
                            int quarter = getFinancialQuarter(localDate);
                            return year + "-Q" + quarter;
                        },
                        Collectors.counting()
                ));
    }

    /**
     * Проверяет, находится ли запрос в указанном квартале (системная временная зона)
     *
     * @param request запрос
     * @param quarter номер квартала (1, 2, 3, 4)
     * @return true если запрос находится в указанном квартале
     */
    public boolean isInQuarter(RequestModel request, int quarter) {
        validateQuarter(quarter);
        ZoneId systemZone = ZoneId.systemDefault();

        LocalDate localDate = request.getCreatedAt()
                .withZoneSameInstant(systemZone)
                .toLocalDate();
        return getFinancialQuarter(localDate) == quarter;
    }

    /**
     * Валидация номера квартала
     *
     * @param quarter номер квартала
     */
    private void validateQuarter(int quarter) {
        if (quarter < 1 || quarter > 4) {
            throw new IllegalArgumentException("Quarter must be 1, 2, 3 or 4");
        }
    }

    /**
     * Получить текущий квартал (системная временная зона)
     *
     * @return номер текущего квартала (1-4)
     */
    public int getCurrentQuarter() {
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);
        return getFinancialQuarter(today);
    }


    /**
     * Фильтрует и сортирует запросы за конкретный временной промежуток (системная временная зона)
     *
     * @param requests список запросов
     * @param startDateTime начало промежутка (включительно)
     * @param endDateTime конец промежутка (включительно)
     * @return отсортированный список запросов за указанный промежуток
     */
    public List<RequestModel> filterByDateTimeRange(List<RequestModel> requests, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        ZoneId systemZone = ZoneId.systemDefault();

        // Проверяем, что начальное время не позже конечного
        if (startDateTime.isAfter(endDateTime)) {
            throw new IllegalArgumentException("Start datetime cannot be after end datetime");
        }

        // Преобразуем в ZonedDateTime
        ZonedDateTime startZoned = startDateTime.atZone(systemZone);
        ZonedDateTime endZoned = endDateTime.atZone(systemZone);

        return requests.stream()
                .filter(request -> {
                    ZonedDateTime requestDateTime = request.getCreatedAt()
                            .withZoneSameInstant(systemZone);
                    return !requestDateTime.isBefore(startZoned) && !requestDateTime.isAfter(endZoned);
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует и сортирует запросы за конкретный временной промежуток (строковый ввод)
     *
     * @param requests список запросов
     * @param startDateTimeString начало промежутка в формате "yyyy-MM-dd'T'HH:mm:ss" (включительно)
     * @param endDateTimeString конец промежутка в формате "yyyy-MM-dd'T'HH:mm:ss" (включительно)
     * @return отсортированный список запросов за указанный промежуток
     */
    public List<RequestModel> filterByDateTimeRange(List<RequestModel> requests, String startDateTimeString, String endDateTimeString) {
        ZoneId systemZone = ZoneId.systemDefault();

        // Парсим строки
        LocalDateTime startDateTime = LocalDateTime.parse(startDateTimeString);
        LocalDateTime endDateTime = LocalDateTime.parse(endDateTimeString);

        return filterByDateTimeRange(requests, startDateTime, endDateTime);
    }

    /**
     * Фильтрует и сортирует запросы за конкретный промежуток времени сегодня
     *
     * @param requests список запросов
     * @param startTime начало времени (например, "09:00")
     * @param endTime конец времени (например, "18:00")
     * @return отсортированный список запросов за указанный промежуток времени сегодня
     */
    public List<RequestModel> filterByTimeRangeToday(List<RequestModel> requests, String startTime, String endTime) {
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);

        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);

        LocalDateTime startDateTime = LocalDateTime.of(today, start);
        LocalDateTime endDateTime = LocalDateTime.of(today, end);

        return filterByDateTimeRange(requests, startDateTime, endDateTime);
    }

    /**
     * Фильтрует и сортирует запросы за утренние часы (06:00-12:00)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за утренние часы
     */
    public List<RequestModel> filterByMorning(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();

        return requests.stream()
                .filter(request -> {
                    LocalTime requestTime = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalTime();
                    return requestTime.isAfter(LocalTime.of(5, 59)) &&
                            requestTime.isBefore(LocalTime.of(12, 1));
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует и сортирует запросы за дневные часы (12:00-18:00)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за дневные часы
     */
    public List<RequestModel> filterByAfternoon(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();

        return requests.stream()
                .filter(request -> {
                    LocalTime requestTime = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalTime();
                    return requestTime.isAfter(LocalTime.of(11, 59)) &&
                            requestTime.isBefore(LocalTime.of(18, 1));
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует и сортирует запросы за вечерние часы (18:00-00:00)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за вечерние часы
     */
    public List<RequestModel> filterByEvening(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();

        return requests.stream()
                .filter(request -> {
                    LocalTime requestTime = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalTime();
                    return requestTime.isAfter(LocalTime.of(17, 59));
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует и сортирует запросы за ночные часы (00:00-06:00)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за ночные часы
     */
    public List<RequestModel> filterByNight(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();

        return requests.stream()
                .filter(request -> {
                    LocalTime requestTime = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalTime();
                    return requestTime.isBefore(LocalTime.of(6, 0));
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует и сортирует запросы за рабочие часы (09:00-18:00)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за рабочие часы
     */
    public List<RequestModel> filterByBusinessHours(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();

        return requests.stream()
                .filter(request -> {
                    LocalTime requestTime = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .toLocalTime();
                    return requestTime.isAfter(LocalTime.of(8, 59)) &&
                            requestTime.isBefore(LocalTime.of(18, 1));
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует и сортирует запросы за последние N часов
     *
     * @param requests список запросов
     * @param hours количество часов
     * @return отсортированный список запросов за последние N часов
     */
    public List<RequestModel> filterByLastNHours(List<RequestModel> requests, int hours) {
        if (hours < 1) {
            throw new IllegalArgumentException("Hours must be at least 1");
        }

        ZoneId systemZone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(systemZone);
        ZonedDateTime startTime = now.minusHours(hours);

        return requests.stream()
                .filter(request -> {
                    ZonedDateTime requestDateTime = request.getCreatedAt()
                            .withZoneSameInstant(systemZone);
                    return !requestDateTime.isBefore(startTime) && !requestDateTime.isAfter(now);
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Фильтрует и сортирует запросы за последние 24 часа
     *
     * @param requests список запросов
     * @return отсортированный список запросов за последние 24 часа
     */
    public List<RequestModel> filterByLast24Hours(List<RequestModel> requests) {
        return filterByLastNHours(requests, 24);
    }

    /**
     * Фильтрует и сортирует запросы за конкретный час дня
     *
     * @param requests список запросов
     * @param hour час дня (0-23)
     * @return отсортированный список запросов за указанный час
     */
    public List<RequestModel> filterByHour(List<RequestModel> requests, int hour) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Hour must be between 0 and 23");
        }

        ZoneId systemZone = ZoneId.systemDefault();

        return requests.stream()
                .filter(request -> {
                    int requestHour = request.getCreatedAt()
                            .withZoneSameInstant(systemZone)
                            .getHour();
                    return requestHour == hour;
                })
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Сортирует запросы по времени (от самого раннего к самому позднему)
     *
     * @param requests список запросов
     * @return отсортированный список запросов по времени
     */
    public List<RequestModel> sortByTimeAscending(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();

        return requests.stream()
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone)
                ))
                .collect(Collectors.toList());
    }

    /**
     * Сортирует запросы по времени (от самого позднего к самому раннему)
     *
     * @param requests список запросов
     * @return отсортированный список запросов по времени
     */
    public List<RequestModel> sortByTimeDescending(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();

        return requests.stream()
                .sorted(Comparator.comparing(
                        request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                        Comparator.reverseOrder()
                ))
                .collect(Collectors.toList());
    }



}