package de.lukasbreuer.clarity.api.web.business;

import de.lukasbreuer.clarity.api.business.contract.Contract;
import de.lukasbreuer.clarity.api.business.contract.ContractRepository;
import de.lukasbreuer.clarity.api.business.payment.PaymentAssignmentRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Creates, changes and ends customer contracts. Revenue figures are derived
 * from them in the panel, so a change shows up there right away. Error codes:
 * 4000 invalid contract, 4001 unknown contract, 4002 end date before the
 * start.
 */
@RestController
@RequiredArgsConstructor
public final class ContractController {
  private final ContractRepository contractRepository;
  private final PaymentAssignmentRepository assignmentRepository;

  @RequestMapping(path = "/business/contracts/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findContracts() {
    return contractRepository.findAll()
      .thenApply(contracts -> ApiResponse.success(Map.of("contracts",
        contracts.stream()
          .sorted(Comparator.comparing(Contract::startDate))
          .map(Contract::information)
          .toList())));
  }

  @RequestMapping(path = "/business/contracts/create/",
    method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> create(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var draft = ContractDraft.parse(ApiRequestBody.of(payload, response));
    if (draft.isEmpty()) {
      return ApiResponse.error(4000).future();
    }
    return contractRepository.save(draft.get().create())
      .thenApply(contract -> ApiResponse.success(Map.of("contract",
        contract.information())));
  }

  @RequestMapping(path = "/business/contracts/update/",
    method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> update(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var contractId = body.getUUID("id");
    var draft = ContractDraft.parse(body);
    if (contractId == null) {
      return ApiResponse.error(4001).future();
    }
    if (draft.isEmpty()) {
      return ApiResponse.error(4000).future();
    }
    return change(contractId, contract -> {
      draft.get().applyTo(contract);
      return contract.endDate() == null ||
        !contract.endDate().isBefore(contract.startDate());
    });
  }

  /**
   * Ends a contract on the given day, its last one. The panel suggests the
   * earliest day the notice period allows, but any day after the start goes,
   * since contracts also end by mutual agreement.
   */
  @RequestMapping(path = "/business/contracts/end/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> end(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var contractId = body.getUUID("id");
    var endDate = parseDate(body.getString("end_date", 10));
    if (contractId == null) {
      return ApiResponse.error(4001).future();
    }
    if (endDate.isEmpty()) {
      return ApiResponse.error(4002).future();
    }
    return change(contractId, contract -> {
      contract.end(endDate.get());
      return !endDate.get().isBefore(contract.startDate());
    });
  }

  @RequestMapping(path = "/business/contracts/resume/",
    method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> resume(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var contractId = ApiRequestBody.of(payload, response).getUUID("id");
    if (contractId == null) {
      return ApiResponse.error(4001).future();
    }
    return change(contractId, contract -> {
      contract.resume();
      return true;
    });
  }

  /**
   * For contracts entered by mistake; payments assigned to it go back to
   * being matched automatically.
   */
  @RequestMapping(path = "/business/contracts/delete/",
    method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> delete(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var contractId = ApiRequestBody.of(payload, response).getUUID("id");
    if (contractId == null) {
      return ApiResponse.error(4001).future();
    }
    return contractRepository.findById(contractId)
      .thenCompose(this::delete);
  }

  private CompletableFuture<ApiResponse> delete(Optional<Contract> contract) {
    if (contract.isEmpty()) {
      return ApiResponse.error(4001).future();
    }
    assignmentRepository.deleteByContractId(contract.get().id());
    return contractRepository.delete(contract.get())
      .thenApply(_ -> ApiResponse.success());
  }

  /**
   * @param change Applies the change and says whether the contract still
   *               ends after it starts
   */
  private CompletableFuture<ApiResponse> change(
    UUID contractId, Function<Contract, Boolean> change
  ) {
    return contractRepository.findById(contractId)
      .thenCompose(contract -> {
        if (contract.isEmpty()) {
          return ApiResponse.error(4001).future();
        }
        if (!change.apply(contract.get())) {
          return ApiResponse.error(4002).future();
        }
        return contractRepository.save(contract.get())
          .thenApply(saved -> ApiResponse.success(Map.of("contract",
            saved.information())));
      });
  }

  private Optional<LocalDate> parseDate(String value) {
    try {
      return Optional.of(LocalDate.parse(value));
    } catch (Exception exception) {
      return Optional.empty();
    }
  }
}
