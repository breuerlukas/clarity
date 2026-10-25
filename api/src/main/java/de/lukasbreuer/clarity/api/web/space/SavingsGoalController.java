package de.lukasbreuer.clarity.api.web.space;

import de.lukasbreuer.clarity.api.banking.account.AccountScope;
import de.lukasbreuer.clarity.api.space.SpaceRepository;
import de.lukasbreuer.clarity.api.space.goal.SavingsGoal;
import de.lukasbreuer.clarity.api.space.goal.SavingsGoalRepository;
import de.lukasbreuer.clarity.api.web.request.ApiRequestBody;
import de.lukasbreuer.clarity.api.web.request.UuidParameter;
import de.lukasbreuer.clarity.api.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The savings goals of a space: list, create and change them. A linked
 * account has to belong to the same space. Error codes: 5001 unknown
 * space, 8200 unknown goal, 8201 invalid goal, 8202 account of another
 * space.
 */
@RestController
@RequiredArgsConstructor
public final class SavingsGoalController {
  private final SpaceRepository spaceRepository;
  private final SavingsGoalRepository goalRepository;
  private final AccountScope accountScope;

  @RequestMapping(path = "/goals/", method = RequestMethod.GET)
  public CompletableFuture<ApiResponse> findGoals(
    @RequestParam("space") String space
  ) {
    var spaceId = UuidParameter.parse(space);
    if (spaceId.isEmpty()) {
      return ApiResponse.error(5001).future();
    }
    return goalRepository.findBySpaceIdOrderByCreatedAt(spaceId.get())
      .thenApply(goals -> ApiResponse.success(Map.of("goals",
        goals.stream().map(SavingsGoal::information).toList())));
  }

  @RequestMapping(path = "/goals/create/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> create(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var spaceId = body.getUUID("space");
    var draft = SavingsGoalDraft.parse(body);
    if (spaceId == null) {
      return ApiResponse.error(5001).future();
    }
    if (draft.isEmpty()) {
      return ApiResponse.error(8201).future();
    }
    return spaceRepository.existsById(spaceId)
      .thenCompose(exists -> exists ?
        save(draft.get().create(spaceId), draft.get()) :
        ApiResponse.error(5001).future());
  }

  @RequestMapping(path = "/goals/update/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> update(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var body = ApiRequestBody.of(payload, response);
    var goalId = body.getUUID("id");
    var draft = SavingsGoalDraft.parse(body);
    if (goalId == null) {
      return ApiResponse.error(8200).future();
    }
    if (draft.isEmpty()) {
      return ApiResponse.error(8201).future();
    }
    return goalRepository.findById(goalId)
      .thenCompose(goal -> goal.map(draft.get()::applyTo)
        .map(changed -> save(changed, draft.get()))
        .orElse(ApiResponse.error(8200).future()));
  }

  private CompletableFuture<ApiResponse> save(
    SavingsGoal goal, SavingsGoalDraft draft
  ) {
    return accountBelongs(goal.spaceId(), draft.accountId())
      .thenCompose(belongs -> belongs ? goalRepository.save(goal)
        .thenApply(saved -> ApiResponse.success(Map.of("goal",
          saved.information()))) : ApiResponse.error(8202).future());
  }

  private CompletableFuture<Boolean> accountBelongs(
    UUID spaceId, UUID accountId
  ) {
    return accountId == null ? CompletableFuture.completedFuture(true) :
      accountScope.accountIds(spaceId).thenApply(ids ->
        ids.contains(accountId));
  }

  @RequestMapping(path = "/goals/delete/", method = RequestMethod.POST)
  public CompletableFuture<ApiResponse> delete(
    @RequestBody String payload, HttpServletResponse response
  ) {
    var goalId = ApiRequestBody.of(payload, response).getUUID("id");
    if (goalId == null) {
      return ApiResponse.error(8200).future();
    }
    return goalRepository.findById(goalId).thenCompose(this::delete);
  }

  private CompletableFuture<ApiResponse> delete(Optional<SavingsGoal> goal) {
    return goal.map(found -> goalRepository.delete(found)
        .thenApply(_ -> ApiResponse.success()))
      .orElse(ApiResponse.error(8200).future());
  }
}
