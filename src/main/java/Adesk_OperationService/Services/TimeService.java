package Adesk_OperationService.Services;

import Adesk_OperationService.Model.OperationModel.RequestModel;
import Adesk_OperationService.Repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeService {
    private final RequestRepository _requestRepository;

    // ========== БАЗОВЫЕ АСИНХРОННЫЕ МЕТОДЫ ==========

    private CompletableFuture<List<RequestModel>> filterAsync(
            List<RequestModel> requests,
            java.util.function.Predicate<RequestModel> predicate) {

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            ZoneId systemZone = ZoneId.systemDefault();

            List<RequestModel> result = requests.stream()
                    .filter(predicate)
                    .sorted(Comparator.comparing(
                            request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                            Comparator.reverseOrder()))
                    .collect(Collectors.toList());

            log.debug("filterAsync completed in {} ms, filtered {}/{}",
                    System.currentTimeMillis() - startTime, result.size(), requests.size());

            return result;
        });
    }

    private CompletableFuture<List<RequestModel>> filterAsync(
            List<RequestModel> requests,
            java.util.function.Predicate<RequestModel> predicate,
            Comparator<RequestModel> comparator) {

        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            List<RequestModel> result = requests.stream()
                    .filter(predicate)
                    .sorted(comparator)
                    .collect(Collectors.toList());

            log.debug("filterAsync with comparator completed in {} ms",
                    System.currentTimeMillis() - startTime);

            return result;
        });
    }

    // ========== СИНХРОННЫЕ МЕТОДЫ (ДЛЯ ОБРАТНОЙ СОВМЕСТИМОСТИ) ==========

    /**
     * Фильтрует и сортирует запросы за сегодня (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за сегодня
     */
    public List<RequestModel> filterByToday(List<RequestModel> requests) {
        return filterByTodayAsync(requests).join();
    }

    /**
     * Фильтрует и сортирует запросы за вчера (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за вчера
     */
    public List<RequestModel> filterByYesterday(List<RequestModel> requests) {
        return filterByYesterdayAsync(requests).join();
    }

    /**
     * Фильтрует и сортирует запросы за текущую неделю (системная временная зона)
     * Неделя считается с понедельника по воскресенье
     *
     * @param requests список запросов
     * @return отсортированный список запросов за текущую неделю
     */
    public List<RequestModel> filterByCurrentWeek(List<RequestModel> requests) {
        return filterByCurrentWeekAsync(requests).join();
    }

    /**
     * Фильтрует и сортирует запросы за предыдущую неделю (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за предыдущую неделю
     */
    public List<RequestModel> filterByPreviousWeek(List<RequestModel> requests) {
        return filterByPreviousWeekAsync(requests).join();
    }

    /**
     * Фильтрует и сортирует запросы за текущий месяц (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за текущий месяц
     */
    public List<RequestModel> filterByCurrentMonth(List<RequestModel> requests) {
        return filterByCurrentMonthAsync(requests).join();
    }

    /**
     * Фильтрует и сортирует запросы за текущий год (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за текущий год
     */
    public List<RequestModel> filterByCurrentYear(List<RequestModel> requests) {
        return filterByCurrentYearAsync(requests).join();
    }

    /**
     * Фильтрует и сортирует запросы за последние N дней (системная временная зона)
     *
     * @param requests список запросов
     * @param days количество последних дней
     * @return отсортированный список запросов за последние N дней
     */
    public List<RequestModel> filterByLastNDays(List<RequestModel> requests, int days) {
        return filterByLastNDaysAsync(requests, days).join();
    }

    /**
     * Фильтрует и сортирует запросы за последние 7 дней (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за последние 7 дней
     */
    public List<RequestModel> filterByLast7Days(List<RequestModel> requests) {
        return filterByLast7DaysAsync(requests).join();
    }

    /**
     * Фильтрует и сортирует запросы за последние 30 дней (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за последние 30 дней
     */
    public List<RequestModel> filterByLast30Days(List<RequestModel> requests) {
        return filterByLast30DaysAsync(requests).join();
    }

    /**
     * Фильтрует и сортирует запросы за конкретную дату (системная временная зона)
     *
     * @param requests список запросов
     * @param date конкретная дата
     * @return отсортированный список запросов за указанную дату
     */
    public List<RequestModel> filterByDate(List<RequestModel> requests, LocalDate date) {
        return filterByDateAsync(requests, date).join();
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
        return filterByDateRangeAsync(requests, startDate, endDate).join();
    }

    // ========== АСИНХРОННЫЕ МЕТОДЫ ДЛЯ ФИЛЬТРАЦИИ ПО ДНЯМ И НЕДЕЛЯМ ==========

    /**
     * Асинхронно фильтрует и сортирует запросы за сегодня (системная временная зона)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов за сегодня
     */
    public CompletableFuture<List<RequestModel>> filterByTodayAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);
        return filterAsync(requests, request ->
                request.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate().equals(today));
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за вчера (системная временная зона)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов за вчера
     */
    public CompletableFuture<List<RequestModel>> filterByYesterdayAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate yesterday = LocalDate.now(systemZone).minusDays(1);
        return filterAsync(requests, request ->
                request.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate().equals(yesterday));
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за текущую неделю (системная временная зона)
     * Неделя считается с понедельника по воскресенье
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов за текущую неделю
     */
    public CompletableFuture<List<RequestModel>> filterByCurrentWeekAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        return filterAsync(requests, request -> {
            LocalDate requestDate = request.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate();
            return !requestDate.isBefore(startOfWeek) && !requestDate.isAfter(endOfWeek);
        });
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за предыдущую неделю (системная временная зона)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов за предыдущую неделю
     */
    public CompletableFuture<List<RequestModel>> filterByPreviousWeekAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);
        LocalDate lastWeek = today.minusWeeks(1);
        LocalDate startOfLastWeek = lastWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfLastWeek = lastWeek.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        return filterAsync(requests, request -> {
            LocalDate requestDate = request.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate();
            return !requestDate.isBefore(startOfLastWeek) && !requestDate.isAfter(endOfLastWeek);
        });
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за текущий месяц (системная временная зона)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов за текущий месяц
     */
    public CompletableFuture<List<RequestModel>> filterByCurrentMonthAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        YearMonth currentMonth = YearMonth.now(systemZone);
        LocalDate startOfMonth = currentMonth.atDay(1);
        LocalDate endOfMonth = currentMonth.atEndOfMonth();

        return filterAsync(requests, request -> {
            LocalDate requestDate = request.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate();
            return !requestDate.isBefore(startOfMonth) && !requestDate.isAfter(endOfMonth);
        });
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за текущий год (системная временная зона)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов за текущий год
     */
    public CompletableFuture<List<RequestModel>> filterByCurrentYearAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        int currentYear = Year.now(systemZone).getValue();
        LocalDate startOfYear = LocalDate.of(currentYear, 1, 1);
        LocalDate endOfYear = LocalDate.of(currentYear, 12, 31);

        return filterAsync(requests, request -> {
            LocalDate requestDate = request.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate();
            return !requestDate.isBefore(startOfYear) && !requestDate.isAfter(endOfYear);
        });
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за последние N дней (системная временная зона)
     *
     * @param requests список запросов
     * @param days количество последних дней
     * @return CompletableFuture с отсортированным списком запросов за последние N дней
     * @throws IllegalArgumentException если days < 1
     */
    public CompletableFuture<List<RequestModel>> filterByLastNDaysAsync(List<RequestModel> requests, int days) {
        if (days < 1) {
            throw new IllegalArgumentException("Days must be at least 1");
        }
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);
        LocalDate startDate = today.minusDays(days - 1);

        return filterAsync(requests, request -> {
            LocalDate requestDate = request.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate();
            return !requestDate.isBefore(startDate) && !requestDate.isAfter(today);
        });
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за последние 7 дней (системная временная зона)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов за последние 7 дней
     */
    public CompletableFuture<List<RequestModel>> filterByLast7DaysAsync(List<RequestModel> requests) {
        return filterByLastNDaysAsync(requests, 7);
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за последние 30 дней (системная временная зона)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов за последние 30 дней
     */
    public CompletableFuture<List<RequestModel>> filterByLast30DaysAsync(List<RequestModel> requests) {
        return filterByLastNDaysAsync(requests, 30);
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за конкретную дату (системная временная зона)
     *
     * @param requests список запросов
     * @param date конкретная дата
     * @return CompletableFuture с отсортированным списком запросов за указанную дату
     */
    public CompletableFuture<List<RequestModel>> filterByDateAsync(List<RequestModel> requests, LocalDate date) {
        ZoneId systemZone = ZoneId.systemDefault();
        return filterAsync(requests, request ->
                request.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate().equals(date));
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за диапазон дат (системная временная зона)
     *
     * @param requests список запросов
     * @param startDate начальная дата (включительно)
     * @param endDate конечная дата (включительно)
     * @return CompletableFuture с отсортированным списком запросов за указанный диапазон
     */
    public CompletableFuture<List<RequestModel>> filterByDateRangeAsync(List<RequestModel> requests, LocalDate startDate, LocalDate endDate) {
        ZoneId systemZone = ZoneId.systemDefault();
        return filterAsync(requests, request -> {
            LocalDate requestDate = request.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate();
            return !requestDate.isBefore(startDate) && !requestDate.isAfter(endDate);
        });
    }

    // ========== МЕТОДЫ ДЛЯ ПОЛУЧЕНИЯ ЗАПИСЕЙ ПО КВАРТАЛАМ ==========

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
     * Синхронно фильтрует и сортирует запросы по выбранному кварталу (системная временная зона)
     *
     * @param requests список запросов
     * @param quarter номер квартала (1, 2, 3, 4)
     * @return отсортированный список запросов за выбранный квартал
     */
    public List<RequestModel> filterByQuarter(List<RequestModel> requests, int quarter) {
        return filterByQuarterAsync(requests, quarter).join();
    }

    /**
     * Асинхронно фильтрует и сортирует запросы по выбранному кварталу (системная временная зона)
     *
     * @param requests список запросов
     * @param quarter номер квартала (1, 2, 3, 4)
     * @return CompletableFuture с отсортированным списком запросов за выбранный квартал
     * @throws IllegalArgumentException если quarter не в диапазоне 1-4
     */
    public CompletableFuture<List<RequestModel>> filterByQuarterAsync(List<RequestModel> requests, int quarter) {
        validateQuarter(quarter);
        ZoneId systemZone = ZoneId.systemDefault();
        return filterAsync(requests, request -> {
            LocalDate localDate = request.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate();
            return getFinancialQuarter(localDate) == quarter;
        });
    }

    /**
     * Синхронно фильтрует и сортирует запросы по выбранному кварталу текущего года (системная временная зона)
     *
     * @param requests список запросов
     * @param quarter номер квартала (1, 2, 3, 4)
     * @return отсортированный список запросов за выбранный квартал текущего года
     */
    public List<RequestModel> filterByQuarterThisYear(List<RequestModel> requests, int quarter) {
        return filterByQuarterThisYearAsync(requests, quarter).join();
    }

    /**
     * Асинхронно фильтрует и сортирует запросы по выбранному кварталу текущего года (системная временная зона)
     *
     * @param requests список запросов
     * @param quarter номер квартала (1, 2, 3, 4)
     * @return CompletableFuture с отсортированным списком запросов за выбранный квартал текущего года
     * @throws IllegalArgumentException если quarter не в диапазоне 1-4
     */
    public CompletableFuture<List<RequestModel>> filterByQuarterThisYearAsync(List<RequestModel> requests, int quarter) {
        validateQuarter(quarter);
        ZoneId systemZone = ZoneId.systemDefault();
        int currentYear = Year.now(systemZone).getValue();
        return filterAsync(requests, request -> {
            LocalDate localDate = request.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate();
            return localDate.getYear() == currentYear && getFinancialQuarter(localDate) == quarter;
        });
    }

    /**
     * Синхронно фильтрует запросы за текущий квартал (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за текущий квартал
     */
    public List<RequestModel> filterByCurrentQuarter(List<RequestModel> requests) {
        return filterByCurrentQuarterAsync(requests).join();
    }

    /**
     * Асинхронно фильтрует запросы за текущий квартал (системная временная зона)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов за текущий квартал
     */
    public CompletableFuture<List<RequestModel>> filterByCurrentQuarterAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);
        int currentQuarter = getFinancialQuarter(today);
        return filterByQuarterAsync(requests, currentQuarter);
    }

    /**
     * Синхронно фильтрует запросы за предыдущий квартал (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за предыдущий квартал
     */
    public List<RequestModel> filterByPreviousQuarter(List<RequestModel> requests) {
        return filterByPreviousQuarterAsync(requests).join();
    }

    /**
     * Асинхронно фильтрует запросы за предыдущий квартал (системная временная зона)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов за предыдущий квартал
     */
    public CompletableFuture<List<RequestModel>> filterByPreviousQuarterAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);
        int currentQuarter = getFinancialQuarter(today);
        int currentYear = today.getYear();

        final int previousQuarter;
        final int previousYear;
        if (currentQuarter - 1 < 1) {
            previousQuarter = 4;
            previousYear = currentYear - 1;
        } else {
            previousQuarter = currentQuarter - 1;
            previousYear = currentYear;
        }

        return filterAsync(requests, request -> {
            LocalDate localDate = request.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate();
            return localDate.getYear() == previousYear && getFinancialQuarter(localDate) == previousQuarter;
        });
    }

    /**
     * Синхронно фильтрует запросы за следующий квартал (системная временная зона)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за следующий квартал
     */
    public List<RequestModel> filterByNextQuarter(List<RequestModel> requests) {
        return filterByNextQuarterAsync(requests).join();
    }

    /**
     * Асинхронно фильтрует запросы за следующий квартал (системная временная зона)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов за следующий квартал
     */
    public CompletableFuture<List<RequestModel>> filterByNextQuarterAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);
        int currentQuarter = getFinancialQuarter(today);
        int currentYear = today.getYear();

        final int nextQuarter;
        final int nextYear;
        if (currentQuarter + 1 > 4) {
            nextQuarter = 1;
            nextYear = currentYear + 1;
        } else {
            nextQuarter = currentQuarter + 1;
            nextYear = currentYear;
        }

        return filterAsync(requests, request -> {
            LocalDate localDate = request.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate();
            return localDate.getYear() == nextYear && getFinancialQuarter(localDate) == nextQuarter;
        });
    }

    /**
     * Синхронно получает запросы за несколько кварталов (системная временная зона)
     *
     * @param requests список запросов
     * @param quarters номера кварталов (1, 2, 3, 4)
     * @return отсортированный список запросов за выбранные кварталы
     */
    public List<RequestModel> filterByQuarters(List<RequestModel> requests, List<Integer> quarters) {
        return filterByQuartersAsync(requests, quarters).join();
    }

    /**
     * Асинхронно получает запросы за несколько кварталов (системная временная зона)
     *
     * @param requests список запросов
     * @param quarters номера кварталов (1, 2, 3, 4)
     * @return CompletableFuture с отсортированным списком запросов за выбранные кварталы
     * @throws IllegalArgumentException если quarters null или пустой
     */
    public CompletableFuture<List<RequestModel>> filterByQuartersAsync(List<RequestModel> requests, List<Integer> quarters) {
        if (quarters == null || quarters.isEmpty()) {
            throw new IllegalArgumentException("Quarters list cannot be null or empty");
        }
        quarters.forEach(this::validateQuarter);
        ZoneId systemZone = ZoneId.systemDefault();

        return CompletableFuture.supplyAsync(() ->
                requests.stream()
                        .filter(request -> {
                            LocalDate localDate = request.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate();
                            int quarter = getFinancialQuarter(localDate);
                            return quarters.contains(quarter);
                        })
                        .sorted((r1, r2) -> {
                            LocalDate date1 = r1.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate();
                            LocalDate date2 = r2.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate();

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

                            return r2.getCreatedAt().withZoneSameInstant(systemZone)
                                    .compareTo(r1.getCreatedAt().withZoneSameInstant(systemZone));
                        })
                        .collect(Collectors.toList())
        );
    }

    /**
     * Синхронно сортирует запросы по кварталам (сначала новые кварталы, потом новые записи внутри квартала)
     *
     * @param requests список запросов
     * @return отсортированный список
     */
    public List<RequestModel> sortByQuarter(List<RequestModel> requests) {
        return sortByQuarterAsync(requests).join();
    }

    /**
     * Асинхронно сортирует запросы по кварталам (сначала новые кварталы, потом новые записи внутри квартала)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком
     */
    public CompletableFuture<List<RequestModel>> sortByQuarterAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();

        return CompletableFuture.supplyAsync(() ->
                requests.stream()
                        .sorted((r1, r2) -> {
                            LocalDate date1 = r1.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate();
                            LocalDate date2 = r2.getCreatedAt().withZoneSameInstant(systemZone).toLocalDate();

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

                            return r2.getCreatedAt().withZoneSameInstant(systemZone)
                                    .compareTo(r1.getCreatedAt().withZoneSameInstant(systemZone));
                        })
                        .collect(Collectors.toList())
        );
    }

    /**
     * Синхронно получает статистику по кварталам для переданного списка запросов
     *
     * @param requests список запросов
     * @return Map где ключ - "Год-Квартал" (напр. "2024-Q1"), значение - количество запросов
     */
    public Map<String, Long> getQuarterStats(List<RequestModel> requests) {
        return getQuarterStatsAsync(requests).join();
    }

    /**
     * Асинхронно получает статистику по кварталам для переданного списка запросов
     *
     * @param requests список запросов
     * @return CompletableFuture с Map где ключ - "Год-Квартал" (напр. "2024-Q1"), значение - количество запросов
     */
    public CompletableFuture<Map<String, Long>> getQuarterStatsAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();

        return CompletableFuture.supplyAsync(() ->
                requests.stream()
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
                        ))
        );
    }

    /**
     * Синхронно проверяет, находится ли запрос в указанном квартале (системная временная зона)
     *
     * @param request запрос
     * @param quarter номер квартала (1, 2, 3, 4)
     * @return true если запрос находится в указанном квартале
     */
    public boolean isInQuarter(RequestModel request, int quarter) {
        return isInQuarterAsync(request, quarter).join();
    }

    /**
     * Асинхронно проверяет, находится ли запрос в указанном квартале (системная временная зона)
     *
     * @param request запрос
     * @param quarter номер квартала (1, 2, 3, 4)
     * @return CompletableFuture с true если запрос находится в указанном квартале
     */
    public CompletableFuture<Boolean> isInQuarterAsync(RequestModel request, int quarter) {
        validateQuarter(quarter);
        ZoneId systemZone = ZoneId.systemDefault();

        return CompletableFuture.supplyAsync(() -> {
            LocalDate localDate = request.getCreatedAt()
                    .withZoneSameInstant(systemZone)
                    .toLocalDate();
            return getFinancialQuarter(localDate) == quarter;
        });
    }

    /**
     * Валидация номера квартала
     *
     * @param quarter номер квартала
     * @throws IllegalArgumentException если quarter не в диапазоне 1-4
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

    // ========== МЕТОДЫ ДЛЯ ФИЛЬТРАЦИИ ПО ВРЕМЕНИ ==========

    /**
     * Синхронно фильтрует и сортирует запросы за конкретный временной промежуток (системная временная зона)
     *
     * @param requests список запросов
     * @param startDateTime начало промежутка (включительно)
     * @param endDateTime конец промежутка (включительно)
     * @return отсортированный список запросов за указанный промежуток
     */
    public List<RequestModel> filterByDateTimeRange(List<RequestModel> requests, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return filterByDateTimeRangeAsync(requests, startDateTime, endDateTime).join();
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за конкретный временной промежуток (системная временная зона)
     *
     * @param requests список запросов
     * @param startDateTime начало промежутка (включительно)
     * @param endDateTime конец промежутка (включительно)
     * @return CompletableFuture с отсортированным списком запросов за указанный промежуток
     * @throws IllegalArgumentException если startDateTime после endDateTime
     */
    public CompletableFuture<List<RequestModel>> filterByDateTimeRangeAsync(
            List<RequestModel> requests, LocalDateTime startDateTime, LocalDateTime endDateTime) {

        if (startDateTime.isAfter(endDateTime)) {
            throw new IllegalArgumentException("Start datetime cannot be after end datetime");
        }

        ZoneId systemZone = ZoneId.systemDefault();
        ZonedDateTime startZoned = startDateTime.atZone(systemZone);
        ZonedDateTime endZoned = endDateTime.atZone(systemZone);

        return filterAsync(requests, request -> {
            ZonedDateTime requestDateTime = request.getCreatedAt().withZoneSameInstant(systemZone);
            return !requestDateTime.isBefore(startZoned) && !requestDateTime.isAfter(endZoned);
        });
    }

    /**
     * Синхронно фильтрует и сортирует запросы за конкретный временной промежуток (строковый ввод)
     *
     * @param requests список запросов
     * @param startDateTimeString начало промежутка в формате "yyyy-MM-dd'T'HH:mm:ss" (включительно)
     * @param endDateTimeString конец промежутка в формате "yyyy-MM-dd'T'HH:mm:ss" (включительно)
     * @return отсортированный список запросов за указанный промежуток
     */
    public List<RequestModel> filterByDateTimeRange(List<RequestModel> requests, String startDateTimeString, String endDateTimeString) {
        return filterByDateTimeRangeAsync(requests, startDateTimeString, endDateTimeString).join();
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за конкретный временной промежуток (строковый ввод)
     *
     * @param requests список запросов
     * @param startDateTimeString начало промежутка в формате "yyyy-MM-dd'T'HH:mm:ss" (включительно)
     * @param endDateTimeString конец промежутка в формате "yyyy-MM-dd'T'HH:mm:ss" (включительно)
     * @return CompletableFuture с отсортированным списком запросов за указанный промежуток
     */
    public CompletableFuture<List<RequestModel>> filterByDateTimeRangeAsync(
            List<RequestModel> requests, String startDateTimeString, String endDateTimeString) {

        LocalDateTime startDateTime = LocalDateTime.parse(startDateTimeString);
        LocalDateTime endDateTime = LocalDateTime.parse(endDateTimeString);
        return filterByDateTimeRangeAsync(requests, startDateTime, endDateTime);
    }

    /**
     * Синхронно фильтрует и сортирует запросы за конкретный промежуток времени сегодня
     *
     * @param requests список запросов
     * @param startTime начало времени (например, "09:00")
     * @param endTime конец времени (например, "18:00")
     * @return отсортированный список запросов за указанный промежуток времени сегодня
     */
    public List<RequestModel> filterByTimeRangeToday(List<RequestModel> requests, String startTime, String endTime) {
        return filterByTimeRangeTodayAsync(requests, startTime, endTime).join();
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за конкретный промежуток времени сегодня
     *
     * @param requests список запросов
     * @param startTime начало времени (например, "09:00")
     * @param endTime конец времени (например, "18:00")
     * @return CompletableFuture с отсортированным списком запросов за указанный промежуток времени сегодня
     */
    public CompletableFuture<List<RequestModel>> filterByTimeRangeTodayAsync(
            List<RequestModel> requests, String startTime, String endTime) {

        ZoneId systemZone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(systemZone);
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);
        LocalDateTime startDateTime = LocalDateTime.of(today, start);
        LocalDateTime endDateTime = LocalDateTime.of(today, end);
        return filterByDateTimeRangeAsync(requests, startDateTime, endDateTime);
    }

    /**
     * Синхронно фильтрует и сортирует запросы за утренние часы (06:00-12:00)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за утренние часы
     */
    public List<RequestModel> filterByMorning(List<RequestModel> requests) {
        return filterByMorningAsync(requests).join();
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за утренние часы (06:00-12:00)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов за утренние часы
     */
    public CompletableFuture<List<RequestModel>> filterByMorningAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        return filterAsync(requests, request -> {
            LocalTime requestTime = request.getCreatedAt().withZoneSameInstant(systemZone).toLocalTime();
            return requestTime.isAfter(LocalTime.of(5, 59)) && requestTime.isBefore(LocalTime.of(12, 1));
        });
    }

    /**
     * Синхронно фильтрует и сортирует запросы за дневные часы (12:00-18:00)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за дневные часы
     */
    public List<RequestModel> filterByAfternoon(List<RequestModel> requests) {
        return filterByAfternoonAsync(requests).join();
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за дневные часы (12:00-18:00)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов за дневные часы
     */
    public CompletableFuture<List<RequestModel>> filterByAfternoonAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        return filterAsync(requests, request -> {
            LocalTime requestTime = request.getCreatedAt().withZoneSameInstant(systemZone).toLocalTime();
            return requestTime.isAfter(LocalTime.of(11, 59)) && requestTime.isBefore(LocalTime.of(18, 1));
        });
    }

    /**
     * Синхронно фильтрует и сортирует запросы за вечерние часы (18:00-00:00)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за вечерние часы
     */
    public List<RequestModel> filterByEvening(List<RequestModel> requests) {
        return filterByEveningAsync(requests).join();
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за вечерние часы (18:00-00:00)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов за вечерние часы
     */
    public CompletableFuture<List<RequestModel>> filterByEveningAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        return filterAsync(requests, request -> {
            LocalTime requestTime = request.getCreatedAt().withZoneSameInstant(systemZone).toLocalTime();
            return requestTime.isAfter(LocalTime.of(17, 59));
        });
    }

    /**
     * Синхронно фильтрует и сортирует запросы за ночные часы (00:00-06:00)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за ночные часы
     */
    public List<RequestModel> filterByNight(List<RequestModel> requests) {
        return filterByNightAsync(requests).join();
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за ночные часы (00:00-06:00)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов за ночные часы
     */
    public CompletableFuture<List<RequestModel>> filterByNightAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        return filterAsync(requests, request -> {
            LocalTime requestTime = request.getCreatedAt().withZoneSameInstant(systemZone).toLocalTime();
            return requestTime.isBefore(LocalTime.of(6, 0));
        });
    }

    /**
     * Синхронно фильтрует и сортирует запросы за рабочие часы (09:00-18:00)
     *
     * @param requests список запросов
     * @return отсортированный список запросов за рабочие часы
     */
    public List<RequestModel> filterByBusinessHours(List<RequestModel> requests) {
        return filterByBusinessHoursAsync(requests).join();
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за рабочие часы (09:00-18:00)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов за рабочие часы
     */
    public CompletableFuture<List<RequestModel>> filterByBusinessHoursAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        return filterAsync(requests, request -> {
            LocalTime requestTime = request.getCreatedAt().withZoneSameInstant(systemZone).toLocalTime();
            return requestTime.isAfter(LocalTime.of(8, 59)) && requestTime.isBefore(LocalTime.of(18, 1));
        });
    }

    /**
     * Синхронно фильтрует и сортирует запросы за последние N часов
     *
     * @param requests список запросов
     * @param hours количество часов
     * @return отсортированный список запросов за последние N часов
     */
    public List<RequestModel> filterByLastNHours(List<RequestModel> requests, int hours) {
        return filterByLastNHoursAsync(requests, hours).join();
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за последние N часов
     *
     * @param requests список запросов
     * @param hours количество часов
     * @return CompletableFuture с отсортированным списком запросов за последние N часов
     * @throws IllegalArgumentException если hours < 1
     */
    public CompletableFuture<List<RequestModel>> filterByLastNHoursAsync(List<RequestModel> requests, int hours) {
        if (hours < 1) {
            throw new IllegalArgumentException("Hours must be at least 1");
        }

        ZoneId systemZone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(systemZone);
        ZonedDateTime startTime = now.minusHours(hours);

        return filterAsync(requests, request -> {
            ZonedDateTime requestDateTime = request.getCreatedAt().withZoneSameInstant(systemZone);
            return !requestDateTime.isBefore(startTime) && !requestDateTime.isAfter(now);
        });
    }

    /**
     * Синхронно фильтрует и сортирует запросы за последние 24 часа
     *
     * @param requests список запросов
     * @return отсортированный список запросов за последние 24 часа
     */
    public List<RequestModel> filterByLast24Hours(List<RequestModel> requests) {
        return filterByLast24HoursAsync(requests).join();
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за последние 24 часа
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов за последние 24 часа
     */
    public CompletableFuture<List<RequestModel>> filterByLast24HoursAsync(List<RequestModel> requests) {
        return filterByLastNHoursAsync(requests, 24);
    }

    /**
     * Синхронно фильтрует и сортирует запросы за конкретный час дня
     *
     * @param requests список запросов
     * @param hour час дня (0-23)
     * @return отсортированный список запросов за указанный час
     */
    public List<RequestModel> filterByHour(List<RequestModel> requests, int hour) {
        return filterByHourAsync(requests, hour).join();
    }

    /**
     * Асинхронно фильтрует и сортирует запросы за конкретный час дня
     *
     * @param requests список запросов
     * @param hour час дня (0-23)
     * @return CompletableFuture с отсортированным списком запросов за указанный час
     * @throws IllegalArgumentException если hour не в диапазоне 0-23
     */
    public CompletableFuture<List<RequestModel>> filterByHourAsync(List<RequestModel> requests, int hour) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Hour must be between 0 and 23");
        }

        ZoneId systemZone = ZoneId.systemDefault();
        return filterAsync(requests, request ->
                request.getCreatedAt().withZoneSameInstant(systemZone).getHour() == hour
        );
    }

    /**
     * Синхронно сортирует запросы по времени (от самого раннего к самому позднему)
     *
     * @param requests список запросов
     * @return отсортированный список запросов по времени
     */
    public List<RequestModel> sortByTimeAscending(List<RequestModel> requests) {
        return sortByTimeAscendingAsync(requests).join();
    }

    /**
     * Асинхронно сортирует запросы по времени (от самого раннего к самому позднему)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов по времени
     */
    public CompletableFuture<List<RequestModel>> sortByTimeAscendingAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        return CompletableFuture.supplyAsync(() ->
                requests.stream()
                        .sorted(Comparator.comparing(
                                request -> request.getCreatedAt().withZoneSameInstant(systemZone)))
                        .collect(Collectors.toList())
        );
    }

    /**
     * Синхронно сортирует запросы по времени (от самого позднего к самому раннему)
     *
     * @param requests список запросов
     * @return отсортированный список запросов по времени
     */
    public List<RequestModel> sortByTimeDescending(List<RequestModel> requests) {
        return sortByTimeDescendingAsync(requests).join();
    }

    /**
     * Асинхронно сортирует запросы по времени (от самого позднего к самому раннему)
     *
     * @param requests список запросов
     * @return CompletableFuture с отсортированным списком запросов по времени
     */
    public CompletableFuture<List<RequestModel>> sortByTimeDescendingAsync(List<RequestModel> requests) {
        ZoneId systemZone = ZoneId.systemDefault();
        return CompletableFuture.supplyAsync(() ->
                requests.stream()
                        .sorted(Comparator.comparing(
                                request -> request.getCreatedAt().withZoneSameInstant(systemZone),
                                Comparator.reverseOrder()))
                        .collect(Collectors.toList())
        );
    }
}